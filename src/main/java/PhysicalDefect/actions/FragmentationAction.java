package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import PhysicalDefect.patches.FragmentationPatch;

public class FragmentationAction extends AbstractGameAction {
    private AbstractCard card;
    private int remainingRecursions;

    public FragmentationAction(AbstractCard card, int remainingRecursions) {
        this.card = card;
        this.remainingRecursions = remainingRecursions;
        this.actionType = ActionType.DAMAGE;
        // 设为 0 或者极短的时间，让伤害判定紧凑，不再等待卡牌动画
        this.duration = 0.01F;
    }

    @Override
    public void update() {
        // 1. 安全检查
        if (remainingRecursions <= 0 || AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.isDone = true;
            return;
        }

        AbstractPlayer p = AbstractDungeon.player;

        // 2. 寻找随机目标
        AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                AbstractDungeon.cardRandomRng);

        if (target != null && !target.isDeadOrEscaped()) {
            // 3. 创建分身
            AbstractCard tmp = card.makeStatEquivalentCopy();

            // 4. 标记为“已碎片化”（必须在计算伤害前！）
            FragmentationPatch.CardFields.isFragmentedInstance.set(tmp, true);

            // 5. 【关键】计算针对该目标的最终数值
            // 这会触发你的 Patch，把伤害改为除以 N 后的数值
            tmp.calculateCardDamage(target);

            // 6. 【核心修改】直接调用 use 方法
            // 不再使用 NewQueueCardAction。
            // 这样不会有卡牌飞出来的动画，只会执行卡牌里的 action（比如 DamageAction）
            // 视觉上就是敌人身上直接蹦出伤害数字和受击特效
            tmp.use(p, target);

            // 7. 递归：继续处理剩余次数
            if (remainingRecursions > 1) {
                // 如果希望伤害像加特林一样极快地连续出现，可以用 addToTop
                // 如果希望有节奏感，用 addToBottom
                AbstractDungeon.actionManager.addToBottom(
                        new FragmentationAction(card, remainingRecursions - 1));
            }
        }

        this.isDone = true;
    }
}
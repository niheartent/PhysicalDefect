package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import PhysicalDefect.patches.FragmentationPatch;

public class FragmentationAction extends AbstractGameAction {
    private AbstractCard card;
    private int remainingRecursions; // 还需要递归多少次

    /**
     * @param card                原始卡牌的副本
     * @param remainingRecursions 剩余需要打出的次数（即当前的碎片化层数）
     */
    public FragmentationAction(AbstractCard card, int remainingRecursions) {
        this.card = card;
        this.remainingRecursions = remainingRecursions;
        this.actionType = ActionType.DAMAGE;
        this.duration = 0.1F; // 稍微给点间隔，防止动画重叠太快
    }

    @Override
    public void update() {
        if (remainingRecursions <= 0 || AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.isDone = true;
            return;
        }

        // 1. 寻找随机目标
        AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                AbstractDungeon.cardRandomRng);

        if (target != null && !target.isDeadOrEscaped()) {
            // 2. 创建分身
            AbstractCard tmp = card.makeStatEquivalentCopy();

            // 3. 标记为“已碎片化”，防止无限触发 Patch 中的 Trigger
            FragmentationPatch.CardFields.isFragmentedInstance.set(tmp, true);

            // 4. 重要：清除部分特性，防止消耗能量或再次烧牌
            tmp.purgeOnUse = true;
            tmp.freeToPlayOnce = true;

            // 5. 强制计算对该随机目标的伤害（应用被削减后的面板 + 力量）
            tmp.calculateCardDamage(target);

            // 6. 使用 NewQueueCardAction 将卡牌打出
            // 这个 Action 会处理卡牌的动画、音效和实际伤害结算
            AbstractDungeon.actionManager.addToBottom(new NewQueueCardAction(tmp, target, true, true));

            // 7. 递归：减少次数，再次加入队列
            if (remainingRecursions > 1) {
                AbstractDungeon.actionManager.addToBottom(
                        new FragmentationAction(card, remainingRecursions - 1));
            }
        }

        this.isDone = true;
    }
}
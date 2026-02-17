package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import PhysicalDefect.patches.NegativeFocusPatches;

public class ScatterRecursiveAction extends AbstractGameAction {
    private AbstractCard card;
    private AbstractPlayer p;
    private int times;

    public ScatterRecursiveAction(AbstractPlayer p, AbstractCard card, int times) {
        this.p = p;
        this.card = card;
        this.times = times;
        // 建议设为 0 或者极其短的时间，因为 UseCardAction 本身就有动画时间
        // 如果这里设太长，连发会显得很慢
        this.duration = 0.0F;
        this.actionType = ActionType.DAMAGE;
    }

    @Override
    public void update() {
        // 0. 安全检查：如果次数没了，或者怪全死光了，直接停止递归
        if (times <= 0 || AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.isDone = true;
            return;
        }

        // 1. 寻找目标
        AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                AbstractDungeon.cardRandomRng);

        // 2. 如果找到了活着的怪，才进行攻击和后续递归
        if (target != null) {
            AbstractCard tmp = card.makeStatEquivalentCopy();

            // 引用你的 SpireField，确保 Patch 不会拦截它
            NegativeFocusPatches.CardFields.isScatteredInstance.set(tmp, true);

            tmp.purgeOnUse = true;
            tmp.freeToPlayOnce = true;

            // 重新计算伤害
            tmp.calculateCardDamage(target);

            // 打出卡牌 (UseCardAction 会进入队列)
            tmp.use(p, target);

            // 3. 【递归】只有在本次成功锁定了目标的前提下，才通过 addToBottom 安排下一次
            // 这样能保证如果你一枪把最后一个怪打死了，后续的子弹就不会再尝试发射了
            if (times > 1) {
                AbstractDungeon.actionManager.addToBottom(new ScatterRecursiveAction(p, card, times - 1));
            }
        }

        this.isDone = true;
    }
}
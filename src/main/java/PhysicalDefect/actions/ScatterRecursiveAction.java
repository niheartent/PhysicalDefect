package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import PhysicalDefect.patches.NegativeFocusPatches;

// public class ScatterRecursiveAction extends AbstractGameAction {
//     private AbstractCard card;
//     private AbstractPlayer p;
//     private int times;

//     public ScatterRecursiveAction(AbstractPlayer p, AbstractCard card, int times) {
//         this.p = p;
//         this.card = card;
//         this.times = times;
//         // 建议设为 0 或者极其短的时间，因为 UseCardAction 本身就有动画时间
//         // 如果这里设太长，连发会显得很慢
//         this.duration = 0.0F;
//         this.actionType = ActionType.DAMAGE;
//     }

//     @Override
//     public void update() {
//         // 0. 安全检查：如果次数没了，或者怪全死光了，直接停止递归
//         if (times <= 0 || AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
//             this.isDone = true;
//             return;
//         }

//         // 1. 寻找目标
//         AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
//                 AbstractDungeon.cardRandomRng);

//         // 2. 如果找到了活着的怪，才进行攻击和后续递归
//         if (target != null) {
//             AbstractCard tmp = card.makeStatEquivalentCopy();

//             // 引用你的 SpireField，确保 Patch 不会拦截它
//             NegativeFocusPatches.CardFields.isScatteredInstance.set(tmp, true);

//             tmp.purgeOnUse = true;
//             tmp.freeToPlayOnce = true;

//             // 重新计算伤害
//             tmp.calculateCardDamage(target);

//             // 打出卡牌 (UseCardAction 会进入队列)
//             tmp.use(p, target);

//             // 3. 【递归】只有在本次成功锁定了目标的前提下，才通过 addToBottom 安排下一次
//             // 这样能保证如果你一枪把最后一个怪打死了，后续的子弹就不会再尝试发射了
//             if (times > 1) {
//                 AbstractDungeon.actionManager.addToBottom(new ScatterRecursiveAction(p, card, times - 1));
//             }
//         }

//         this.isDone = true;
//     }
// }

public class ScatterRecursiveAction extends AbstractGameAction {
    private AbstractCard card;
    private AbstractPlayer p;
    private int times; // 剩余攻击次数
    private int decayStep; // 当前力量衰减层数 (第2下传1，第3下传2...)

    /**
     * @param decayStep 当前这一下攻击需要减少多少点力量收益
     */
    public ScatterRecursiveAction(AbstractPlayer p, AbstractCard card, int times, int decayStep) {
        this.p = p;
        this.card = card;
        this.times = times;
        this.decayStep = decayStep;
        this.duration = 0.0F;
        this.actionType = ActionType.DAMAGE;
    }

    @Override
    public void update() {
        // 0. 安全检查
        if (times <= 0 || AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.isDone = true;
            return;
        }

        // 1. 寻找目标
        AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                AbstractDungeon.cardRandomRng);

        // 2. 如果找到了活着的怪
        if (target != null) {
            AbstractCard tmp = card.makeStatEquivalentCopy();
            NegativeFocusPatches.CardFields.isScatteredInstance.set(tmp, true);
            tmp.purgeOnUse = true;
            tmp.freeToPlayOnce = true;

            // -----------------------------------------------------------
            // A. 先计算标准伤害 (此时包含了 易伤、全额力量、愤怒姿态等)
            // -----------------------------------------------------------
            tmp.calculateCardDamage(target);

            // -----------------------------------------------------------
            // B. 手动剔除衰减的力量值
            // -----------------------------------------------------------
            AbstractPower strPower = p.getPower("Strength");
            if (strPower != null && strPower.amount > 0) {
                // 我们应该扣除的力量值 = 当前衰减层数，但不能超过玩家拥有的实际力量值
                // 比如：玩家力量 3，当前是第 5 段攻击 (decayStep=4)，
                // 理论应该减 4 力量，但玩家只有 3，所以最多把力量收益扣成 0，不能扣成负的。
                int reduction = Math.min(this.decayStep, strPower.amount);

                // 直接修改卡牌的最终伤害数值
                tmp.damage -= reduction;

                // 防止伤害被减成负数 (虽然一般不会，但为了安全)
                if (tmp.damage < 0)
                    tmp.damage = 0;

                // 如果你想让 UI 显示变色，可以保留 isDamageModified，
                // 但因为 tmp 是瞬间打出的，玩家其实看不清数字变化。
            }

            // 3. 打出卡牌
            tmp.use(p, target);

            // 4. 【递归】衰减层数 +1
            if (times > 1) {
                // 注意这里：times - 1 (次数减少), decayStep + 1 (衰减加重)
                AbstractDungeon.actionManager.addToBottom(
                        new ScatterRecursiveAction(p, card, times - 1, decayStep + 1));
            }
        }

        this.isDone = true;
    }
}
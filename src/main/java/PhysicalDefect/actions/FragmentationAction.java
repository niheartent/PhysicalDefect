package PhysicalDefect.actions;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.blue.Strike_Blue; // 引入打击
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import PhysicalDefect.patches.FragmentationPatch;
import PhysicalDefect.patches.FragmentationPatch.CardFields;
// import PhysicalDefect.powers.AlmightyPower; // 假设你的金卡能力叫这个
import PhysicalDefect.powers.AlmightyPower;
import PhysicalDefect.powers.FragmentationPower;

// public class FragmentationAction extends AbstractGameAction {
//     private AbstractCard sourceCard; // 原卡，仅用于全知全能模式复制
//     private int remainingRecursions;
//     private int fixedBaseDamage; // 【新增】固定的碎片基础伤害（未计算力量/易伤前的面板）

//     /**
//      * @param sourceCard          原卡（用于复制特效）
//      * @param remainingRecursions 剩余次数
//      * @param fixedBaseDamage     【关键】传入计算好的 (原面板-力量)/N 的基础值
//      */
//     public FragmentationAction(AbstractCard sourceCard, int remainingRecursions, int fixedBaseDamage) {
//         this.sourceCard = sourceCard;
//         this.remainingRecursions = remainingRecursions;
//         this.fixedBaseDamage = fixedBaseDamage;
//         this.actionType = ActionType.DAMAGE;
//         this.duration = 0.01F;
//     }

//     @Override
//     public void update() {
//         if (remainingRecursions <= 0 || AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
//             this.isDone = true;
//             return;
//         }

//         AbstractPlayer p = AbstractDungeon.player;
//         AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
//                 AbstractDungeon.cardRandomRng);

//         if (target != null && !target.isDeadOrEscaped()) {

//             // 检查是否有【全知全能】能力
//             boolean isAlmighty = p.hasPower(AlmightyPower.POWER_ID);

//             if (isAlmighty) {
//                 // === 全知全能模式 ===
//                 // 复制原卡，保留所有特效
//                 AbstractCard tmp = sourceCard.makeStatEquivalentCopy();
//                 FragmentationPatch.CardFields.isFragmentedInstance.set(tmp, true);

//                 // 这里依然要重新计算一次，为了确保数值显示正确（虽然use里通常会再算一次）
//                 tmp.calculateCardDamage(target);
//                 tmp.use(p, target);

//             } else {
//                 // === 普通模式 (使用替身) ===
//                 // 1. 创建一个白板“打击”，作为计算器
//                 AbstractCard calculatorCard = new Strike_Blue();

//                 // 2. 强行把它的基础伤害改成我们传入的“碎片值”
//                 calculatorCard.damage = this.fixedBaseDamage;
//                 // 4. 只取结果，不打出卡牌
//                 AbstractDungeon.actionManager.addToBottom(
//                         new DamageAction(target,
//                                 new DamageInfo(p, calculatorCard.damage, DamageInfo.DamageType.NORMAL),
//                                 AbstractGameAction.AttackEffect.SLASH_HORIZONTAL)); // 统一使用横劈特效
//             }

//             // 递归
//             if (remainingRecursions > 1) {

//                 // 将固定的 fixedBaseDamage 传递下去，保证数值恒定
//                 AbstractDungeon.actionManager.addToBottom(
//                         new FragmentationAction(sourceCard, remainingRecursions - 1, fixedBaseDamage));
//             }
//         }

//         this.isDone = true;
//     }
// }

public class FragmentationAction extends AbstractGameAction {
    private AbstractCard sourceCard;
    private int hits;
    private int fixedBaseDamage;

    public FragmentationAction(AbstractCard sourceCard, int hits, int fixedBaseDamage) {
        this.sourceCard = sourceCard;
        this.hits = hits;
        this.fixedBaseDamage = fixedBaseDamage;
    }

    @Override
    public void update() {
        // 利用 for 循环将所有攻击动作加入队列
        // 注意：这里使用 addToTop，确保它们优先执行，且不会被怪物的受击反击动作隔开
        for (int i = this.hits - 1; i >= 0; i--) {
            AbstractDungeon.actionManager.addToTop(
                    new FragmentSingleHitAction(this.sourceCard, i));
        }
        this.isDone = true; // 派发完毕，立刻结束
    }
}

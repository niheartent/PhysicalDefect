package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.blue.Strike_Blue;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import PhysicalDefect.patches.AlmightyPatch;
import PhysicalDefect.patches.FragmentationPatch;
import PhysicalDefect.powers.AlmightyPower;

// public class FragmentSingleHitAction extends AbstractGameAction {
//     private AbstractCard sourceCard;
//     private int fixedBaseDamage;
//     private int hitIndex;

//     public FragmentSingleHitAction(AbstractCard sourceCard, int fixedBaseDamage, int hitIndex) {
//         this.sourceCard = sourceCard;
//         this.fixedBaseDamage = fixedBaseDamage;
//         this.actionType = ActionType.DAMAGE;
//         this.hitIndex = hitIndex;
//     }

//     @Override
//     public void update() {
//         AbstractPlayer p = AbstractDungeon.player;
//         this.target = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);

//         if (this.target != null && !this.target.isDeadOrEscaped()) {
//             boolean isAlmighty = p.hasPower(AlmightyPower.POWER_ID);

//             if (isAlmighty) {
//                 // 复制一张卡用于计算
//                 AbstractCard tmp = sourceCard.makeStatEquivalentCopy();
//                 FragmentationPatch.CardFields.isFragmentedInstance.set(tmp, true);
//                 if (AlmightyPatch.isAlmightyBlacklisted(tmp.cardID)) {
//                     int tempDamageBoost = tmp.magicNumber * this.hitIndex;
//                     tmp.calculateCardDamage((AbstractMonster) this.target);
//                     tmp.damage += tempDamageBoost;
//                     addToTop(new DamageAction(this.target,
//                             new DamageInfo(p, tmp.damage, DamageInfo.DamageType.NORMAL),
//                             AbstractGameAction.AttackEffect.SLASH_DIAGONAL)); // 用通用斩击特效替代爪击特效

//                 } else {
//                     tmp.calculateCardDamage((AbstractMonster) this.target);

//                     java.util.ArrayList<AbstractGameAction> beforeActions = new java.util.ArrayList<>(
//                             AbstractDungeon.actionManager.actions);

//                     tmp.use(p, (AbstractMonster) this.target);

//                     java.util.ArrayList<AbstractGameAction> spawnedActions = new java.util.ArrayList<>();
//                     for (AbstractGameAction a : AbstractDungeon.actionManager.actions) {
//                         if (!beforeActions.contains(a)) {
//                             spawnedActions.add(a);
//                         }
//                     }

//                     AbstractDungeon.actionManager.actions.removeAll(spawnedActions);
//                     for (int i = 0; i < spawnedActions.size(); i++) {
//                         AbstractDungeon.actionManager.actions.add(i, spawnedActions.get(i));
//                     }
//                 }

//             } else {

//                 // 【修复核心】：不再使用提前抓取的 fixedBaseDamage，而是动态复制原卡
//                 AbstractCard tmp = sourceCard.makeStatEquivalentCopy();

//                 // 继承碎片化标记，防止某些情况下触发死循环
//                 FragmentationPatch.CardFields.isFragmentedInstance.set(tmp, true);

//                 // 动态计算对当前随机目标的最终伤害（完美解决回响形态伤害为0的问题，且能吃到爪击等卡牌自身的增伤）
//                 tmp.calculateCardDamage((AbstractMonster) this.target);

//                 // 只生成纯粹的伤害动作，不执行 tmp.use()，完美实现“阉割附加特效”的初衷
//                 addToTop(new DamageAction(this.target,
//                         new DamageInfo(p, tmp.damage, DamageInfo.DamageType.NORMAL),
//                         AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));

//                 // AbstractCard calculatorCard = new Strike_Blue();
//                 // calculatorCard.damage = this.fixedBaseDamage;

//                 // addToTop(new DamageAction(this.target,
//                 // new DamageInfo(p, calculatorCard.damage, DamageInfo.DamageType.NORMAL),
//                 // AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
//             }
//         }
//         this.isDone = true;
//     }
// }

public class FragmentSingleHitAction extends AbstractGameAction {
    // 【核心修改1】：把原本在 update 里临时复制的卡，变成 Action 的成员变量，作为“快照”保存
    private AbstractCard cardSnapshot;
    private int hitIndex;

    public FragmentSingleHitAction(AbstractCard sourceCard, int hitIndex) {
        // 【核心修改2】：在卡牌刚刚打出、Action 被创建的瞬间，立刻复制原卡的状态。
        // 此时爪击的成长动作还没来得及执行，完美保留了第一段攻击时的面板和数值。
        this.cardSnapshot = sourceCard.makeStatEquivalentCopy();

        // 提前给快照打上标记，防止嵌套循环
        FragmentationPatch.CardFields.isFragmentedInstance.set(this.cardSnapshot, true);

        this.actionType = ActionType.DAMAGE;
        this.hitIndex = hitIndex;
    }

    @Override
    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        this.target = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);

        if (this.target != null && !this.target.isDeadOrEscaped()) {
            boolean isAlmighty = p.hasPower(AlmightyPower.POWER_ID);

            if (isAlmighty) {
                // 全能模式下，直接使用预先存好的快照
                if (AlmightyPatch.isAlmightyBlacklisted(this.cardSnapshot.cardID)) {
                    int tempDamageBoost = this.cardSnapshot.magicNumber * this.hitIndex;

                    // 动态计算快照对当前随机目标的伤害（吃易伤等目标Debuff）
                    this.cardSnapshot.calculateCardDamage((AbstractMonster) this.target);
                    this.cardSnapshot.damage += tempDamageBoost;

                    addToTop(new DamageAction(this.target,
                            new DamageInfo(p, this.cardSnapshot.damage, DamageInfo.DamageType.NORMAL),
                            AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                } else {
                    this.cardSnapshot.calculateCardDamage((AbstractMonster) this.target);

                    java.util.ArrayList<AbstractGameAction> beforeActions = new java.util.ArrayList<>(
                            AbstractDungeon.actionManager.actions);

                    // 执行快照原本的效果
                    this.cardSnapshot.use(p, (AbstractMonster) this.target);

                    java.util.ArrayList<AbstractGameAction> spawnedActions = new java.util.ArrayList<>();
                    for (AbstractGameAction a : AbstractDungeon.actionManager.actions) {
                        if (!beforeActions.contains(a)) {
                            spawnedActions.add(a);
                        }
                    }

                    AbstractDungeon.actionManager.actions.removeAll(spawnedActions);
                    for (int i = 0; i < spawnedActions.size(); i++) {
                        AbstractDungeon.actionManager.actions.add(i, spawnedActions.get(i));
                    }
                }
            } else {
                // 【核心修改3】：未开启全能时，依然使用快照进行伤害计算，彻底解决爪击错位和回响0伤害问题
                this.cardSnapshot.calculateCardDamage((AbstractMonster) this.target);

                addToTop(new DamageAction(this.target,
                        new DamageInfo(p, this.cardSnapshot.damage, DamageInfo.DamageType.NORMAL),
                        AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
            }
        }
        this.isDone = true;
    }
}
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

public class FragmentSingleHitAction extends AbstractGameAction {
    private AbstractCard sourceCard;
    private int fixedBaseDamage;
    private int hitIndex;

    public FragmentSingleHitAction(AbstractCard sourceCard, int fixedBaseDamage, int hitIndex) {
        this.sourceCard = sourceCard;
        this.fixedBaseDamage = fixedBaseDamage;
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
                // 复制一张卡用于计算
                AbstractCard tmp = sourceCard.makeStatEquivalentCopy();
                FragmentationPatch.CardFields.isFragmentedInstance.set(tmp, true);
                if (AlmightyPatch.isAlmightyBlacklisted(tmp.cardID)) {
                    int tempDamageBoost = tmp.magicNumber * this.hitIndex;
                    tmp.calculateCardDamage((AbstractMonster) this.target);
                    tmp.damage += tempDamageBoost;
                    addToTop(new DamageAction(this.target,
                            new DamageInfo(p, tmp.damage, DamageInfo.DamageType.NORMAL),
                            AbstractGameAction.AttackEffect.SLASH_DIAGONAL)); // 用通用斩击特效替代爪击特效

                } else {
                    tmp.calculateCardDamage((AbstractMonster) this.target);

                    java.util.ArrayList<AbstractGameAction> beforeActions = new java.util.ArrayList<>(
                            AbstractDungeon.actionManager.actions);

                    tmp.use(p, (AbstractMonster) this.target);

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
                AbstractCard calculatorCard = new Strike_Blue();
                calculatorCard.damage = this.fixedBaseDamage;

                addToTop(new DamageAction(this.target,
                        new DamageInfo(p, calculatorCard.damage, DamageInfo.DamageType.NORMAL),
                        AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
            }
        }
        this.isDone = true;
    }
}
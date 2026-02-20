package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import PhysicalDefect.patches.AlmightyPatch;
import PhysicalDefect.patches.FragmentationPatch;
import PhysicalDefect.powers.AlmightyPower;

public class FragmentSingleHitAction extends AbstractGameAction {
    private AbstractCard cardSnapshot;
    private int hitIndex;

    public FragmentSingleHitAction(AbstractCard sourceCard, int hitIndex) {
        this.cardSnapshot = sourceCard.makeStatEquivalentCopy();
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
                if (AlmightyPatch.isAlmightyBlacklisted(this.cardSnapshot.cardID)) {
                    int tempDamageBoost = this.cardSnapshot.magicNumber * this.hitIndex;
                    this.cardSnapshot.calculateCardDamage((AbstractMonster) this.target);
                    this.cardSnapshot.damage += tempDamageBoost;

                    addToTop(new DamageAction(this.target,
                            new DamageInfo(p, this.cardSnapshot.damage, DamageInfo.DamageType.NORMAL),
                            AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
                } else {
                    this.cardSnapshot.calculateCardDamage((AbstractMonster) this.target);

                    java.util.ArrayList<AbstractGameAction> beforeActions = new java.util.ArrayList<>(
                            AbstractDungeon.actionManager.actions);

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
                this.cardSnapshot.calculateCardDamage((AbstractMonster) this.target);

                addToTop(new DamageAction(this.target,
                        new DamageInfo(p, this.cardSnapshot.damage, DamageInfo.DamageType.NORMAL),
                        AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
            }
        }
        this.isDone = true;
    }
}
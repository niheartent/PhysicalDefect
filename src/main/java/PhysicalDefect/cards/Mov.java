package PhysicalDefect.cards;

import PhysicalDefect.actions.MovAction;
import PhysicalDefect.modcore.PhysicalDefect;
import basemod.abstracts.CustomCard;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class Mov extends CustomCard {
    public static final String ID = PhysicalDefect.makeID("Mov");
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String IMG_PATH = PhysicalDefect.assetPath("img/cards/Mov.png");
    private static final int COST = 1;
    private static final int UPGRADED_COST = 0;

    public Mov() {
        super(ID, cardStrings.NAME, IMG_PATH, COST,
                cardStrings.DESCRIPTION,
                CardType.SKILL, CardColor.BLUE, CardRarity.UNCOMMON, CardTarget.SELF);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractDungeon.actionManager.addToBottom(new MovAction());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(UPGRADED_COST);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Mov();
    }
}
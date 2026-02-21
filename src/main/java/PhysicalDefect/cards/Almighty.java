package PhysicalDefect.cards;

import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.powers.AlmightyPower;
import PhysicalDefect.powers.FragmentationPower;

public class Almighty extends CustomCard {
    // 卡牌ID：PhysicalDefect:Almighty
    public static final String ID = PhysicalDefect.makeID("Almighty");
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings(ID);

    // 图片路径
    public static final String IMG = PhysicalDefect.assetPath("img/cards/Almighty.png");

    public Almighty() {
        super(ID, cardStrings.NAME, IMG, 2, cardStrings.DESCRIPTION,
                CardType.POWER, CardColor.BLUE,
                CardRarity.RARE, CardTarget.SELF);

        // 基础属性：虚无
        this.isEthereal = false;
        this.baseMagicNumber = 0;
        this.magicNumber = this.baseMagicNumber;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 核心效果：赋予全知全能能力
        addToBot(new ApplyPowerAction(p, p, new AlmightyPower(p)));
        if (this.upgraded && this.magicNumber > 0) {
            addToBot(new ApplyPowerAction(p, p, new FragmentationPower(p, this.magicNumber), this.magicNumber));
        }
    }

    @Override
    public void upgrade() {
        if (!upgraded) {
            upgradeName();
            upgradeMagicNumber(1);
            this.rawDescription = cardStrings.UPGRADE_DESCRIPTION;
            initializeDescription();
        }
    }
}
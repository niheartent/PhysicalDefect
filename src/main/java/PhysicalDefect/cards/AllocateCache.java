package PhysicalDefect.cards;

import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.orbs.Register;
import basemod.abstracts.CustomCard;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class AllocateCache extends CustomCard {
    public static final String ID = PhysicalDefect.makeID("AllocateCache");
    // 记得在本地化文件 CardStrings.json 中配置名字和描述
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String IMG_PATH = PhysicalDefect.assetPath("img/cards/AllocateCache.png");
    private static final int COST = 1;
    private static final int UPGRADED_COST = 0;

    public AllocateCache() {
        super(ID, cardStrings.NAME, IMG_PATH, COST, cardStrings.DESCRIPTION,
                CardType.SKILL, CardColor.BLUE, CardRarity.COMMON, CardTarget.SELF);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChannelAction(new Register()));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(UPGRADED_COST);
            this.initializeDescription();
        }
    }
}
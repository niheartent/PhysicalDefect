package PhysicalDefect.relics;

import basemod.abstracts.CustomRelic;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
// import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import PhysicalDefect.modcore.PhysicalDefect;

public class BackupBattery extends CustomRelic {
    public static final String ID = PhysicalDefect.makeID("BackupBattery");
    private static final String IMG_PATH = PhysicalDefect.assetPath("img/relics/BackupBattery.png");
    private static final RelicTier RELIC_TIER = RelicTier.STARTER;
    private static final LandingSound LANDING_SOUND = LandingSound.CLINK;

    private boolean triggeredThisCombat = false;

    public BackupBattery() {
        super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
    }

    @Override
    public void atPreBattle() {
        this.triggeredThisCombat = false; // 重置开关
        this.grayscale = false; // 点亮遗物
        this.beginLongPulse(); // (可选) 让遗物在生效前脉冲闪烁，提示玩家
    }

    @Override
    public void atTurnStart() {
        if (!this.triggeredThisCombat) {

            this.flash();
            this.stopPulse();
            this.addToBot(new ChannelAction(new Plasma()));
            this.triggeredThisCombat = true;
            this.grayscale = true;
        }
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new BackupBattery();
    }
}
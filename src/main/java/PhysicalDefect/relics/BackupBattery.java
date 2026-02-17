package PhysicalDefect.relics;

import basemod.abstracts.CustomRelic;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.combat.OrbFlareEffect;

import PhysicalDefect.modcore.PhysicalDefect;

public class BackupBattery extends CustomRelic {
    // 遗物ID（此处的ModHelper在“04 - 本地化”的进阶中提到）
    public static final String ID = PhysicalDefect.makeID("BackupBattery");

    private static final String IMG_PATH = PhysicalDefect.assetPath("img/relics/BackupBattery.png");
    private static final RelicTier RELIC_TIER = RelicTier.STARTER;
    private static final LandingSound LANDING_SOUND = LandingSound.CLINK;

    public BackupBattery() {
        super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
        // 如果你需要轮廓图，取消注释下面一行并注释上面一行，不需要就删除
        // super(ID, ImageMaster.loadImage(IMG_PATH),
        // ImageMaster.loadImage(OUTLINE_PATH), RELIC_TIER, LANDING_SOUND);
    }

    @Override
    public void atBattleStart() {
        this.flash(); // 遗物闪烁特效

        // 1. 播放球体闪烁视觉效果
        AbstractDungeon.actionManager.addToBottom(
                new VFXAction(new OrbFlareEffect(new Plasma(), OrbFlareEffect.OrbFlareColor.PLASMA), 0.1F));

        // 2. 生成一个等离子球
        AbstractDungeon.actionManager.addToBottom(new ChannelAction(new Plasma()));
    }

    // 获取遗物描述，但原版游戏只在初始化和获取遗物时调用，故该方法等于初始描述
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public AbstractRelic makeCopy() {
        return new BackupBattery();
    }
}
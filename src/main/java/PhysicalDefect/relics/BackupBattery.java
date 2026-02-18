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
    // 遗物ID
    public static final String ID = PhysicalDefect.makeID("BackupBattery");

    private static final String IMG_PATH = PhysicalDefect.assetPath("img/relics/BackupBattery.png");
    private static final RelicTier RELIC_TIER = RelicTier.STARTER;
    private static final LandingSound LANDING_SOUND = LandingSound.CLINK;

    public BackupBattery() {
        super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
    }

    // 战斗开始时，确保遗物不是灰色的
    @Override
    public void atBattleStart() {
        this.grayscale = false;
    }

    // 回合开始时调用
    @SuppressWarnings("static-access")
    @Override
    public void atTurnStart() {
        // 检查是否是第 2 回合
        if (AbstractDungeon.actionManager.turn == 2) {
            this.flash(); // 遗物闪烁特效

            // 1. 播放球体闪烁视觉效果
            AbstractDungeon.actionManager.addToBottom(
                    new VFXAction(new OrbFlareEffect(new Plasma(), OrbFlareEffect.OrbFlareColor.PLASMA), 0.1F));

            // 2. 生成一个等离子球
            AbstractDungeon.actionManager.addToBottom(new ChannelAction(new Plasma()));

            // 3. 将遗物变灰，表示本场战斗已使用过
            this.grayscale = true;
        }
    }

    // 获取遗物描述
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public AbstractRelic makeCopy() {
        return new BackupBattery();
    }
}
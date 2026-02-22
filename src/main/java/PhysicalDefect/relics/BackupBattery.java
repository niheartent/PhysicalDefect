package PhysicalDefect.relics;

import basemod.abstracts.CustomRelic;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
// import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import PhysicalDefect.modcore.PhysicalDefect;

// public class BackupBattery extends CustomRelic {
//     public static final String ID = PhysicalDefect.makeID("BackupBattery");
//     private static final String IMG_PATH = PhysicalDefect.assetPath("img/relics/BackupBattery.png");
//     private static final RelicTier RELIC_TIER = RelicTier.STARTER;
//     private static final LandingSound LANDING_SOUND = LandingSound.CLINK;

//     private boolean triggeredThisCombat = false;

//     public BackupBattery() {
//         super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
//     }

//     @Override
//     public void atPreBattle() {
//         this.triggeredThisCombat = false; // 重置开关
//         this.grayscale = false; // 点亮遗物
//         this.beginLongPulse(); // (可选) 让遗物在生效前脉冲闪烁，提示玩家
//     }

//     @Override
//     public void atTurnStart() {
//         if (!this.triggeredThisCombat) {

//             this.flash();
//             this.stopPulse();
//             this.addToBot(new ChannelAction(new Plasma()));
//             this.triggeredThisCombat = true;
//             this.grayscale = true;
//         }
//     }

//     @Override
//     public String getUpdatedDescription() {
//         return this.DESCRIPTIONS[0];
//     }

//     @Override
//     public AbstractRelic makeCopy() {
//         return new BackupBattery();
//     }
// }

public class BackupBattery extends CustomRelic {
    public static final String ID = PhysicalDefect.makeID("BackupBattery");
    private static final String IMG_PATH = PhysicalDefect.assetPath("img/relics/BackupBattery.png");
    private static final RelicTier RELIC_TIER = RelicTier.STARTER; // 初始遗物
    private static final LandingSound LANDING_SOUND = LandingSound.CLINK;

    private boolean triggeredThisCombat = false;

    public BackupBattery() {
        super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
    }

    // 战斗开始前：重置遗物状态
    @Override
    public void atPreBattle() {
        this.triggeredThisCombat = false;
        this.grayscale = false; // 点亮遗物图标
    }

    // 核心判定：当玩家失去生命值时触发（被格挡全挡住不掉血则不触发）
    @Override
    public void wasHPLost(int damageAmount) {
        // 条件：伤害大于0，且本场战斗尚未触发过
        if (damageAmount > 0 && !this.triggeredThisCombat) {
            this.flash(); // 遗物闪烁动画
            // 生成 1 个等离子球
            this.addToBot(new ChannelAction(new Plasma()));

            // 标记为已触发，并变灰
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
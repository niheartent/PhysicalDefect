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

    // 【核心修改1】定义一个私有变量，用来记录本场战斗是否已经触发过
    private boolean triggeredThisCombat = false;

    public BackupBattery() {
        super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
    }

    // 【核心修改2】在战斗预备阶段重置这个开关
    // atPreBattle 是比 atBattleStart 更早的时机，专门用于重置数据
    @Override
    public void atPreBattle() {
        this.triggeredThisCombat = false; // 重置开关
        this.grayscale = false; // 点亮遗物
        this.beginLongPulse(); // (可选) 让遗物在生效前脉冲闪烁，提示玩家
    }

    // 【核心修改3】回合开始时的逻辑
    @Override
    public void atTurnStart() {
        // 不再检测 turn == 1，而是检测“是否还没触发过”
        if (!this.triggeredThisCombat) {

            this.flash();
            this.stopPulse(); // 停止脉冲

            // 生成等离子球
            this.addToBot(new ChannelAction(new Plasma()));

            // 【关键】立即锁死开关，并将遗物变灰
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
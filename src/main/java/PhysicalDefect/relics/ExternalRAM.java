package PhysicalDefect.relics;

import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.orbs.Register;
import basemod.abstracts.CustomRelic;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.helpers.ImageMaster;

public class ExternalRAM extends CustomRelic {
    public static final String ID = PhysicalDefect.makeID("ExternalRAM");
    private static final String IMG_PATH = PhysicalDefect.assetPath("img/relics/ExternalRAM.png");
    private static final LandingSound LANDING_SOUND = LandingSound.CLINK;
    private static final RelicTier RELIC_TIER = RelicTier.UNCOMMON; // 初始遗物

    public ExternalRAM() {
        super(ID, ImageMaster.loadImage(IMG_PATH), RELIC_TIER, LANDING_SOUND);
    }

    @Override
    public void atPreBattle() {
        this.grayscale = false; // 点亮遗物图标
    }

    // 战斗开始时的触发逻辑
    @Override
    public void atBattleStart() {
        // 闪烁一下遗物图标，提示玩家生效了
        this.flash();
        // 将生成寄存器球的动作压入队列
        this.addToBot(new ChannelAction(new Register()));
        this.grayscale = true;
    }

    @Override
    public String getUpdatedDescription() {
        // 返回在 RelicStrings.json 中配置的描述文本
        return DESCRIPTIONS[0];
    }

    @Override
    public CustomRelic makeCopy() {
        return new ExternalRAM();
    }
}
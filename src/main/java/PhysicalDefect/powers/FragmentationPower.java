package PhysicalDefect.powers;

import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

import PhysicalDefect.modcore.PhysicalDefect;

public class FragmentationPower extends AbstractPower {
    public static final String POWER_ID = PhysicalDefect.makeID("FragmentationPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    public static final String NAME = powerStrings.NAME;
    public static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    public static final int POOLNUM = 3;

    public FragmentationPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.type = PowerType.BUFF;
        this.isTurnBased = false;
        // this.loadRegion("fragmentation"); // 请确保你有对应图片，或者暂时借用其他图标
        // this.img = null; // 如果没有图片，先设为null防止崩，或者借用strength图标
        // this.loadRegion("static_discharge");
        updateDescription();
        this.img = new Texture(PhysicalDefect.assetPath("img/powers/Fragmentation.png"));
    }

    @Override
    public void updateDescription() {
        // 描述建议：
        // DESCRIPTIONS[0] = "你的单体攻击会被拆解为 1 + "
        // DESCRIPTIONS[1] = " 段。首段攻击目标不变，后续碎片攻击随机敌人。"
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}
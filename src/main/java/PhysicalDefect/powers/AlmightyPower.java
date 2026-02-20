package PhysicalDefect.powers;

import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import PhysicalDefect.modcore.PhysicalDefect;

public class AlmightyPower extends AbstractPower {
    public static final String POWER_ID = PhysicalDefect.makeID("AlmightyPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    public static final String NAME = powerStrings.NAME;
    public static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public AlmightyPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = -1; // -1 表示这是一个状态类能力，不显示层数
        this.type = PowerType.BUFF;

        // 加载图片
        this.img = new Texture(PhysicalDefect.assetPath("img/powers/Almighty.png"));

        updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}
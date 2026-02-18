package PhysicalDefect.orbs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.combat.OrbFlareEffect;
import com.megacrit.cardcrawl.vfx.combat.PlasmaOrbActivateEffect; //以此特效为例，你可以换
import com.megacrit.cardcrawl.vfx.combat.PlasmaOrbPassiveEffect;

import basemod.abstracts.CustomOrb;
import PhysicalDefect.modcore.PhysicalDefect; // 你的主类包名，用于获取资源路径

public class SensorOrb extends CustomOrb {

    // 1. 定义 ID
    public static final String ORB_ID = PhysicalDefect.makeID("SensorOrb");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    public static final String[] DESC = orbString.DESCRIPTION;

    // 2. 构造函数
    public SensorOrb() {

        super(
                ORB_ID,
                orbString.NAME,
                1, // passiveAmount: 被动数值（回合开始抽牌数）
                2, // evokeAmount: 激发数值（激发抽牌数）
                DESC[0], // 这里的描述只是占位，后面 updateDescription 会覆盖
                "", // 这里的图片路径填空字符串，因为我们在下面手动加载贴图，或者你可以填具体的路径
                null // 假如你有贴图的话
        );

        // 借用等离子球的图

        // 如果你暂时没有图片，可以用这两行代码借用原版【等离子球】的图片和特效颜色
        this.img = ImageMaster.ORB_LIGHTNING;
        this.angle = MathUtils.random(360.0F);

        // 这一步是更新描述文本
        updateDescription();
    }

    // 3. 核心：更新描述文本
    @Override
    public void updateDescription() {
        // applyFocus(); // 确保数值受到集中影响（虽然对于抽牌球，通常我们不希望抽牌数受集中加成）/

        // 注意：通常抽牌球是不吃【集中】加成的！
        // 如果你想让它吃集中（比如负集中导致抽牌减少？），就保留 applyFocus。
        // 如果不想吃集中（固定抽1/抽2），请在下面重置数值：
        // this.passiveAmount = this.basePassiveAmount;
        // this.evokeAmount = this.baseEvokeAmount;

        // description 拼接： "回合开始时，抽 #b1 张牌。 NL 激发时，抽 #b2 张牌。"
        // DESC[0] = "回合开始时，抽 #b"
        // DESC[1] = " 张牌。"
        // DESC[2] = "激发时，抽 #b"
        // DESC[3] = " 张牌。"
        this.description = DESC[0] + this.passiveAmount + DESC[1] + DESC[2] + this.evokeAmount + DESC[3];
    }

    // 4. 被动效果：回合开始触发
    @Override
    public void onStartOfTurn() {
        // 添加特效（可选，增加打击感）
        AbstractDungeon.effectList.add(new OrbFlareEffect(this, OrbFlareEffect.OrbFlareColor.LIGHTNING));

        // 执行动作：抽牌
        // 注意：addToBottom 意味着在所有“回合开始”动作的末尾执行（即发牌后）
        AbstractDungeon.actionManager.addToBottom(new DrawCardAction(AbstractDungeon.player, this.passiveAmount));
    }

    // 5. 主动激发效果
    @Override
    public void onEvoke() {
        // 执行动作：抽牌
        AbstractDungeon.actionManager.addToBottom(new DrawCardAction(AbstractDungeon.player, this.evokeAmount));
    }

    // 6. 视觉效果：生成球时的音效
    @Override
    public void playChannelSFX() {
        // 使用原版声音，"ORB_LIGNTNING" 听起来比较像高科技设备
        CardCrawlGame.sound.play("ORB_LIGNTNING_CHANNEL", 0.1F);
    }

    // 7. 复制方法（必须实现，用于生成新实例）
    @Override
    public AbstractOrb makeCopy() {
        return new SensorOrb();
    }

    // 8. 渲染特效（可选）
    // 这里借用了等离子球的粒子效果，让它看起来一直在动
    @Override
    public void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
        super.render(sb);
        // 如果想加一些闪烁的粒子，可以在 updateAnimation 里加，或者直接 copy 原版球的代码
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        // 产生一些被动粒子，让球看起来是活的
        this.angle += Gdx.graphics.getDeltaTime() * 45.0F; // 让球转起来
    }
}
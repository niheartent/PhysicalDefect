package PhysicalDefect.characters;

import PhysicalDefect.modcore.PhysicalDefect; // 引用你的主类
import basemod.abstracts.CustomPlayer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
// import com.badlogic.gdx.graphics.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.blue.Strike_Blue;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.cutscenes.CutscenePanel;
import com.megacrit.cardcrawl.events.beyond.SpireHeart;
import com.megacrit.cardcrawl.events.city.Vampires;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.ui.panels.energyorb.EnergyOrbBlue;

import java.util.ArrayList;

// 1. 继承 CustomPlayer 而不是 AbstractPlayer
public class MyPhysicalDefect extends CustomPlayer {

    // 定义角色 ID (请确保这和你在主类里定义的枚举一致)
    public static final AbstractPlayer.PlayerClass ENUM_ID = PhysicalDefect.THE_PHYSICAL_DEFECT;

    // 加载本地化文件 (记得在 localization/zhs/CharacterStrings.json 里写好内容)
    // 对应原版源码第 36 行
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack
            .getCharacterString("PhysicalDefect:MyPhysicalDefect");
    private static final String[] NAMES = characterStrings.NAMES;
    private static final String[] TEXT = characterStrings.TEXT;

    public MyPhysicalDefect(String name) {
        // 2. 构造函数
        // 参数2: 你的角色枚举
        // 参数3: 能量球的一组纹理 (这里偷懒直接用了 null，后面代码会处理，或者你可以new EnergyOrbBlue())
        // 参数4: 骨骼动画 (直接抄原版第 54 行的路径!)
        super(name, ENUM_ID, new EnergyOrbBlue(), null, null);

        // 3. 初始化角色模型 (抄原版第 50-53 行)
        // 这里的路径完全指向原版游戏资源，不需要你复制图片文件
        initializeClass(null,
                "images/characters/defect/shoulder2.png", // 抄原版
                "images/characters/defect/shoulder.png", // 抄原版
                "images/characters/defect/corpse.png", // 抄原版
                getLoadout(),
                20.0F, -10.0F, 220.0F, 290.0F, // 抄原版：碰撞箱大小
                new EnergyManager(3)); // 初始3费

        // 4. 加载动画 (抄原版第 54-57 行)
        loadAnimation("images/characters/defect/idle/skeleton.atlas", "images/characters/defect/idle/skeleton.json",
                1.0F);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        this.stateData.setMix("Hit", "Idle", 0.1f);
        e.setTimeScale(0.9F);
    }

    // --- 核心配置 ---

    // 5. 初始卡组 (参考原版 getStartingDeck，但改成你要的)
    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<>();
        retVal.add("Strike_B");
        retVal.add("Strike_B");
        retVal.add("Strike_B");
        retVal.add("Strike_B");
        retVal.add("Defend_B");
        retVal.add("Defend_B");
        retVal.add("Defend_B");
        retVal.add("Defend_B");
        retVal.add("Reprogram");
        return retVal;
    }

    // 6. 初始遗物 (参考原版 getStartingRelics)
    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<>();
        retVal.add(PhysicalDefect.makeID("BackupBattery"));
        return retVal;
    }

    // 7. 角色选择界面的信息 (参考原版 getLoadout)
    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(
                NAMES[0], // 名字
                TEXT[0], // 描述
                75, // 初始血量
                75, // 最大血量
                3, // 初始充能球栏位 (物理机也许不需要球？或者填 3)
                99, // 初始金币
                5, // 每回合抽牌数
                this,
                getStartingRelics(),
                getStartingDeck(),
                false);
    }

    // 8. 你的卡牌颜色 (重点！返回 BLUE 才能用故障机器人的卡)
    // 对应原版 101 行
    @Override
    public AbstractCard.CardColor getCardColor() {
        return AbstractCard.CardColor.BLUE;
    }

    // --- 视觉效果 (全部抄原版) ---

    @Override
    public String getTitle(AbstractPlayer.PlayerClass plyrClass) {
        return NAMES[0];
    }

    @Override
    public Color getCardRenderColor() {
        return Color.SKY;
    }

    @Override
    public Color getCardTrailColor() {
        return Color.SKY.cpy(); // 抄原版 125 行
    }

    @Override
    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontBlue; // 抄原版 144 行
    }

    @Override
    public String getCustomModeCharacterButtonSoundKey() {
        return "ATTACK_MAGIC_BEAM_SHORT"; // 抄原版 198 行
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        // 抄原版 193 行：选人时的音效和震动
        CardCrawlGame.sound.playA("ATTACK_MAGIC_BEAM_SHORT", MathUtils.random(-0.2F, 0.2F));
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    public String getPortraitImageName() {
        // 这也是原版故障机器人的立绘路径
        // 如果你想用原版的，直接返回这个文件名（游戏会自动在 images/ui/charSelect/ 下寻找）
        return "defectPortrait.jpg";
    }

    @Override
    public void damage(DamageInfo info) {
        // 逻辑：如果是攻击伤害（不是荆棘），且伤害超过了当前的护甲（真掉血了）
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output - this.currentBlock > 0) {
            // 播放 "Hit" 动画
            AnimationState.TrackEntry e = this.state.setAnimation(0, "Hit", false);
            // 播放完后自动切回 "Idle"
            this.state.addAnimation(0, "Idle", true, 0.0F);
            e.setTime(0.9F); // 设置动画起始时间点
        }
        super.damage(info);
    }

    // 控制能量球的渲染
    @Override
    public void renderOrb(SpriteBatch sb, boolean enabled, float current_x, float current_y) {
        this.energyOrb.renderOrb(sb, enabled, current_x, current_y);
    }

    // 控制能量球的旋转/浮动更新
    @Override
    public void updateOrb(int orbCount) {
        this.energyOrb.updateOrb(orbCount);
    }

    // 获取左上角能量球的贴图
    @Override
    public Texture getEnergyImage() {
        return ImageMaster.BLUE_ORB_FLASH_VFX;
    }

    @Override
    public TextureAtlas.AtlasRegion getOrb() {
        return AbstractCard.orb_blue;
    }

    // --- 杂项 ---

    @Override
    public AbstractPlayer newInstance() {
        return new MyPhysicalDefect(this.name);
    }

    @Override
    public String getSpireHeartText() {
        return SpireHeart.DESCRIPTIONS[10]; // 抄原版打心脏的剧情文本
    }

    @Override
    public Color getSlashAttackColor() {
        return Color.SKY;
    }

    @Override
    public String getVampireText() {
        return Vampires.DESCRIPTIONS[5]; // 吸血鬼事件文本
    }

    @Override
    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        // 抄原版 232 行：打心脏时的特效
        return new AbstractGameAction.AttackEffect[] {
                AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE,
                AbstractGameAction.AttackEffect.SLASH_DIAGONAL,
                AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE,
                AbstractGameAction.AttackEffect.SLASH_DIAGONAL
        };
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 5;
    }

    @Override
    public String getLocalizedCharacterName() {
        return NAMES[0];
    }

    @Override
    public AbstractCard getStartCardForEvent() {
        return new Strike_Blue();
    }

    // =================================================================
    // 2. 修改碎心通关漫画 (打败心脏后的 3 张分镜)
    // =================================================================
    @Override
    public java.util.List<CutscenePanel> getCutscenePanels() {
        java.util.List<CutscenePanel> panels = new java.util.ArrayList<>();

        // 故障机器人的原版三格漫画。
        // 第一个分镜附带了机器人专属的“发射激光”音效 (ATTACK_DEFECT_BEAM)
        panels.add(new CutscenePanel("images/scenes/defect1.png", "ATTACK_DEFECT_BEAM"));
        panels.add(new CutscenePanel("images/scenes/defect2.png"));
        panels.add(new CutscenePanel("images/scenes/defect3.png"));

        return panels;
    }
}
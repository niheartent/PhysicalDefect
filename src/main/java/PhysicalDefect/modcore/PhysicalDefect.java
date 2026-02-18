package PhysicalDefect.modcore;

import java.util.Properties;

import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.CardStrings;
// import com.megacrit.cardcrawl.localization.CharacterStrings;
// import com.megacrit.cardcrawl.localization.EventStrings;
// import com.megacrit.cardcrawl.localization.MonsterStrings;
// import com.megacrit.cardcrawl.localization.OrbStrings;
// import com.megacrit.cardcrawl.localization.PotionStrings;
// import com.megacrit.cardcrawl.localization.PowerStrings;
// import com.megacrit.cardcrawl.localization.RelicStrings;
// import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;

import PhysicalDefect.characters.MyPhysicalDefect;
import PhysicalDefect.relics.BackupBattery;
import basemod.BaseMod;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.helpers.RelicType;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;

/**
 * Hello world!
 *
 */

@SpireInitializer
public class PhysicalDefect
        implements EditCardsSubscriber, EditStringsSubscriber, EditCharactersSubscriber, EditRelicsSubscriber,
        PostInitializeSubscriber {

    public static String makeID(String id) {
        return MOD_ID + ":" + id;
    }

    public static String assetPath(String path) {
        return MOD_ID + "/" + path;
    }

    /*----------------------------------------模组核心----------------------------------------------- */
    public static final String MOD_ID = "PhysicalDefect";
    public static final String AUTHOR = "Nihe";
    public static final String DESCRIPTION = "The 4TH Favorite Character";
    @SpireEnum
    public static AbstractPlayer.PlayerClass THE_PHYSICAL_DEFECT;
    public static boolean enableNegativeFocus = false;
    public static SpireConfig modConfig;

    public PhysicalDefect() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new PhysicalDefect();
    }

    @Override
    public void receiveEditCards() {
        // BaseMod.addCard(new Strike());
    }

    private Settings.GameLanguage languageSupport() {
        switch (Settings.language) {
            case ZHS:
                return Settings.language;
            default:
                return Settings.GameLanguage.ENG;
        }
    }

    public void receiveEditStrings() {
        Settings.GameLanguage language = languageSupport();
        loadLocStrings(Settings.GameLanguage.ENG);
        if (!language.equals(Settings.GameLanguage.ENG)) {
            loadLocStrings(language);
        }
    }

    @Override
    public void receiveEditCharacters() {
        // 1. 角色按钮图：直接引用原版故障机器人的选择按钮
        String MY_CHARACTER_BUTTON = "images/ui/charSelect/defectButton.png";

        // 2. 角色全屏立绘：直接引用原版故障机器人的背景图
        String MY_CHARACTER_PORTRAIT = "images/ui/charSelect/defectPortrait.jpg";

        // 3. 注册角色
        // 参数含义：实例对象, 按钮路径, 立绘路径, 角色枚举ID
        BaseMod.addCharacter(
                new MyPhysicalDefect(CardCrawlGame.playerName),
                MY_CHARACTER_BUTTON,
                MY_CHARACTER_PORTRAIT,
                PhysicalDefect.THE_PHYSICAL_DEFECT);
    }

    private void loadLocStrings(Settings.GameLanguage language) {
        String path = "localization/" + language.toString().toLowerCase() + "/";
        BaseMod.loadCustomStringsFile(CardStrings.class, assetPath(path +
                "CardStrings.json"));
        BaseMod.loadCustomStringsFile(CharacterStrings.class, assetPath(path +
                "CharacterStrings.json"));
        BaseMod.loadCustomStringsFile(RelicStrings.class, assetPath(path +
                "RelicStrings.json"));
        BaseMod.loadCustomStringsFile(UIStrings.class, assetPath(path +
                "UIStrings.json"));

        BaseMod.loadCustomStringsFile(OrbStrings.class, assetPath(path +
                "OrbStrings.json"));
        // BaseMod.loadCustomStringsFile(EventStrings.class, assetPath(path +
        // "EventStrings.json"));

        // BaseMod.loadCustomStringsFile(PotionStrings.class, assetPath(path +
        // "PotionStrings.json"));

        // BaseMod.loadCustomStringsFile(MonsterStrings.class, assetPath(path +
        // "monsters.json"));
        // BaseMod.loadCustomStringsFile(PowerStrings.class, assetPath(path +
        // "PowerStrings.json"));

    }

    @Override
    public void receiveEditRelics() {
        BaseMod.addRelic(new BackupBattery(), RelicType.SHARED);
    }

    public static void loadConfig() {
        try {
            Properties defaults = new Properties();
            defaults.setProperty("enableNegativeFocus", "true");

            // "PhysicalDefect" 是文件名，"config" 是文件后缀
            modConfig = new SpireConfig("PhysicalDefect", "config", defaults);

            enableNegativeFocus = modConfig.getBool("enableNegativeFocus");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. 注册配置菜单
    @Override
    public void receivePostInitialize() {
        // 加载配置
        loadConfig();

        // 创建设置面板
        ModPanel settingsPanel = new ModPanel();

        UIStrings configStrings = CardCrawlGame.languagePack.getUIString(makeID("Config"));
        String labelText = configStrings.TEXT[0];

        // 创建开关按钮
        ModLabeledToggleButton enableFocusBtn = new ModLabeledToggleButton(
                labelText,
                350.0f, // x 坐标
                700.0f, // y 坐标
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                enableNegativeFocus, // 初始状态
                settingsPanel,
                (label) -> {
                }, // hover 逻辑
                (button) -> { // 点击逻辑
                    enableNegativeFocus = button.enabled;
                    try {
                        modConfig.setBool("enableNegativeFocus", enableNegativeFocus);
                        modConfig.save();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        settingsPanel.addUIElement(enableFocusBtn);

        // 注册到 BaseMod (你需要准备一张 badge.png 图片作为 Mod 图标)
        Texture badgeTexture = new Texture(assetPath("/img/badge.png"));
        BaseMod.registerModBadge(badgeTexture, MOD_ID, AUTHOR, DESCRIPTION, settingsPanel);
    }

}

package PhysicalDefect.modcore;

import java.util.Properties;

import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import PhysicalDefect.cards.Almighty;
import PhysicalDefect.characters.MyPhysicalDefect;
import PhysicalDefect.relics.BackupBattery;
import basemod.BaseMod;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.helpers.RelicType;
import basemod.interfaces.EditCardsSubscriber;
import basemod.interfaces.EditCharactersSubscriber;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;

import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;

/**
 * Hello world!
 *
 */

@SpireInitializer
public class PhysicalDefect
        implements EditCardsSubscriber,
        EditStringsSubscriber,
        EditCharactersSubscriber,
        EditRelicsSubscriber,
        EditKeywordsSubscriber,
        PostInitializeSubscriber {
    // =================================================================
    // 1. 核心
    // =================================================================

    public static final String MOD_ID = "PhysicalDefect";
    public static final String AUTHOR = "Nihe";
    public static final String DESCRIPTION = "The 4TH Favorite Character";
    @SpireEnum
    public static AbstractPlayer.PlayerClass THE_PHYSICAL_DEFECT;
    public static boolean enableFragmentation = false;
    public static SpireConfig modConfig;

    public static boolean shouldAddDescription() {
        return PhysicalDefect.enableFragmentation &&
                AbstractDungeon.player instanceof MyPhysicalDefect;
    }

    private Settings.GameLanguage languageSupport() {
        switch (Settings.language) {
            case ZHS:
                return Settings.language;
            default:
                return Settings.GameLanguage.ENG;
        }
    }

    public static void loadConfig() {
        try {
            Properties defaults = new Properties();
            defaults.setProperty("enableNegativeFocus", "true");
            modConfig = new SpireConfig("PhysicalDefect", "config", defaults);
            enableFragmentation = modConfig.getBool("enableNegativeFocus");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PhysicalDefect() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new PhysicalDefect();
    }

    @Override
    public void receiveEditCards() {
        BaseMod.addCard(new Almighty());
    }

    @Override
    public void receiveEditCharacters() {
        String MY_CHARACTER_BUTTON = "images/ui/charSelect/defectButton.png";
        String MY_CHARACTER_PORTRAIT = "images/ui/charSelect/defectPortrait.jpg";
        BaseMod.addCharacter(
                new MyPhysicalDefect(CardCrawlGame.playerName),
                MY_CHARACTER_BUTTON,
                MY_CHARACTER_PORTRAIT,
                PhysicalDefect.THE_PHYSICAL_DEFECT);
    }

    public void receiveEditStrings() {
        Settings.GameLanguage language = languageSupport();
        loadLocStrings(Settings.GameLanguage.ENG);
        if (!language.equals(Settings.GameLanguage.ENG)) {
            loadLocStrings(language);
        }
    }

    @Override
    public void receiveEditKeywords() {
        Settings.GameLanguage language = languageSupport();
        loadLocKeywords(Settings.GameLanguage.ENG);
        if (!language.equals(Settings.GameLanguage.ENG)) {
            loadLocKeywords(language);
        }
    }

    @Override
    public void receiveEditRelics() {
        BaseMod.addRelic(new BackupBattery(), RelicType.SHARED);
    }

    @Override
    public void receivePostInitialize() {
        loadConfig();
        ModPanel settingsPanel = new ModPanel();
        UIStrings configStrings = CardCrawlGame.languagePack.getUIString(makeID("Config"));
        String labelText = configStrings.TEXT[0];
        ModLabeledToggleButton enableFocusBtn = new ModLabeledToggleButton(
                labelText,
                350.0f, // x 坐标
                700.0f, // y 坐标
                Settings.CREAM_COLOR,
                FontHelper.charDescFont,
                enableFragmentation, // 初始状态
                settingsPanel,
                (label) -> {
                }, // hover 逻辑
                (button) -> { // 点击逻辑
                    enableFragmentation = button.enabled;
                    try {
                        modConfig.setBool("enableNegativeFocus", enableFragmentation);
                        modConfig.save();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        settingsPanel.addUIElement(enableFocusBtn);
        Texture badgeTexture = new Texture(assetPath("/img/badge.png"));
        BaseMod.registerModBadge(badgeTexture, MOD_ID, AUTHOR, DESCRIPTION, settingsPanel);
    }

    // =================================================================
    // 2. 本地化
    // =================================================================
    public static String makeID(String id) {
        return MOD_ID + ":" + id;
    }

    public static String assetPath(String path) {
        return MOD_ID + "/" + path;
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

        BaseMod.loadCustomStringsFile(PowerStrings.class, assetPath(path +
                "PowerStrings.json"));

        // BaseMod.loadCustomStringsFile(EventStrings.class, assetPath(path +
        // "EventStrings.json"));

        // BaseMod.loadCustomStringsFile(PotionStrings.class, assetPath(path +
        // "PotionStrings.json"));

        // BaseMod.loadCustomStringsFile(MonsterStrings.class, assetPath(path +
        // "monsters.json"));

    }

    public static class Keyword {
        public String PROPER_NAME;
        public String[] NAMES;
        public String DESCRIPTION;
    }

    private void loadLocKeywords(Settings.GameLanguage language) {
        String path = "localization/" + language.toString().toLowerCase() + "/";
        Gson gson = new Gson();
        String json = Gdx.files.internal(assetPath(path + "KeywordStrings.json"))
                .readString(String.valueOf(StandardCharsets.UTF_8));
        Keyword[] keywords = gson.fromJson(json, Keyword[].class);

        if (keywords != null) {
            for (Keyword keyword : keywords) {
                BaseMod.addKeyword("physicaldefect", keyword.PROPER_NAME, keyword.NAMES, keyword.DESCRIPTION);
            }
        }
    }

}

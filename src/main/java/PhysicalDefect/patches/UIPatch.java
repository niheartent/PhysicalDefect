package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatches;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.blue.BiasedCognition;
import com.megacrit.cardcrawl.cards.blue.Claw;
import com.megacrit.cardcrawl.cards.blue.Defragment;
import com.megacrit.cardcrawl.cards.red.Rampage;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;

import PhysicalDefect.modcore.PhysicalDefect;
import java.util.HashSet;
import java.util.Arrays;

public class UIPatch {

    public static final HashSet<String> UI_REBUILD_LIST = new HashSet<>(Arrays.asList(
            "Gash", // 爪击
            "Rampage", // 暴走
            "Defragment", // 碎片整理
            "Biased Cognition" // 偏差认知
    // 未来添加新卡，直接写在这里即可
    ));

    // =================================================================
    // 1. 核心重构
    // =================================================================
    public static void rebuildDescription(AbstractCard card) {
        if (!UI_REBUILD_LIST.contains(card.cardID)) {
            return;
        }
        String baseDesc = CardCrawlGame.languagePack.getCardStrings(card.cardID).DESCRIPTION;
        String upgradeDesc = CardCrawlGame.languagePack.getCardStrings(card.cardID).UPGRADE_DESCRIPTION;
        if (!PhysicalDefect.enableFragmentation) {
            card.rawDescription = (card.upgraded && upgradeDesc != null) ? upgradeDesc : baseDesc;
            card.initializeDescription();
            return;
        }
        switch (card.cardID) {
            case "Gash":
            case "Rampage":
                buildAlmightyText(card, baseDesc);
                break;
            case "Defragment":
                buildDefragmentText(card, baseDesc);
                break;
            case "Biased Cognition":
                buildBiasedCognitionText(card, baseDesc);
                break;
        }
        card.initializeDescription();
    }

    // =================================================================
    // 2. 文本处理
    // =================================================================

    private static void buildAlmightyText(AbstractCard card, String baseDesc) {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(PhysicalDefect.makeID("AlmightyBonus"));
        if (uiStrings != null && uiStrings.TEXT != null) {
            card.rawDescription = baseDesc + uiStrings.TEXT[0];
        }
    }

    private static void buildDefragmentText(AbstractCard card, String baseDesc) {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(PhysicalDefect.makeID("DefragmentPatch"));
        if (uiStrings != null && uiStrings.TEXT != null && uiStrings.TEXT.length >= 3) {
            if (card.upgraded) {
                card.rawDescription = baseDesc + uiStrings.TEXT[0] + uiStrings.TEXT[1] + uiStrings.TEXT[2];
            } else {
                card.rawDescription = baseDesc + uiStrings.TEXT[0] + uiStrings.TEXT[2];
            }
        }
    }

    private static void buildBiasedCognitionText(AbstractCard card, String baseDesc) {
        UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(PhysicalDefect.makeID("BiasedCognitionPatch"));
        if (uiStrings != null && uiStrings.TEXT != null && uiStrings.TEXT.length >= 2) {
            // 拼接文本
            if (card.upgraded) {
                card.rawDescription = baseDesc + uiStrings.TEXT[0] + uiStrings.TEXT[1];
            } else {
                card.rawDescription = baseDesc + uiStrings.TEXT[0];
            }
        }
    }

    // =================================================================
    // 3. 底层卡牌复制
    // =================================================================
    @SpirePatch(clz = AbstractCard.class, method = "makeStatEquivalentCopy")
    public static class FixDescriptionOnCopy {
        @SpirePostfixPatch
        public static AbstractCard Postfix(AbstractCard __result) {
            rebuildDescription(__result);
            return __result;
        }
    }

    // =================================================================
    // 4. 卡牌的构造与升级
    // =================================================================

    // --- 爪击 (Gash) ---
    @SpirePatches({
            @SpirePatch(clz = Claw.class, method = SpirePatch.CONSTRUCTOR),
            @SpirePatch(clz = Rampage.class, method = SpirePatch.CONSTRUCTOR),
            @SpirePatch(clz = Defragment.class, method = SpirePatch.CONSTRUCTOR),
            @SpirePatch(clz = BiasedCognition.class, method = SpirePatch.CONSTRUCTOR)
    })
    public static class AllConstructorsPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            rebuildDescription(__instance);
        }
    }

    // =================================================================
    // 5. 升级方法
    // =================================================================
    @SpirePatches({
            @SpirePatch(clz = Claw.class, method = "upgrade"),
            @SpirePatch(clz = Rampage.class, method = "upgrade"),
            @SpirePatch(clz = Defragment.class, method = "upgrade"),
            @SpirePatch(clz = BiasedCognition.class, method = "upgrade")
    })
    public static class AllUpgradesPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            rebuildDescription(__instance);
        }
    }
}
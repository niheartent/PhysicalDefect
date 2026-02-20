package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.blue.Claw;
import com.megacrit.cardcrawl.cards.red.Rampage;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;

import PhysicalDefect.modcore.PhysicalDefect;

public class AlmightyPatch {

    // =================================================================
    // 7. 连击逻辑修复 (黑名单保持不变)
    // =================================================================
    public static final java.util.HashSet<String> ALMIGHTY_BLACKLIST = new java.util.HashSet<>(java.util.Arrays.asList(
            "Gash", // 爪击
            "Rampage" // 暴走
    ));

    public static boolean isAlmightyBlacklisted(String cardID) {
        return ALMIGHTY_BLACKLIST.contains(cardID);
    }

    // =================================================================
    // 🌟 核心：统一的描述重建方法
    // =================================================================
    public static void rebuildDescription(AbstractCard card) {
        if (PhysicalDefect.shouldAddDescription()) {
            // 动态获取 UI 文本，防止空指针异常
            UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(PhysicalDefect.makeID("AlmightyBonus"));

            if (uiStrings != null && uiStrings.TEXT != null) {

                String baseDesc = CardCrawlGame.languagePack.getCardStrings(card.cardID).DESCRIPTION;
                card.rawDescription = baseDesc + uiStrings.TEXT[0];
                card.initializeDescription();
            }
        } else {

            card.rawDescription = CardCrawlGame.languagePack.getCardStrings(card.cardID).DESCRIPTION;
            card.initializeDescription();
        }
    }

    // =================================================================
    // 1. 拦截爪击 (Claw / Gash)
    // =================================================================
    @SpirePatch(clz = Claw.class, method = SpirePatch.CONSTRUCTOR)
    public static class ClawConstructorPatch {
        @SpirePostfixPatch
        public static void Postfix(Claw __instance) {
            rebuildDescription(__instance);
        }
    }

    @SpirePatch(clz = Claw.class, method = "upgrade")
    public static class ClawUpgradePatch {
        @SpirePostfixPatch
        public static void Postfix(Claw __instance) {
            rebuildDescription(__instance);
        }
    }

    // =================================================================
    // 2. 拦截暴走 (Rampage)
    // =================================================================
    @SpirePatch(clz = Rampage.class, method = SpirePatch.CONSTRUCTOR)
    public static class RampageConstructorPatch {
        @SpirePostfixPatch
        public static void Postfix(Rampage __instance) {
            rebuildDescription(__instance);
        }
    }

    @SpirePatch(clz = Rampage.class, method = "upgrade")
    public static class RampageUpgradePatch {
        @SpirePostfixPatch
        public static void Postfix(Rampage __instance) {
            rebuildDescription(__instance);
        }
    }

    @SpirePatch(clz = AbstractCard.class, method = "makeStatEquivalentCopy")
    public static class FixDescriptionOnCopy {
        @SpirePostfixPatch
        public static AbstractCard Postfix(AbstractCard __result) {
            if (__result instanceof com.megacrit.cardcrawl.cards.blue.Claw) {
                AlmightyPatch.rebuildDescription(__result);
            }
            // 处理暴走
            else if (__result instanceof com.megacrit.cardcrawl.cards.red.Rampage) {
                AlmightyPatch.rebuildDescription(__result);
            }

            return __result;
        }
    }

}
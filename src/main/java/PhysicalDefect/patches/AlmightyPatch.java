package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.blue.Claw;
import com.megacrit.cardcrawl.cards.red.Rampage;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import PhysicalDefect.characters.MyPhysicalDefect;
import PhysicalDefect.modcore.PhysicalDefect;

public class AlmightyPatch {

    // =================================================================
    // 7. 连击逻辑修复
    // =================================================================
    public static final java.util.HashSet<String> ALMIGHTY_BLACKLIST = new java.util.HashSet<>(java.util.Arrays.asList(
            "Gash", // 爪击
            "Rampage" // 暴走 (战士的卡，如果你允许跨职业抓牌的话)
    // 未来如果设计了其他永久成长的卡，直接把 ID 加到这里就行
    ));

    public static boolean isAlmightyBlacklisted(String cardID) {
        return ALMIGHTY_BLACKLIST.contains(cardID);
    }

    // 获取本地化 UI 文本
    private static final UIStrings UIStrings = CardCrawlGame.languagePack
            .getUIString(PhysicalDefect.makeID("AlmightyBonus"));

    // =================================================================
    // 1. 拦截爪击 (Claw / Gash)
    // =================================================================
    @SpirePatch(clz = Claw.class, method = SpirePatch.CONSTRUCTOR)
    public static class ClawConstructorPatch {
        @SpirePostfixPatch
        public static void Postfix(Claw __instance) {
            if (PhysicalDefect.shouldAddDescription()) {
                // 直接追加描述并刷新
                __instance.rawDescription += UIStrings.TEXT[0];
                __instance.initializeDescription();
            }
        }
    }

    @SpirePatch(clz = Claw.class, method = "upgrade")
    public static class ClawUpgradePatch {
        @SpirePostfixPatch
        public static void Postfix(Claw __instance) {
            // 升级后如果文本消失了（原版upgrade可能会重置rawDescription），重新补上
            if (PhysicalDefect.shouldAddDescription() && !__instance.rawDescription.contains(UIStrings.TEXT[0])) {
                __instance.rawDescription += UIStrings.TEXT[0];
                __instance.initializeDescription();
            }
        }
    }

    // =================================================================
    // 2. 拦截狂暴 (Rampage)
    // =================================================================
    @SpirePatch(clz = Rampage.class, method = SpirePatch.CONSTRUCTOR)
    public static class RampageConstructorPatch {
        @SpirePostfixPatch
        public static void Postfix(Rampage __instance) {
            if (PhysicalDefect.shouldAddDescription()) {
                __instance.rawDescription += UIStrings.TEXT[0];
                __instance.initializeDescription();
            }
        }
    }

    @SpirePatch(clz = Rampage.class, method = "upgrade")
    public static class RampageUpgradePatch {
        @SpirePostfixPatch
        public static void Postfix(Rampage __instance) {
            if (PhysicalDefect.shouldAddDescription() && !__instance.rawDescription.contains(UIStrings.TEXT[0])) {
                __instance.rawDescription += UIStrings.TEXT[0];
                __instance.initializeDescription();
            }
        }
    }
}

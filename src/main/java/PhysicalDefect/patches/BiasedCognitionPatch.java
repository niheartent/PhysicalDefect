package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.BiasPower;

import PhysicalDefect.modcore.PhysicalDefect;

public class BiasedCognitionPatch {
    @SpirePatch(clz = com.megacrit.cardcrawl.powers.BiasPower.class, method = SpirePatch.CONSTRUCTOR)
    public static class BiasPowerTypePatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.powers.BiasPower __instance) {
            __instance.type = AbstractPower.PowerType.BUFF;
        }
    }

    @SpirePatch(clz = BiasPower.class, method = "updateDescription")
    public static class BiasPowerDescriptionPatch {

        @SpirePostfixPatch
        public static void Postfix(BiasPower __instance) {
            // 检查全局机制是否开启
            if (PhysicalDefect.enableFragmentation) {

                UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(PhysicalDefect.makeID("BiasPowerPatch"));
                if (uiStrings != null && uiStrings.TEXT != null && uiStrings.TEXT.length >= 2) {
                    __instance.description += uiStrings.TEXT[0] + __instance.amount + uiStrings.TEXT[1];
                }
            }
        }
    }
}
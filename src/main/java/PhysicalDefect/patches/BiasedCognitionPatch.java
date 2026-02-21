package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.BiasPower;

import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.powers.FragmentationPower;

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

    @SpirePatch(clz = com.megacrit.cardcrawl.cards.blue.BiasedCognition.class, method = "use")
    public static class BiasedCognitionUsePatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.cards.blue.BiasedCognition __instance,
                com.megacrit.cardcrawl.characters.AbstractPlayer p,
                com.megacrit.cardcrawl.monsters.AbstractMonster m) {

            // 只有当开启机制且卡牌已升级时触发
            if (PhysicalDefect.enableFragmentation && __instance.upgraded) {
                AbstractDungeon.actionManager.addToBottom(
                        new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(p, p,
                                new FragmentationPower(p, 1), 1));
            }
        }
    }
}
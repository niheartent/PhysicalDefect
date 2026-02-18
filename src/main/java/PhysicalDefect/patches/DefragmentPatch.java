package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.blue.Defragment;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;

import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.powers.FragmentationPower;

public class DefragmentPatch {
    private static final UIStrings UIStrings = CardCrawlGame.languagePack
            .getUIString(PhysicalDefect.makeID("DefragmentPatch"));

    // =================================================================
    // 1. 基础描述 (Constructor)
    // =================================================================
    @SpirePatch(clz = Defragment.class, method = SpirePatch.CONSTRUCTOR)
    public static class AppendBaseDescription {

        @SpirePostfixPatch
        public static void Postfix(Defragment __instance) {
            // 默认追加基础描述
            __instance.rawDescription += UIStrings.TEXT[0];

            // 如果生成出来直接就是升级版（比如通过卡牌生成器），需要补上升级描述
            if (__instance.upgraded) {
                __instance.rawDescription += UIStrings.TEXT[1];
            }
            __instance.initializeDescription();
        }
    }

    // =================================================================
    // 2. 升级描述 (Upgrade Logic)
    // =================================================================
    // 监听 upgrade 方法，当玩家在火堆升级或通过事件升级时，实时更新文本
    @SpirePatch(clz = Defragment.class, method = "upgrade")
    public static class UpdateDescriptionOnUpgrade {
        @SpirePostfixPatch
        public static void Postfix(Defragment __instance) {
            // 防止重复添加（虽然 upgrade 通常只调一次，但为了安全）
            if (!__instance.rawDescription.contains(UIStrings.TEXT[1])) {
                __instance.rawDescription += UIStrings.TEXT[1];
                __instance.initializeDescription();
            }
        }
    }

    // =================================================================
    // 3. 实现卡牌效果 (Effect Logic)
    // =================================================================
    @SpirePatch(clz = Defragment.class, method = "use")
    public static class ExtraEffect {
        @SpirePostfixPatch
        public static void Postfix(Defragment __instance, AbstractPlayer p, AbstractMonster m) {

            // 1. 检查是否有【碎片化】BUFF
            if (p.hasPower(FragmentationPower.POWER_ID)) {

                AbstractPower dexPower = p.getPower("Dexterity");
                int currentDex = (dexPower != null) ? dexPower.amount : 0;

                // 2. 逻辑分流
                if (currentDex > 0) {
                    // 情况A：正敏捷 -> 直接翻倍 (无论是否升级)
                    AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(p, p, new DexterityPower(p, currentDex), currentDex));
                } else if (currentDex < 0) {
                    // 情况B：负敏捷
                    if (__instance.upgraded) {
                        // 只有【升级后】才执行归零逻辑
                        AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(p, p, new DexterityPower(p, Math.abs(currentDex)),
                                        Math.abs(currentDex)));
                    } else {
                        // 【未升级】：负敏捷不处理（或者你可以选择翻倍负面效果，但通常不建议）
                        // 这里我们选择 "什么都不做"，仅仅是消耗了碎片化但没有敏捷收益
                        // 这让升级变得更有意义
                    }
                }

                // 3. 消耗所有【碎片化】
                AbstractDungeon.actionManager.addToBottom(
                        new RemoveSpecificPowerAction(p, p, FragmentationPower.POWER_ID));
            }
        }
    }
}
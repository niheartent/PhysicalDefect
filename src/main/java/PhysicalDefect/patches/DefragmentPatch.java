package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
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

    // =================================================================
    // 🌟 核心：统一的描述重建方法，防止无限追加或被覆盖
    // =================================================================
    public static void rebuildDescription(Defragment card) {
        if (PhysicalDefect.shouldAddDescription()) {
            UIStrings uiStrings = CardCrawlGame.languagePack.getUIString(PhysicalDefect.makeID("DefragmentPatch"));
            // 增加 length >= 3 的安全检查，防止读取越界
            if (uiStrings != null && uiStrings.TEXT != null && uiStrings.TEXT.length >= 3) {
                // 1. 获取原版最纯净的基础描述
                String baseDesc = CardCrawlGame.languagePack.getCardStrings(card.cardID).DESCRIPTION;

                // 2. 根据是否升级，拼接不同的三段式文本
                if (card.upgraded) {
                    // 升级版：原版 + [消耗碎片化] + [获得1敏捷] + [敏捷翻倍]
                    card.rawDescription = baseDesc + uiStrings.TEXT[0] + uiStrings.TEXT[1] + uiStrings.TEXT[2];
                } else {
                    // 基础版：原版 + [消耗碎片化] + [敏捷翻倍] (跳过 TEXT[1])
                    card.rawDescription = baseDesc + uiStrings.TEXT[0] + uiStrings.TEXT[2];
                }

                card.initializeDescription();
            }
        } else {
            // 如果玩家在设置里关掉了机制，恢复原版描述
            card.rawDescription = CardCrawlGame.languagePack.getCardStrings(card.cardID).DESCRIPTION;
            card.initializeDescription();
        }
    }

    // =================================================================
    // 1. 基础描述 (Constructor)
    // =================================================================
    @SpirePatch(clz = Defragment.class, method = SpirePatch.CONSTRUCTOR)
    public static class AppendBaseDescription {
        @SpirePostfixPatch
        public static void Postfix(Defragment __instance) {
            rebuildDescription(__instance);
        }
    }

    // =================================================================
    // 2. 升级描述 (Upgrade Logic)
    // =================================================================
    @SpirePatch(clz = Defragment.class, method = "upgrade")
    public static class UpdateDescriptionOnUpgrade {
        @SpirePostfixPatch
        public static void Postfix(Defragment __instance) {
            rebuildDescription(__instance);
        }
    }

    // =================================================================
    // 3. 拦截底层卡牌复制
    // =================================================================
    @SpirePatch(clz = AbstractCard.class, method = "makeStatEquivalentCopy")
    public static class FixDescriptionOnCopy {
        @SpirePostfixPatch
        public static AbstractCard Postfix(AbstractCard __result) {
            // 只要复制出来的卡是碎片整理，就强制重新刷一遍描述
            if (__result instanceof Defragment) {
                rebuildDescription((Defragment) __result);
            }
            return __result;
        }
    }

    // =================================================================
    // 4. 实现卡牌效果 (Effect Logic) - 保持你的原逻辑不变
    // =================================================================
    @SpirePatch(clz = Defragment.class, method = "use")
    public static class ExtraEffect {
        @SpirePostfixPatch
        public static void Postfix(Defragment __instance, AbstractPlayer p, AbstractMonster m) {

            // 1. 获取【碎片化】BUFF
            AbstractPower fragPower = p.getPower(FragmentationPower.POWER_ID);

            if (fragPower != null) {
                // 提前记录要消耗的碎片化层数
                int fragStacks = fragPower.amount;

                AbstractPower dexPower = p.getPower("Dexterity");
                int currentDex = (dexPower != null) ? dexPower.amount : 0;

                // 2. 逻辑分流
                if (__instance.upgraded) {

                    if (fragStacks > 0) {
                        AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(p, p, new DexterityPower(p, fragStacks), fragStacks));
                    }

                    int newDex = currentDex + fragStacks;

                    if (newDex > 0) {
                        AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(p, p, new DexterityPower(p, newDex), newDex));
                    }
                } else {
                    if (currentDex > 0) {
                        AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(p, p, new DexterityPower(p, currentDex), currentDex));
                    }
                }

                // 3. 彻底消耗所有【碎片化】
                AbstractDungeon.actionManager.addToBottom(
                        new RemoveSpecificPowerAction(p, p, FragmentationPower.POWER_ID));
            }
        }
    }
}
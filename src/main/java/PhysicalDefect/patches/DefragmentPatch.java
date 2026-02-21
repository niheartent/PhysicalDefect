package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.blue.Defragment;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.DexterityPower;

import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.powers.FragmentationPower;

public class DefragmentPatch {
    // =================================================================
    // 1. 实现卡牌效果 (Effect Logic)
    // =================================================================
    @SpirePatch(clz = Defragment.class, method = "use")
    public static class ExtraEffect {
        @SpirePostfixPatch
        public static void Postfix(Defragment __instance, AbstractPlayer p, AbstractMonster m) {

            // 【极简开关】：如果机制未开启，直接放行原版逻辑，彻底掐断后续判定
            if (!PhysicalDefect.enableFragmentation)
                return;

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
package PhysicalDefect.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import PhysicalDefect.actions.FragmentationAction;
import PhysicalDefect.powers.FragmentationPower;
import PhysicalDefect.modcore.PhysicalDefect;
import basemod.ReflectionHacks;

public class FragmentationPatch {

    // =================================================================
    // 0. 数据存储
    // =================================================================
    @SpirePatch(clz = AbstractCard.class, method = SpirePatch.CLASS)
    public static class CardFields {
        public static SpireField<Boolean> isFragmentedInstance = new SpireField<>(() -> false);
        public static SpireField<Integer> snapshotDamage = new SpireField<>(() -> 0);
    }

    @SpirePatch(clz = AbstractPlayer.class, method = SpirePatch.CLASS)
    public static class PlayerFields {
        public static SpireField<Integer> focusLossAccumulator = new SpireField<>(() -> 0);
    }

    @SpirePatch(clz = ApplyPowerAction.class, method = SpirePatch.CLASS)
    public static class ActionFields {
        public static SpireField<Boolean> hasTriggeredFocusLoss = new SpireField<>(() -> false);
    }

    // =================================================================
    // 1. 核心判定与公式
    // =================================================================
    private static boolean shouldFragment(AbstractPlayer p, AbstractCard card) {
        // 【极简开关】
        if (!PhysicalDefect.enableFragmentation)
            return false;

        if (p == null || !p.hasPower(FragmentationPower.POWER_ID))
            return false;
        if (card.type != AbstractCard.CardType.ATTACK)
            return false;

        boolean isAoe = ReflectionHacks.getPrivate(card, AbstractCard.class, "isMultiDamage");
        if (isAoe || card.damageTypeForTurn != DamageInfo.DamageType.NORMAL) {
            return false;
        }

        boolean isDerivative = CardFields.isFragmentedInstance.get(card);
        boolean isInHand = p.hand.contains(card);
        boolean isInLimbo = p.limbo.contains(card);

        if (!isDerivative && !isInHand && !isInLimbo) {
            return false;
        }

        return true;
    }

    private static int calculateSplitDamage(int baseDamage, int fragLevel, int strengthAmt) {
        int N = 1 + fragLevel;
        int rawBase = baseDamage - strengthAmt;
        int perHit = Math.max(1, rawBase / N);
        return perHit + strengthAmt;
    }

    // =================================================================
    // 2. 集中值监听
    // =================================================================
    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class FocusLossListener {
        @SpirePrefixPatch
        public static void Prefix(ApplyPowerAction __instance) {
            if (!PhysicalDefect.enableFragmentation)
                return;
            if (ActionFields.hasTriggeredFocusLoss.get(__instance))
                return;

            ActionFields.hasTriggeredFocusLoss.set(__instance, true);
            AbstractPower powerToApply = ReflectionHacks.getPrivate(__instance, ApplyPowerAction.class, "powerToApply");

            if (powerToApply != null && "Focus".equals(powerToApply.ID) && powerToApply.amount < 0) {
                if (__instance.target instanceof AbstractPlayer) {
                    AbstractPlayer p = (AbstractPlayer) __instance.target;
                    int lossAmount = Math.abs(powerToApply.amount);
                    int currentAcc = PlayerFields.focusLossAccumulator.get(p);
                    currentAcc += lossAmount;

                    int stacksToGain = currentAcc / 2;
                    int remainder = currentAcc % 2;
                    PlayerFields.focusLossAccumulator.set(p, remainder);

                    if (stacksToGain > 0) {
                        AbstractDungeon.actionManager.addToTop(
                                new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(p, p,
                                        new FragmentationPower(p, stacksToGain), stacksToGain));
                    }
                }
            }
        }
    }

    // =================================================================
    // 3. 伤害数值修改
    // =================================================================
    private static void applySplitLogic(AbstractCard card) {
        AbstractPlayer p = AbstractDungeon.player;
        if (!shouldFragment(p, card))
            return;

        int fragLvl = p.getPower(FragmentationPower.POWER_ID).amount;
        if (fragLvl <= 0)
            return;

        int strength = p.hasPower("Strength") ? p.getPower("Strength").amount : 0;
        int newDamage = calculateSplitDamage(card.damage, fragLvl, strength);

        if (card.damage != newDamage) {
            card.damage = newDamage;
            card.isDamageModified = true;
        }
    }

    @SpirePatch(clz = AbstractCard.class, method = "calculateCardDamage")
    public static class DamageMath {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance, AbstractMonster mo) {
            applySplitLogic(__instance);
        }
    }

    @SpirePatch(clz = AbstractCard.class, method = "applyPowers")
    public static class DamageDisplayMath {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            applySplitLogic(__instance);
            CardFields.snapshotDamage.set(__instance, __instance.damage);
        }
    }

    // =================================================================
    // 4. 触发攻击逻辑
    // =================================================================
    @SpirePatch(clz = UseCardAction.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { AbstractCard.class,
            AbstractCreature.class })
    public static class TriggerFragmentAttack {
        @SpirePrefixPatch
        public static void Prefix(UseCardAction __instance, AbstractCard card, AbstractCreature target) {
            if (CardFields.isFragmentedInstance.get(card))
                return;
            AbstractPlayer p = AbstractDungeon.player;
            if (!shouldFragment(p, card))
                return;

            int fragLvl = p.getPower(FragmentationPower.POWER_ID).amount;
            if (fragLvl > 0) {
                AbstractDungeon.actionManager
                        .addToBottom(new FragmentationAction(card, fragLvl, CardFields.snapshotDamage.get(card)));
            }
        }
    }

    // =================================================================
    // 5. UI 渲染 (显示 xN)
    // =================================================================
    @SpirePatch(clz = AbstractCard.class, method = "render", paramtypez = { SpriteBatch.class })
    public static class RenderHitCount {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance, SpriteBatch sb) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || !p.hasPower(FragmentationPower.POWER_ID) || !p.hand.contains(__instance))
                return;

            if (shouldFragment(p, __instance) && !CardFields.isFragmentedInstance.get(__instance)) {
                int fragLvl = p.getPower(FragmentationPower.POWER_ID).amount;
                int totalHits = 1 + fragLvl;

                if (totalHits > 1) {
                    FontHelper.renderRotatedText(sb, FontHelper.cardEnergyFont_L, "x" + totalHits,
                            __instance.current_x, __instance.current_y,
                            130.0F * __instance.drawScale * Settings.scale,
                            180.0F * __instance.drawScale * Settings.scale,
                            __instance.angle, true, Settings.GOLD_COLOR);
                }
            }
        }
    }

    // =================================================================
    // 6. 人工制品穿透与增益判定
    // =================================================================
    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class ArtifactBypass {
        @SpirePrefixPatch
        public static void Prefix(ApplyPowerAction __instance) {
            // 【极简开关】
            if (!PhysicalDefect.enableFragmentation)
                return;

            AbstractPower powerToApply = ReflectionHacks.getPrivate(__instance, ApplyPowerAction.class, "powerToApply");
            // 【修复】去掉了 MyPhysicalDefect 的强制绑定，只要是玩家都生效
            if (__instance.target instanceof AbstractPlayer &&
                    powerToApply != null &&
                    "Focus".equals(powerToApply.ID) &&
                    powerToApply.amount < 0) {
                powerToApply.type = AbstractPower.PowerType.BUFF;
            }
        }
    }
}
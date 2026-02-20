package PhysicalDefect.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.defect.GashAction;
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
import PhysicalDefect.characters.MyPhysicalDefect;
import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.patches.FragmentationPatch.CardFields;
import basemod.ReflectionHacks;
import javassist.CtBehavior;

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

    // 【新增】给 ApplyPowerAction 增加一个标记，防止单次动作重复触发
    @SpirePatch(clz = ApplyPowerAction.class, method = SpirePatch.CLASS)
    public static class ActionFields {
        public static SpireField<Boolean> hasTriggeredFocusLoss = new SpireField<>(() -> false);
    }

    // =================================================================
    // 1. 核心判定与公式
    // =================================================================
    private static boolean shouldFragment(AbstractPlayer p, AbstractCard card) {

        Boolean enable = PhysicalDefect.enableNegativeFocus;
        if (!enable) {
            return false;
        }
        // 1. 基础检查：必须有玩家、有能力、是攻击牌
        if (p == null || !p.hasPower(FragmentationPower.POWER_ID))
            return false;
        if (card.type != AbstractCard.CardType.ATTACK)
            return false;

        // 2. AOE 检查：AOE 不生效
        boolean isAoe = ReflectionHacks.getPrivate(card, AbstractCard.class, "isMultiDamage");
        if (isAoe || card.damageTypeForTurn != DamageInfo.DamageType.NORMAL) {
            return false;
        }

        // 3. 【核心修正】位置与状态检查
        // 情况 A: 它是衍生出来的分身卡 (isFragmentedInstance) -> 必须拆分伤害！
        boolean isDerivative = CardFields.isFragmentedInstance.get(card);

        // 情况 B: 它是本体卡，且在手牌或正在打出 -> 需要拆分伤害
        boolean isInHand = p.hand.contains(card);
        boolean isInLimbo = p.limbo.contains(card);

        // 如果既不是分身，也不在手牌/打出状态（比如在弃牌堆），就不处理
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
    // 2. 集中值监听 (修复版)
    // =================================================================
    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class FocusLossListener {
        @SpirePrefixPatch
        public static void Prefix(ApplyPowerAction __instance) {
            Boolean enable = PhysicalDefect.enableNegativeFocus;
            if (!enable) {
                return;
            }
            // 【修复】首先检查这个 Action 实例是否已经处理过
            if (ActionFields.hasTriggeredFocusLoss.get(__instance)) {
                return;
            }

            // 标记为已处理，确保后续帧不再执行
            ActionFields.hasTriggeredFocusLoss.set(__instance, true);

            // 获取正在施加的 Power
            AbstractPower powerToApply = ReflectionHacks.getPrivate(__instance, ApplyPowerAction.class, "powerToApply");

            // 逻辑检查：是 Focus 且 数值为负
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
                        // 使用 addToTop 或者 addToBottom 都可以，建议 Top 让反馈更及时
                        AbstractDungeon.actionManager.addToTop(
                                new com.megacrit.cardcrawl.actions.common.ApplyPowerAction(p, p,
                                        new FragmentationPower(p, stacksToGain), stacksToGain));
                    }
                }
            }
        }
    }

    // =================================================================
    // 3. 伤害数值修改 (保持不变)
    // =================================================================

    private static void applySplitLogic(AbstractCard card) {
        AbstractPlayer p = AbstractDungeon.player;

        // 1. 基础检查
        if (!shouldFragment(p, card))
            return;

        // 2. 获取层数
        int fragLvl = p.getPower(FragmentationPower.POWER_ID).amount;
        if (fragLvl <= 0)
            return;

        // 3. 获取力量
        int strength = 0;
        if (p.hasPower("Strength")) {
            strength = p.getPower("Strength").amount;
        }

        // 4. 计算并应用
        // 注意：calculateCardDamage 和 applyPowers 调用时，card.damage 已经被原版逻辑计算过一次（包含力量了）
        // 我们的 calculateSplitDamage 会先扣除力量，除以N，再加回力量
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
            // 直接调用公共方法
            applySplitLogic(__instance);
        }
    }

    @SpirePatch(clz = AbstractCard.class, method = "applyPowers")
    public static class DamageDisplayMath {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            // 直接调用公共方法

            applySplitLogic(__instance);
            CardFields.snapshotDamage.set(__instance, __instance.damage);
        }
    }

    // =================================================================
    // 4. 触发攻击逻辑 (保持不变)
    // =================================================================
    @SpirePatch(clz = UseCardAction.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { AbstractCard.class,
            AbstractCreature.class })
    public static class TriggerFragmentAttack {
        @SpirePrefixPatch
        public static void Prefix(UseCardAction __instance, AbstractCard card,
                AbstractCreature target) {
            if (CardFields.isFragmentedInstance.get(card))
                return;
            AbstractPlayer p = AbstractDungeon.player;
            if (!shouldFragment(p, card))
                return;

            int fragLvl = p.getPower(FragmentationPower.POWER_ID).amount;
            if (fragLvl > 0) {

                AbstractDungeon.actionManager
                        .addToBottom(new FragmentationAction(card, fragLvl,
                                CardFields.snapshotDamage.get(card)));
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

            // 1. 基本条件检查 (Power, Attack, etc)
            // 注意：这里我们手动检查 hand，而不完全依赖 shouldFragment
            // 因为 shouldFragment 为了伤害计算允许了 Limbo，但渲染我们只想限死在 Hand
            if (p == null || !p.hasPower(FragmentationPower.POWER_ID))
                return;

            // 【关键修改】严格检查：卡牌必须在玩家的手牌(Hand)中
            if (!p.hand.contains(__instance)) {
                return;
            }

            // 再次确认是符合条件的攻击牌
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
    // 6. 人工制品穿透 (保持不变)
    // =================================================================
    @SpirePatch(clz = ApplyPowerAction.class, method = "update")
    public static class ArtifactBypass {
        @SpirePrefixPatch
        public static void Prefix(ApplyPowerAction __instance) {
            Boolean enable = PhysicalDefect.enableNegativeFocus;
            if (!enable) {
                return;
            }
            // 注意：这里不需要 check hasTriggeredFocusLoss，因为修改 powerType 本身就是幂等的（多次设置 BUFF 类型没副作用）
            // 如果你想严格一点也可以加上检查
            AbstractPower powerToApply = ReflectionHacks.getPrivate(__instance, ApplyPowerAction.class, "powerToApply");
            if (__instance.target instanceof MyPhysicalDefect &&
                    powerToApply != null &&
                    "Focus".equals(powerToApply.ID) &&
                    powerToApply.amount < 0) {
                powerToApply.type = AbstractPower.PowerType.BUFF;
            }
        }
    }

    // // =================================================================
    // // 7. 连击逻辑修复
    // // =================================================================
    // public static final java.util.HashSet<String> ALMIGHTY_BLACKLIST = new
    // java.util.HashSet<>(java.util.Arrays.asList(
    // "Gash", // 爪击
    // "Rampage" // 暴走 (战士的卡，如果你允许跨职业抓牌的话)
    // // 未来如果设计了其他永久成长的卡，直接把 ID 加到这里就行
    // ));

    // public static boolean isAlmightyBlacklisted(String cardID) {
    // return ALMIGHTY_BLACKLIST.contains(cardID);
    // }
}

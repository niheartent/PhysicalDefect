package PhysicalDefect.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

import PhysicalDefect.actions.ScatterRecursiveAction;
import PhysicalDefect.characters.MyPhysicalDefect;
import PhysicalDefect.modcore.PhysicalDefect;
import basemod.ReflectionHacks;

public class NegativeFocusPatches {

    @SpirePatch(clz = AbstractCard.class, method = SpirePatch.CLASS)
    public static class CardFields {
        // 标记是否为散射子弹
        public static SpireField<Boolean> isScatteredInstance = new SpireField<>(() -> false);
        // 【关键缓存】记录未被散射前的原始伤害，默认 -1
        public static SpireField<Integer> originalPreScatterDamage = new SpireField<>(() -> -1);
    }

    /**
     * 数学核心：仅在渲染层和首次计算时调用
     */
    public static int getScatteredDamage(AbstractPlayer p, int originalOutput, int focusAmt) {
        if (originalOutput <= 0)
            return 0;
        int mVal = Math.abs(focusAmt);

        int strengthAmt = 0;
        if (p.hasPower("Strength")) {
            strengthAmt = p.getPower("Strength").amount;
        }

        int strippedBase = originalOutput - strengthAmt;
        int basePerHit = Math.max(1, strippedBase / mVal);
        return basePerHit + strengthAmt;
    }

    // public static boolean isScatterCard(AbstractPlayer p, AbstractCard card) {
    // if (!(p instanceof MyPhysicalDefect))
    // return false;
    // if (card.type != AbstractCard.CardType.ATTACK)
    // return false;
    // // 如果是子弹卡，绝对不要再进散射逻辑，防止死循环和数值稀释
    // if (CardFields.isScatteredInstance.get(card))
    // return false;

    // boolean isAoe = ReflectionHacks.getPrivate(card, AbstractCard.class,
    // "isMultiDamage");
    // return !isAoe && card.damageTypeForTurn == DamageInfo.DamageType.NORMAL;
    // }
    public static boolean isScatterCard(AbstractPlayer p, AbstractCard card) {
        Boolean enable = PhysicalDefect.enableNegativeFocus;
        if (!enable) {
            return false;
        }
        if (!(p instanceof MyPhysicalDefect))
            return false;
        if (card.type != AbstractCard.CardType.ATTACK)
            return false;

        // 【删除】不要在这里拦截子弹卡，否则子弹卡的数值计算会变回原版全额伤害
        // if (CardFields.isScatteredInstance.get(card))
        // return false;

        boolean isAoe = ReflectionHacks.getPrivate(card, AbstractCard.class, "isMultiDamage");
        return !isAoe && card.damageTypeForTurn == DamageInfo.DamageType.NORMAL;
    }

    // =================================================================
    // 1. 逻辑计算
    // =================================================================

    @SpirePatch(clz = UseCardAction.class, method = SpirePatch.CONSTRUCTOR, paramtypez = { AbstractCard.class,
            AbstractCreature.class })
    public static class ScatteringActionLogic {
        @SpirePrefixPatch
        public static void Prefix(UseCardAction __instance, AbstractCard card, AbstractCreature target) {
            // 【新增】在这里拦截递归：如果是衍生子弹，直接放行，让它作为普通卡打出，不要再分裂了
            if (CardFields.isScatteredInstance.get(card)) {
                return;
            }

            AbstractPlayer p = AbstractDungeon.player;
            if (!isScatterCard(p, card))
                return;

            AbstractPower focusPower = p.getPower("Focus");
            if (focusPower == null || focusPower.amount >= 0)
                return;

            int mVal = Math.abs(focusPower.amount);
            if (mVal <= 1)
                return;

            // --- 乱战/代码自动出牌兼容逻辑 ---
            // 如果 originalPreScatterDamage 为 -1，说明这张牌没经过渲染层的 applyPowers（可能是代码强行打出的）
            if (CardFields.originalPreScatterDamage.get(card) == -1) {
                // 此时手动调用一次计算，强制对齐数值
                card.calculateCardDamage(target instanceof AbstractMonster ? (AbstractMonster) target : null);
            }

            // 获取渲染层已经算好的单段伤害
            int splitDamage = card.damage;
            if (splitDamage <= 0)
                return;

            if (mVal > 1) {
                // 将递归动作加入队列底部
                AbstractDungeon.actionManager.addToBottom(
                        new ScatterRecursiveAction(p, card, mVal - 1, 1));
            }

            // for (int i = 1; i < mVal; i++) {
            // AbstractCard tmp = card.makeStatEquivalentCopy();
            // CardFields.isScatteredInstance.set(tmp, true); // 标记为子弹，Patch 将不再拦截这张卡
            // tmp.purgeOnUse = true;
            // tmp.freeToPlayOnce = true;

            // AbstractMonster randomTarget =
            // AbstractDungeon.getMonsters().getRandomMonster(null, true,
            // AbstractDungeon.cardRandomRng);
            // if (randomTarget != null) {
            // // 核心点：由于 tmp 标记为 isScatteredInstance，
            // // 这里的 calculateCardDamage 只会计算原版的 易伤/虚弱，不会再除以一次 M
            // tmp.calculateCardDamage(randomTarget);
            // tmp.use(p, randomTarget);
            // }
            // }
        }
    }

    // =================================================================
    // 2. 数据计算
    // =================================================================
    @SpirePatch(clz = AbstractCard.class, method = "applyPowers")
    @SpirePatch(clz = AbstractCard.class, method = "calculateCardDamage")
    public static class CardValueDisplay {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || !isScatterCard(p, __instance))
                return;

            AbstractPower focus = p.getPower("Focus");
            if (focus != null && focus.amount < 0) {
                // 1. 记录此时此刻的“真实原始伤害”
                CardFields.originalPreScatterDamage.set(__instance, __instance.damage);

                // 2. 计算并覆盖 damage 属性
                int splitDmg = getScatteredDamage(p, __instance.damage, focus.amount);
                if (__instance.damage > 0) {
                    __instance.damage = splitDmg;
                    __instance.isDamageModified = true;
                }
            } else {
                // 如果集中变正了，重置缓存
                CardFields.originalPreScatterDamage.set(__instance, -1);
            }
        }
    }

    // =================================================================
    // 3. UI 渲染 (右上角 xM)
    // =================================================================
    @SpirePatch(clz = AbstractCard.class, method = "render", paramtypez = { SpriteBatch.class })
    public static class CardRender {
        @SpirePostfixPatch
        public static void Postfix(AbstractCard __instance, SpriteBatch sb) {
            if (AbstractDungeon.player == null)
                return;
            if (isScatterCard(AbstractDungeon.player, __instance) && __instance.damage > 0) {
                AbstractPower focus = AbstractDungeon.player.getPower("Focus");
                if (focus != null && focus.amount < 0) {
                    int mVal = Math.abs(focus.amount);
                    if (mVal > 1) {
                        FontHelper.renderRotatedText(sb, FontHelper.cardEnergyFont_L, "x" + mVal,
                                __instance.current_x, __instance.current_y,
                                130.0F * __instance.drawScale * Settings.scale,
                                180.0F * __instance.drawScale * Settings.scale,
                                __instance.angle, true, Settings.GOLD_COLOR);
                    }
                }
            }
        }
    }

    // =================================================================
    // 4. UI显示描述文本
    // =================================================================
    @SpirePatch(clz = com.megacrit.cardcrawl.powers.FocusPower.class, method = "updateDescription")
    public static class FocusDescriptionPatch {
        @SpirePostfixPatch
        public static void Postfix(com.megacrit.cardcrawl.powers.FocusPower __instance) {

            // 【新增】如果配置关闭，不显示追加文本
            Boolean enable = PhysicalDefect.enableNegativeFocus;
            if (!enable) {
                return;
            }

            if (__instance.amount < 0) {
                int mVal = Math.abs(__instance.amount);

                // 获取本地化文本
                // 对应 JSON key: "PhysicalDefect:FocusTooltip"
                UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("PhysicalDefect:FocusTooltip");
                if (uiStrings == null)
                    return; // 防止未加载崩溃

                StringBuilder sb = new StringBuilder();
                sb.append(__instance.description);

                // TEXT[1] = " NL 你的 #y普通攻击 将被拆分为 #b{0} 段散射伤害。"
                // 使用 java.text.MessageFormat 或者简单的 String.replace 替换占位符
                String desc = uiStrings.TEXT[0].replace("{0}", String.valueOf(mVal));
                sb.append(desc);

                // 动态预览部分
                // if (AbstractDungeon.player != null) {
                // int str = 0;
                // if (AbstractDungeon.player.hasPower("Strength")) {
                // str = AbstractDungeon.player.getPower("Strength").amount;
                // }
                // int base = 6 + str;
                // int split = getScatteredDamage(AbstractDungeon.player, base,
                // __instance.amount);

                // // TEXT[2] = " NL (示例: 打击 #b{1} #y-> #b{2} #yx{0} )"
                // String example = uiStrings.TEXT[2]
                // .replace("{0}", String.valueOf(mVal))
                // .replace("{1}", String.valueOf(base))
                // .replace("{2}", String.valueOf(split));
                // sb.append(example);
                // }

                __instance.description = sb.toString();
            }
        }
    }

    // =================================================================
    // 5. 负集中穿透“人工制品” (Artifact Bypass)
    // =================================================================
    @SpirePatch(clz = com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class, method = "update")
    public static class ArtifactBypassPatch {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.actions.common.ApplyPowerAction __instance) {
            // 通过反射获取该 Action 准备施加的 Power
            Boolean enable = PhysicalDefect.enableNegativeFocus;
            if (!enable) {
                return;
            }
            AbstractPower powerToApply = ReflectionHacks.getPrivate(__instance,
                    com.megacrit.cardcrawl.actions.common.ApplyPowerAction.class, "powerToApply");

            // 判定条件：
            // 1. 目标是物理机
            // 2. Power 是集中 (Focus)
            // 3. 数值是负数
            if (__instance.target instanceof MyPhysicalDefect &&
                    powerToApply != null &&
                    powerToApply.ID.equals("Focus") &&
                    powerToApply.amount < 0) {

                // 核心黑科技：在这一瞬间将 Power 类型改为 BUFF
                // 这样 ApplyPowerAction 的 update 方法在检查 Artifact 时就会跳过它
                powerToApply.type = AbstractPower.PowerType.BUFF;
            }
        }
    }
}
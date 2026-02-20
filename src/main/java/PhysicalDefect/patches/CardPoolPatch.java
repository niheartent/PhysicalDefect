package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import PhysicalDefect.cards.Almighty;
import PhysicalDefect.characters.MyPhysicalDefect; // 确保指向你的角色类
import PhysicalDefect.modcore.PhysicalDefect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// 1. 告诉游戏我们要拦截 AbstractDungeon 类的 initializeCardPools 方法
@SpirePatch(clz = AbstractDungeon.class, method = "initializeCardPools")
public class CardPoolPatch {

    // 2. 使用 PostfixPatch（在原方法运行完后再运行我们的代码）
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance) {
        // 物理机卡池过滤
        if (AbstractDungeon.player instanceof MyPhysicalDefect) {

            filterAllCard(CARD_BLACKLIST);
            filterRelicPool(AbstractDungeon.commonRelicPool, RELIC_BLACKLIST);
            filterRelicPool(AbstractDungeon.uncommonRelicPool, RELIC_BLACKLIST);
            filterRelicPool(AbstractDungeon.rareRelicPool, RELIC_BLACKLIST);
            filterRelicPool(AbstractDungeon.bossRelicPool, RELIC_BLACKLIST);
            filterRelicPool(AbstractDungeon.shopRelicPool, RELIC_BLACKLIST);
            // 碎片化相关
            Boolean enable = PhysicalDefect.enableFragmentation;
            if (!enable) {
                filterAllCard(SPECIAL_CARD_BLACKLIST);
                filterAllCard(NEW_CARD_BLACKLIST);
            }
        }

        // 原版过滤
        if (!(AbstractDungeon.player instanceof MyPhysicalDefect)) {
            filterAllCard(NEW_CARD_BLACKLIST);
        }

        if (AbstractDungeon.player instanceof MyPhysicalDefect) {
            // --- 插入以下验证代码 ---
            System.out.println("=== 物理机器人卡池验证开始 ===");
            for (AbstractCard c : AbstractDungeon.commonCardPool.group) {
                System.out.println("普通池包含: " + c.cardID);
            }
            for (AbstractCard c : AbstractDungeon.uncommonCardPool.group) {
                System.out.println("罕见池包含: " + c.cardID);
            }
            for (AbstractCard c : AbstractDungeon.rareCardPool.group) {
                System.out.println("稀有池包含: " + c.cardID);
            }
            System.out.println("=== 物理机器人卡池验证结束 ===");
        }

    }

    private static final Set<String> CARD_BLACKLIST = new HashSet<>(Arrays.asList(
            "Ball Lightning", "Cold Snap", "Doom and Gloom", "Lockon", "Thunder Strike", "Blizzard", "Compile Driver",

            "Zap", "Tempest", "Coolheaded", "Chill", "Glacier", "Rainbow", "Chaos", "Darkness", "Consume",

            "Creative AI", "Hello World", "Storm", "Static Discharge", "Electrodynamics",
            "Biased Cognition"));
    private static final Set<String> SPECIAL_CARD_BLACKLIST = new HashSet<>(Arrays.asList(
            "Defragment"));

    private static final Set<String> NEW_CARD_BLACKLIST = new HashSet<>(Arrays.asList(
            Almighty.ID));

    private static final Set<String> RELIC_BLACKLIST = new HashSet<>(Arrays.asList(

            "FrozenCore", // 冷冻核心 (替换破损核心，每回合产冰)
            "DataDisk", // 磁盘 (开局 1 集中)
            "Emotion Chip", // 情感芯片 (掉血激发球)
            "Symbiotic Virus" // 共生病毒 (开局产暗球)
    ));

    private static void filterCardPool(ArrayList<AbstractCard> cards, Set<String> cardBlacklist) {
        cards.removeIf(c -> cardBlacklist.contains(c.cardID));
    }

    private static void filterRelicPool(ArrayList<String> pool, Set<String> relicBlackist) {
        // 遗物池里存的是 String 类型的 ID，直接 removeIf 即可
        pool.removeIf(id -> relicBlackist.contains(id));
    }

    private static void filterAllCard(Set<String> cardBlacklist) {
        filterCardPool(AbstractDungeon.commonCardPool.group, cardBlacklist);
        filterCardPool(AbstractDungeon.uncommonCardPool.group, cardBlacklist);
        filterCardPool(AbstractDungeon.rareCardPool.group, cardBlacklist);

        filterCardPool(AbstractDungeon.srcCommonCardPool.group, cardBlacklist);
        filterCardPool(AbstractDungeon.srcUncommonCardPool.group, cardBlacklist);
        filterCardPool(AbstractDungeon.srcRareCardPool.group, cardBlacklist);

    }

}

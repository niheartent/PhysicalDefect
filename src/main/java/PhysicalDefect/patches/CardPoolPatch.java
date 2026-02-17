package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import PhysicalDefect.characters.MyPhysicalDefect; // 确保指向你的角色类
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
        // 3. 关键：只在玩家是你这个新角色时才生效，不干扰原版机器人
        if (AbstractDungeon.player instanceof MyPhysicalDefect) {

            // 4. 分别过滤 普通、罕见、稀有 卡池
            filterCardPool(AbstractDungeon.commonCardPool.group);
            filterCardPool(AbstractDungeon.uncommonCardPool.group);
            filterCardPool(AbstractDungeon.rareCardPool.group);

            filterCardPool(AbstractDungeon.srcCommonCardPool.group);
            filterCardPool(AbstractDungeon.srcUncommonCardPool.group);
            filterCardPool(AbstractDungeon.srcRareCardPool.group);

            filterRelicPool(AbstractDungeon.commonRelicPool);
            filterRelicPool(AbstractDungeon.uncommonRelicPool);
            filterRelicPool(AbstractDungeon.rareRelicPool);
            filterRelicPool(AbstractDungeon.bossRelicPool);
            filterRelicPool(AbstractDungeon.shopRelicPool);
        }

        if (AbstractDungeon.player instanceof MyPhysicalDefect) {
            filterCardPool(AbstractDungeon.commonCardPool.group);
            filterCardPool(AbstractDungeon.uncommonCardPool.group);
            filterCardPool(AbstractDungeon.rareCardPool.group);

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

            "Creative AI", "Hello World", "Storm", "Static Discharge", "Electrodynamics", "Defragment",
            "Biased Cognition"));

    private static final Set<String> RELIC_BLACKLIST = new HashSet<>(Arrays.asList(

            "FrozenCore", // 冷冻核心 (替换破损核心，每回合产冰)
            "DataDisk", // 磁盘 (开局 1 集中)
            "Emotion Chip", // 情感芯片 (掉血激发球)
            "Symbiotic Virus" // 共生病毒 (开局产暗球)
    ));

    private static void filterCardPool(ArrayList<AbstractCard> cards) {
        cards.removeIf(c -> CARD_BLACKLIST.contains(c.cardID));
    }

    private static void filterRelicPool(ArrayList<String> pool) {
        // 遗物池里存的是 String 类型的 ID，直接 removeIf 即可
        pool.removeIf(id -> RELIC_BLACKLIST.contains(id));
    }
}
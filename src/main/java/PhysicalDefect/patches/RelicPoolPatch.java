package PhysicalDefect.patches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import PhysicalDefect.characters.MyPhysicalDefect;

@SpirePatch(clz = AbstractDungeon.class, method = "initializeRelicList")
public class RelicPoolPatch {

    // 2. 使用 PostfixPatch（在原方法运行完后再运行我们的代码）

    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance) {
        // 物理机卡池过滤
        if (AbstractDungeon.player instanceof MyPhysicalDefect) {
            filterAllRelic(RELIC_BLACKLIST);
        }
    }

    private static final Set<String> RELIC_BLACKLIST = new HashSet<>(Arrays.asList(

            "FrozenCore", // 冷冻核心 (替换破损核心，每回合产冰)
            "DataDisk", // 磁盘 (开局 1 集中)
            "Emotion Chip", // 情感芯片 (掉血激发球)
            "Symbiotic Virus" // 共生病毒 (开局产暗球)
    ));

    private static void filterRelicPool(ArrayList<String> pool, Set<String> relicBlackist) {
        // 遗物池里存的是 String 类型的 ID，直接 removeIf 即可
        pool.removeIf(id -> relicBlackist.contains(id));
    }

    private static void filterAllRelic(Set<String> relicBlacklist) {
        filterRelicPool(AbstractDungeon.commonRelicPool, relicBlacklist);
        filterRelicPool(AbstractDungeon.uncommonRelicPool, relicBlacklist);
        filterRelicPool(AbstractDungeon.rareRelicPool, relicBlacklist);
        filterRelicPool(AbstractDungeon.bossRelicPool, relicBlacklist);
        filterRelicPool(AbstractDungeon.shopRelicPool, relicBlacklist);

    }

}

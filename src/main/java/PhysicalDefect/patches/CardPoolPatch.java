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
        boolean isPhysicalDefect = AbstractDungeon.player instanceof MyPhysicalDefect;
        boolean fragEnabled = PhysicalDefect.enableFragmentation;

        if (isPhysicalDefect) {
            filterAllCard(ORB_CARD_BLACKLIST);

            if (!fragEnabled) {
                filterAllCard(FRAGMENT_CORE_CARDS);
                filterAllCard(GENERATOR_CARDS);
            }
        } else {
            if (!fragEnabled) {
                filterAllCard(ONLY_ALMIGHTY);
            }
        }

        String charName = AbstractDungeon.player.title;
        System.out.println("============== [" + charName + "] 卡池验证开始 ==============");
        System.out.println("当前碎片化开关状态: " + fragEnabled);
        System.out.println("--- 普通池 (Common) ---");
        for (AbstractCard c : AbstractDungeon.commonCardPool.group) {
            System.out.println("  ID: " + c.cardID);
        }
        System.out.println("--- 罕见池 (Uncommon) ---");
        for (AbstractCard c : AbstractDungeon.uncommonCardPool.group) {
            System.out.println("  ID: " + c.cardID);
        }
        System.out.println("--- 稀有池 (Rare) ---");
        for (AbstractCard c : AbstractDungeon.rareCardPool.group) {
            System.out.println("  ID: " + c.cardID);
        }
        System.out.println("============== 卡池验证结束 ==============");
    }

    // 1. 物理机永远不想见到的球类卡
    private static final Set<String> ORB_CARD_BLACKLIST = new HashSet<>(Arrays.asList(
            "Ball Lightning", "Cold Snap", "Doom and Gloom", "Lockon", "Thunder Strike", "Blizzard", "Compile Driver",
            "Zap", "Tempest", "Coolheaded", "Chill", "Glacier", "Rainbow", "Chaos", "Darkness", "Consume",
            "Storm", "Static Discharge", "Electrodynamics"));

    // 2. 生成类卡牌 (如果关闭机制，物理机就不需要它们了)
    private static final Set<String> GENERATOR_CARDS = new HashSet<>(Arrays.asList(
            "Creative AI", "Hello World"));

    // 3. 碎片化机制核心
    private static final Set<String> FRAGMENT_CORE_CARDS = new HashSet<>(Arrays.asList(
            Almighty.ID, "Defragment", "Biased Cognition"));

    // 4. 仅全能
    private static final Set<String> ONLY_ALMIGHTY = new HashSet<>(Arrays.asList(
            Almighty.ID));

    private static void filterCardPool(ArrayList<AbstractCard> cards, Set<String> cardBlacklist) {
        cards.removeIf(c -> cardBlacklist.contains(c.cardID));
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

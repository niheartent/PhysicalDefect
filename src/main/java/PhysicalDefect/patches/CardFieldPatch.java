package PhysicalDefect.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;

@SpirePatch(clz = AbstractCard.class, method = SpirePatch.CLASS)
public class CardFieldPatch {
    // 用来存储卡牌修改前的原始描述文本
    public static SpireField<String> originalDescription = new SpireField<>(() -> null);

    // 用来标记这张卡是否已经被我们要的机制修改过
    public static SpireField<Boolean> isScatteredModified = new SpireField<>(() -> false);
}
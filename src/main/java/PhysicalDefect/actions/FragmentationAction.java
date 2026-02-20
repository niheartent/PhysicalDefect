package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class FragmentationAction extends AbstractGameAction {
    private AbstractCard sourceCard;
    private int hits;

    public FragmentationAction(AbstractCard sourceCard, int hits, int fixedBaseDamage) {
        this.sourceCard = sourceCard;
        this.hits = hits;

    }

    @Override
    public void update() {
        // 利用 for 循环将所有攻击动作加入队列
        // 注意：这里使用 addToTop，确保它们优先执行，且不会被怪物的受击反击动作隔开
        for (int i = this.hits - 1; i >= 0; i--) {
            AbstractDungeon.actionManager.addToTop(
                    new FragmentSingleHitAction(this.sourceCard, i));
        }
        this.isDone = true; // 派发完毕，立刻结束
    }
}

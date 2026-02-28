package PhysicalDefect.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class PlayRegisterCardAction extends AbstractGameAction {
    private AbstractCard card;
    private float startX;
    private float startY;

    public PlayRegisterCardAction(AbstractCard card, float startX, float startY) {
        this.card = card;
        this.startX = startX;
        this.startY = startY;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            // 1. 彻底清理残留的视觉状态（修复幽灵卡的核心）
            this.card.unhover();
            this.card.unfadeOut();
            this.card.lighten(true);
            this.card.setAngle(0.0F);
            this.card.isFlipped = false;
            this.card.targetDrawScale = 0.75F;

            // 2. 魔法标记：让系统认为这是一张需要正常打出但不算作从手牌打出的牌
            this.card.freeToPlayOnce = true;
            this.card.isInAutoplay = true;

            // 更新数值（此时 isInRegister 依然为 true，完美触发碎片化数值预览）
            this.card.applyPowers();

            // 【绝不能在这里手动 add 到 limbo！】
            // 交给下面的 cardQueue 处理，否则会出现幽灵卡 Bug！

            this.card.current_x = this.startX;
            this.card.current_y = this.startY;
            // 不设置 target_x 和 target_y，游戏底层会自动把它拉到屏幕中央

            AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                    AbstractDungeon.cardRandomRng);

            // 3. 将其塞入底层的 CardQueue。
            // 最后一个参数是 false！代表它不是被“消耗/蒸发”式打出，打完后能乖乖进弃牌堆！
            AbstractDungeon.actionManager.cardQueue
                    .add(new CardQueueItem(this.card, target, this.card.energyOnUse, true, false));
        }
        this.isDone = true;
    }
}
package PhysicalDefect.actions;

import PhysicalDefect.modcore.PhysicalDefect;
import PhysicalDefect.orbs.Register;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

import java.util.ArrayList;

public class MultiStoreInRegisterAction extends AbstractGameAction {
    private ArrayList<Register> emptyOrbs;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack
            .getUIString(PhysicalDefect.makeID("MultiStoreAction"));
    public static final String[] TEXT = uiStrings.TEXT;

    public MultiStoreInRegisterAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.CARD_MANIPULATION;
        this.emptyOrbs = new ArrayList<>();
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            // 1. 寻找场上所有【空】的寄存器球
            for (AbstractOrb orb : AbstractDungeon.player.orbs) {
                if (orb instanceof Register && ((Register) orb).storedCard == null) {
                    emptyOrbs.add((Register) orb);
                }
            }

            int emptyCount = emptyOrbs.size();

            // 如果没有空球，或者手牌为空，直接结束
            if (emptyCount == 0 || AbstractDungeon.player.hand.isEmpty()) {
                this.isDone = true;
                return;
            }

            // 2. 打开选牌界面
            AbstractDungeon.handCardSelectScreen.open(
                    TEXT[0],
                    emptyCount,
                    false,
                    true,
                    false,
                    false,
                    true);
            this.tickDuration();
            return;
        }

        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            int orbIndex = 0;

            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                if (orbIndex < emptyOrbs.size()) {
                    Register targetOrb = emptyOrbs.get(orbIndex);

                    // 【关键修改】调用刚才新写的专门方法，它会自动生成无文字替身
                    targetOrb.setStoredCard(c);

                    targetOrb.updateDescription();

                    if (targetOrb.passiveAmount > 0) {
                        AbstractDungeon.actionManager.addToBottom(
                                new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player,
                                        targetOrb.passiveAmount));
                    }

                    orbIndex++;
                }
            }

            // 清理界面状态
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
            AbstractDungeon.handCardSelectScreen.selectedCards.group.clear();
            this.isDone = true;
        }

        this.tickDuration();
    }
}
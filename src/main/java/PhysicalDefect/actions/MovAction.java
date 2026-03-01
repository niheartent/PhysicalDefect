package PhysicalDefect.actions;

import PhysicalDefect.orbs.Register;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

public class MovAction extends AbstractGameAction {
    public MovAction() {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = ActionType.WAIT;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractPlayer p = AbstractDungeon.player;

            // 1. 遍历所有球，寻找满载的寄存器
            for (AbstractOrb orb : p.orbs) {
                if (orb instanceof Register) {
                    Register reg = (Register) orb;
                    if (reg.storedCard != null) {

                        // 从球中弹出真实的卡牌，并触发球的空载扫描特效
                        AbstractCard c = reg.removeCard();

                        if (c != null) {
                            // 还原卡牌的状态，防止它还带着球里的缩小比例
                            c.unhover();
                            c.unfadeOut();
                            c.lighten(true);
                            c.setAngle(0.0F);
                            c.drawScale = 0.15F; // 让它从球里的小尺寸开始变大
                            c.targetDrawScale = 0.75F; // 飞回手牌的正常尺寸

                            // 2. 华丽飞回手牌的特效
                            if (p.hand.size() < 10) { // BaseMod.MAX_HAND_SIZE 一般为 10
                                AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(c, reg.cX, reg.cY));
                            } else {
                                // 如果玩家手牌被塞满了，为了防止吞牌，让多出来的飞进弃牌堆
                                AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(c, reg.cX, reg.cY));
                                p.createHandIsFullDialog();
                            }
                        }
                    }
                }
            }

            // 3. 将所有卡牌弹回手牌后，把存牌动作塞进队列底部
            // 这样特效在飞的同时，你的选牌界面就会顺滑地弹出来，允许玩家把刚才的牌（或其他牌）重新存进去
            AbstractDungeon.actionManager.addToBottom(new MultiStoreInRegisterAction());
        }

        this.tickDuration();
    }
}
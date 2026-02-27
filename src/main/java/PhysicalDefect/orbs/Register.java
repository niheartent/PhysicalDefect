package PhysicalDefect.orbs;

import PhysicalDefect.actions.MultiStoreInRegisterAction;
import PhysicalDefect.modcore.PhysicalDefect;
import com.megacrit.cardcrawl.vfx.cardManip.CardGlowBorder;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

public class Register extends AbstractOrb {
    public static final String ORB_ID = PhysicalDefect.makeID("Register");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    public static final String[] DESC = orbString.DESCRIPTION;

    public AbstractCard storedCard = null;
    private AbstractCard dummyCard = null;

    // --- 新增：专门用于撑大发光层范围的不可见替身卡 ---
    private AbstractCard glowCard = null;

    private static Texture emptyTex = null;
    private ArrayList<AbstractGameEffect> glowEffects = new ArrayList<>();
    private float glowTimer = 0.0F;

    // 扫描状态控制
    private boolean isScanningCard = false; // 存入卡牌时的扫描
    private boolean isSpawningOrb = false; // 获取球时的边框扫描
    private float scanTimer = 0.0F;
    private static final float SCAN_TIME = 0.6F;

    public Register() {
        this.ID = ORB_ID;
        this.name = orbString.NAME;
        this.evokeAmount = 1;
        this.passiveAmount = 0;
        this.updateDescription();
        this.channelAnimTimer = 0.5F;

        if (emptyTex == null) {
            emptyTex = ImageMaster.loadImage(PhysicalDefect.assetPath("img/orbs/register.png"));
        }

        // --- 核心改动：获取球时，触发边框的激光扫描！ ---
        this.isSpawningOrb = true;
        this.scanTimer = SCAN_TIME;
        CardCrawlGame.sound.play("ORB_PLASMA_CHANNEL", 0.1F); // 等离子生成音效
    }

    public void setStoredCard(AbstractCard c) {
        this.storedCard = c;

        this.dummyCard = c.makeStatEquivalentCopy();
        this.dummyCard.name = "";
        this.dummyCard.rawDescription = "";
        this.dummyCard.cost = -2;
        this.dummyCard.costForTurn = -2;
        this.dummyCard.initializeDescription();

        this.dummyCard.drawScale = 0.15F;
        this.dummyCard.targetDrawScale = 0.15F;
        this.storedCard.drawScale = 0.75F;
        this.storedCard.targetDrawScale = 0.75F;

        // --- 初始化放大版的发光专属替身卡 ---
        this.glowCard = c.makeStatEquivalentCopy();
        // 缩放比例设为 0.165F（比正常卡牌大一圈），这样光晕就会更显著
        this.glowCard.drawScale = 0.165F;
        this.glowCard.targetDrawScale = 0.165F;

        // --- 核心改动：存入牌时，触发卡牌的激光扫描！ ---
        this.isScanningCard = true;
        this.scanTimer = SCAN_TIME;
        CardCrawlGame.sound.play("ORB_PLASMA_CHANNEL", 0.1F); // 再次播放音效
        this.glowEffects.clear();
    }

    @Override
    public void updateDescription() {
        this.applyFocus();
        if (this.storedCard == null) {
            this.description = DESC[0];
            this.passiveAmount = 0;
        } else {
            int blockAmt = Math.max(0, this.storedCard.costForTurn);
            this.passiveAmount = blockAmt;
            this.description = DESC[1] + this.storedCard.name + DESC[2] + DESC[3] + this.passiveAmount + DESC[4];
        }
    }

    @Override
    public void onEvoke() {
        CardCrawlGame.sound.play("ORB_PLASMA_EVOKE", 0.1F);

        if (this.storedCard != null) {
            if (this.storedCard.type == AbstractCard.CardType.CURSE
                    || this.storedCard.type == AbstractCard.CardType.STATUS) {

                // 【核心修改】：判断卡牌是否带有“消耗”或“虚无”属性
                if (this.storedCard.exhaust || this.storedCard.isEthereal) {
                    // 将卡牌临时放入 limbo（结算暂存区），然后调用原版的消耗逻辑
                    // 这样能完美触发【无惧疼痛】、【枯木树枝】、【卡戎之灰】等联动，并且不触发其负面效果
                    AbstractDungeon.player.limbo.addToTop(this.storedCard);
                    AbstractDungeon.player.limbo.moveToExhaustPile(this.storedCard);
                } else {
                    // 没有消耗/虚无属性的普通状态牌或诅咒，进入弃牌堆
                    AbstractDungeon.player.discardPile.addToTop(this.storedCard);
                }

            } else {
                this.storedCard.freeToPlayOnce = true;
                AbstractDungeon.actionManager.addToTop(new NewQueueCardAction(this.storedCard, true, false, true));
            }

            this.storedCard = null;
            this.dummyCard = null;
            this.glowCard = null; // 激发时清理发光卡牌
        } else {
            AbstractDungeon.actionManager.addToTop(new DrawCardAction(AbstractDungeon.player, 1));
        }
        this.glowEffects.clear();
    }

    @Override
    public void onEndOfTurn() {
        // (被动逻辑保持不变)
        if (this.storedCard != null) {
            if (this.passiveAmount > 0) {
                AbstractDungeon.actionManager.addToBottom(
                        new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player, this.passiveAmount));
            }
        } else {
            boolean isActionAlreadyQueued = false;
            for (AbstractGameAction a : AbstractDungeon.actionManager.actions) {
                if (a instanceof MultiStoreInRegisterAction) {
                    isActionAlreadyQueued = true;
                    break;
                }
            }
            if (!isActionAlreadyQueued && !AbstractDungeon.player.hand.isEmpty()) {
                AbstractDungeon.actionManager.addToBottom(new MultiStoreInRegisterAction());
            }
        }
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();

        // --- 1. 原本的扫描动画与切削粒子喷射 ---
        if (this.isScanningCard || this.isSpawningOrb) {
            this.scanTimer -= Gdx.graphics.getDeltaTime();

            if (this.scanTimer > 0 && MathUtils.randomBoolean(0.6F)) {
                float progress = 1.0F - (this.scanTimer / SCAN_TIME);
                float fullWidth = 300.0F * 0.15F * Settings.scale;
                float halfWidth = fullWidth / 2.0F;
                float laserX = this.cX - halfWidth + (fullWidth * progress);
                float drawY = this.cY + this.bobEffect.y;

                AbstractDungeon.effectsQueue
                        .add(new ScanSparkleEffect(laserX, drawY + MathUtils.random(-30.0F, 30.0F) * Settings.scale));
            }

            if (this.scanTimer <= 0.0F) {
                this.isScanningCard = false;
                this.isSpawningOrb = false;
                this.scanTimer = 0.0F;
            }
        }

        // --- 2. 必须先更新替身卡牌坐标，防止光晕残影滞后 ---
        if (this.dummyCard != null) {
            this.dummyCard.current_x = this.cX;
            this.dummyCard.current_y = this.cY + this.bobEffect.y;
        }
        // 同步更新放大版发光卡牌的坐标
        if (this.glowCard != null) {
            this.glowCard.current_x = this.cX;
            this.glowCard.current_y = this.cY + this.bobEffect.y;
        }

        // --- 3. 新增：持续生成的待机环绕粒子与手牌同款层叠发光 ---
        if (!this.isScanningCard && !this.isSpawningOrb) {
            // (1) 环绕粒子
            if (MathUtils.randomBoolean(0.1F)) {
                float spawnX = this.cX + MathUtils.random(-30.0F, 30.0F) * Settings.scale;
                float spawnY = this.cY + this.bobEffect.y + MathUtils.random(-40.0F, 40.0F) * Settings.scale;
                AbstractDungeon.effectsQueue.add(new RegisterIdleParticleEffect(spawnX, spawnY));
            }

            // (2) 调用原版的手牌同款发光，改用大一圈的 glowCard 生成
            if (this.glowCard != null) {
                this.glowTimer -= Gdx.graphics.getDeltaTime();
                if (this.glowTimer <= 0.0F) {
                    this.glowTimer = 0.25F; // 生成频率加快，光晕显得更厚实
                    // 改为极其鲜艳耀眼的亮青色
                    this.glowCard.glowColor = new Color(0.1F, 1.0F, 1.0F, 1.0F);
                    this.glowEffects.add(new CardGlowBorder(this.glowCard));
                }

                // 独立更新内部的光晕队列
                for (int i = this.glowEffects.size() - 1; i >= 0; i--) {
                    AbstractGameEffect e = this.glowEffects.get(i);
                    e.update();
                    if (e.isDone) {
                        this.glowEffects.remove(i);
                    }
                }
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        float drawY = this.cY + this.bobEffect.y;
        float scaleMultiplier = 0.15F * Settings.scale;
        float halfWidth = 150.0F * scaleMultiplier;
        float halfHeight = 210.0F * scaleMultiplier;
        float fullWidth = 300.0F * scaleMultiplier;
        float fullHeight = 420.0F * scaleMultiplier;

        float progress = 1.0F;
        if (this.isScanningCard || this.isSpawningOrb) {
            progress = 1.0F - (this.scanTimer / SCAN_TIME);
        }

        // 预计算裁剪坐标 (补回之前的缺漏！)
        int clipX = (int) (this.cX - halfWidth);
        int clipY = (int) (drawY - halfHeight);
        int clipW = (int) (fullWidth * progress);
        int clipH = (int) fullHeight;

        sb.setColor(Color.WHITE.cpy());

        // 1. 渲染空载边框
        if (emptyTex != null && this.dummyCard == null) {
            if (this.isSpawningOrb) {
                sb.flush();
                Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
                Gdx.gl.glScissor(clipX, clipY, clipW, clipH);

                drawEmptyFrame(sb, drawY, scaleMultiplier);

                sb.flush();
                Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
                renderLaser(sb, this.cX - halfWidth + clipW, drawY, halfHeight);
            } else {
                drawEmptyFrame(sb, drawY, scaleMultiplier);
            }
        }

        // 2. 渲染存入的卡牌与原版光晕
        if (this.dummyCard != null) {
            if (this.isScanningCard) {
                drawEmptyFrame(sb, drawY, scaleMultiplier);

                sb.flush();
                Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
                Gdx.gl.glScissor(clipX, clipY, clipW, clipH);

                this.dummyCard.render(sb);

                sb.flush();
                Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
                renderLaser(sb, this.cX - halfWidth + clipW, drawY, halfHeight);
            } else {
                // 渲染游戏真实的手牌发光层 (在卡牌底片之下)
                for (AbstractGameEffect e : this.glowEffects) {
                    e.render(sb);
                }
                // 注意：这里仍然渲染原本大小的 dummyCard，只有发光是用 glowCard 撑大的！
                this.dummyCard.render(sb);
            }
        }

        this.hb.render(sb);

        if (this.hb.hovered && this.storedCard != null) {
            this.storedCard.current_x = this.cX;
            this.storedCard.current_y = this.cY + 160.0F * Settings.scale;
            this.storedCard.render(sb);
        }
    }

    // 抽取出的公共方法：绘制空边框
    private void drawEmptyFrame(SpriteBatch sb, float drawY, float scaleMult) {
        sb.setColor(Color.WHITE.cpy());
        sb.draw(emptyTex,
                this.cX - 150.0F, drawY - 210.0F,
                150.0F, 210.0F, 300.0F, 420.0F,
                scaleMult, scaleMult, 0.0F,
                0, 0, 300, 420, false, false);
    }

    private void renderLaser(SpriteBatch sb, float laserX, float drawY, float halfHeight) {
        sb.setBlendFunction(770, 1);
        TextureAtlas.AtlasRegion img = ImageMaster.GLOW_SPARK_2;
        float laserHeight = (halfHeight * 2.0F) * 2.5F;
        float flicker = MathUtils.random(0.8F, 1.2F);
        float lengthScale = laserHeight / img.packedWidth;
        float widthScale0 = (70.0F * Settings.scale * flicker) / img.packedHeight;
        float widthScale1 = (35.0F * Settings.scale * flicker) / img.packedHeight;
        float widthScale2 = (14.0F * Settings.scale * flicker) / img.packedHeight;
        float widthScale3 = (5.0F * Settings.scale * flicker) / img.packedHeight;

        sb.setColor(new Color(0.0F, 0.5F, 1.0F, 0.3F * flicker));
        sb.draw(img, laserX - img.packedWidth / 2.0F, drawY - img.packedHeight / 2.0F,
                img.packedWidth / 2.0F, img.packedHeight / 2.0F,
                img.packedWidth, img.packedHeight,
                lengthScale * 0.85F, widthScale0, 90.0F);
        sb.setColor(new Color(0.0F, 1.0F, 1.0F, 0.6F * flicker));
        sb.draw(img, laserX - img.packedWidth / 2.0F, drawY - img.packedHeight / 2.0F,
                img.packedWidth / 2.0F, img.packedHeight / 2.0F,
                img.packedWidth, img.packedHeight,
                lengthScale, widthScale1, 90.0F);
        sb.setColor(new Color(0.6F, 1.0F, 1.0F, 0.9F * flicker));
        sb.draw(img, laserX - img.packedWidth / 2.0F, drawY - img.packedHeight / 2.0F,
                img.packedWidth / 2.0F, img.packedHeight / 2.0F,
                img.packedWidth, img.packedHeight,
                lengthScale, widthScale2, 90.0F);
        sb.setColor(new Color(1.0F, 1.0F, 1.0F, 1.0F * flicker));
        sb.draw(img, laserX - img.packedWidth / 2.0F, drawY - img.packedHeight / 2.0F,
                img.packedWidth / 2.0F, img.packedHeight / 2.0F,
                img.packedWidth, img.packedHeight,
                lengthScale, widthScale3, 90.0F);

        sb.setBlendFunction(770, 771);
        sb.setColor(Color.WHITE.cpy());
    }

    @Override
    public void playChannelSFX() {
        // 原版默认音效置空，因为我们在构造函数里手写了等离子音效
    }

    @Override
    public AbstractOrb makeCopy() {
        return new Register();
    }

    private static class ScanSparkleEffect extends AbstractGameEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
        private float size;

        public ScanSparkleEffect(float x, float y) {
            this.duration = MathUtils.random(0.2F, 0.5F);
            this.startingDuration = this.duration;
            this.x = x;
            this.y = y;
            this.vX = MathUtils.random(-60.0F, -20.0F) * Settings.scale;
            this.vY = MathUtils.random(-20.0F, 20.0F) * Settings.scale;
            this.color = new Color(MathUtils.random(0.5F, 1.0F), 1.0F, 1.0F, 1.0F);
            this.size = MathUtils.random(2.0F, 4.0F) * Settings.scale;
        }

        @Override
        public void update() {
            this.x += this.vX * Gdx.graphics.getDeltaTime();
            this.y += this.vY * Gdx.graphics.getDeltaTime();
            this.duration -= Gdx.graphics.getDeltaTime();
            this.color.a = Interpolation.fade.apply(0.0F, 1.0F, this.duration / this.startingDuration);
            if (this.duration < 0.0F) {
                this.isDone = true;
            }
        }

        @Override
        public void render(SpriteBatch sb) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.x, this.y, this.size, this.size);
            sb.setBlendFunction(770, 771);
        }

        @Override
        public void dispose() {
        }
    }

    private static class RegisterIdleParticleEffect extends AbstractGameEffect {
        private float x;
        private float y;
        private float vY;
        private float size;
        private float waveTimer;
        private float waveAmplitude;

        public RegisterIdleParticleEffect(float x, float y) {
            this.duration = MathUtils.random(1.0F, 2.0F);
            this.startingDuration = this.duration;
            this.x = x;
            this.y = y;

            // 向上缓慢漂浮
            this.vY = MathUtils.random(10.0F, 30.0F) * Settings.scale;

            // 随机的左右摇摆参数
            this.waveTimer = MathUtils.random(0.0F, 10.0F);
            this.waveAmplitude = MathUtils.random(5.0F, 15.0F) * Settings.scale;

            // 极度细小的淡青色光点
            this.color = new Color(0.4F, 1.0F, 1.0F, 0.0F);
            this.size = MathUtils.random(1.5F, 3.0F) * Settings.scale;
        }

        @Override
        public void update() {
            this.waveTimer += Gdx.graphics.getDeltaTime() * 3.0F;
            float currentX = this.x + MathUtils.sin(this.waveTimer) * this.waveAmplitude;
            this.y += this.vY * Gdx.graphics.getDeltaTime();
            this.duration -= Gdx.graphics.getDeltaTime();
            if (this.duration > this.startingDuration / 2.0F) {
                this.color.a = Interpolation.fade.apply(1.0F, 0.0F,
                        (this.duration - this.startingDuration / 2.0F) / (this.startingDuration / 2.0F));
            } else {
                this.color.a = Interpolation.fade.apply(0.0F, 1.0F, this.duration / (this.startingDuration / 2.0F));
            }
            this.x = currentX - (MathUtils.sin(this.waveTimer) * this.waveAmplitude);

            if (this.duration < 0.0F) {
                this.isDone = true;
            }
        }

        @Override
        public void render(SpriteBatch sb) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            float renderX = this.x + MathUtils.sin(this.waveTimer) * this.waveAmplitude;
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, renderX, this.y, this.size, this.size);
            sb.setBlendFunction(770, 771);
        }

        @Override
        public void dispose() {
        }
    }
}
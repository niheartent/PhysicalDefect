package PhysicalDefect.orbs;

import PhysicalDefect.actions.MultiStoreInRegisterAction;
import PhysicalDefect.actions.PlayRegisterCardAction;
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
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import PhysicalDefect.patches.FragmentationPatch;
import com.megacrit.cardcrawl.helpers.FontHelper;

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
            emptyTex = ImageMaster.loadImage(PhysicalDefect.assetPath("img/orbs/Register.png"));
        }

        // --- 核心改动：获取球时，触发边框的激光扫描！ ---
        this.isSpawningOrb = true;
        this.scanTimer = SCAN_TIME;
        CardCrawlGame.sound.play("ORB_PLASMA_CHANNEL", 0.1F); // 等离子生成音效
    }

    public void setStoredCard(AbstractCard c) {
        this.storedCard = c;

        // 【修复幽灵卡的核心1】：强行洗去所有悬停、透明度和动画残留
        this.storedCard.unhover();
        this.storedCard.unfadeOut();
        this.storedCard.lighten(true);
        this.storedCard.setAngle(0.0F);
        this.storedCard.isFlipped = false; // 防止背面朝上的状态残留

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

        this.glowCard = c.makeStatEquivalentCopy();
        this.glowCard.drawScale = 0.155F;
        this.glowCard.targetDrawScale = 0.155F;

        this.isScanningCard = true;
        this.scanTimer = SCAN_TIME;
        CardCrawlGame.sound.play("ORB_PLASMA_CHANNEL", 0.1F);
        this.glowEffects.clear();

        FragmentationPatch.CardFields.isInRegister.set(this.storedCard, true);
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
            // 【千万不要在这里过早剥离 isInRegister 标记！】
            // 必须让卡牌保留寄存器标记，确保它在排队被计算伤害时依然能触发碎片化！

            this.storedCard.unhover();
            this.storedCard.unfadeOut();
            this.storedCard.setAngle(0.0F);
            this.storedCard.targetDrawScale = 0.75F;

            if (this.storedCard.exhaust || this.storedCard.isEthereal) {
                // 如果是消耗/虚无牌，在这里安全剥离标记并消耗掉
                FragmentationPatch.CardFields.isInRegister.set(this.storedCard, false);
                AbstractCard c = this.storedCard;
                AbstractDungeon.actionManager.addToTop(new AbstractGameAction() {
                    @Override
                    public void update() {
                        AbstractDungeon.player.limbo.addToBottom(c);
                        AbstractDungeon.player.limbo.moveToExhaustPile(c);
                        this.isDone = true;
                    }
                });
            } else {
                // 正常打出：调用刚才我们写好的 Action 塞入原版队列
                AbstractDungeon.actionManager.addToTop(new PlayRegisterCardAction(this.storedCard, this.cX, this.cY));
            }

            this.storedCard = null;
            this.dummyCard = null;
            this.glowCard = null;
        } else {
            AbstractDungeon.actionManager.addToTop(new DrawCardAction(AbstractDungeon.player, 1));
        }
        this.glowEffects.clear();
    }

    @Override
    public void onEndOfTurn() {
        passiveEffect();
    }

    @Override
    public void onStartOfTurn() {
    }

    public void passiveEffect() {
        if (this.storedCard != null) {
            if (this.passiveAmount > 0) {
                // 获取球当前的中心坐标
                float effectX = this.cX;
                float effectY = this.cY + this.bobEffect.y;

                // 1. 触发一次科技感圆环扩散脉冲
                AbstractDungeon.effectsQueue.add(new RegisterRingPulseEffect(effectX, effectY));

                // 2. 触发 10~15 个先爆开后追踪玩家的小光点
                int particleCount = MathUtils.random(10, 15);
                for (int i = 0; i < particleCount; i++) {
                    AbstractDungeon.effectsQueue.add(new RegisterHomingParticle(effectX, effectY));
                }

                // 原有的格挡动作
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
    public void update() {
        super.update();

        if (this.storedCard != null) {
            this.storedCard.applyPowers();
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

        // 预计算裁剪坐标
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
                // --- 新增：在扫描裁剪区域内渲染全息卡费数字 ---
                renderHolographicCost(sb, drawY);

                sb.flush();
                Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
                renderLaser(sb, this.cX - halfWidth + clipW, drawY, halfHeight);
            } else {
                // 渲染游戏真实的手牌发光层 (在卡牌底片之下)
                for (AbstractGameEffect e : this.glowEffects) {
                    e.render(sb);
                }
                // 注意：这里仍然渲染原本大小的 dummyCard
                this.dummyCard.render(sb);
                // --- 新增：在正常状态下渲染全息卡费数字 ---
                renderHolographicCost(sb, drawY);
            }
        }

        this.hb.render(sb);

        if (this.hb.hovered && this.storedCard != null) {
            this.storedCard.current_x = this.cX;
            this.storedCard.current_y = this.cY + 155.0F * Settings.scale;
            this.storedCard.render(sb);
        }
    }

    // ==========================================
    // 新增：绘制科幻全息风格的卡费/格挡数字
    // ==========================================
    private void renderHolographicCost(SpriteBatch sb, float drawY) {
        String costStr = String.valueOf(this.passiveAmount);

        // 位置计算：缩略图下半部分（正好盖住原本描述文字的区域）
        float textY = drawY - 18.0F * Settings.scale;

        // --- 1. 绘制科幻风格的半透明数字底框 ---
        sb.setBlendFunction(770, 1); // 开启叠加高亮混合模式
        sb.setBlendFunction(770, 771); // 恢复正常混合模式

        // --- 2. 绘制发光数字 ---
        // 使用顶栏大数字字体 (topPanelAmountFont)，颜色设定为极其耀眼的青白色
        FontHelper.renderFontCentered(sb,
                FontHelper.topPanelAmountFont,
                costStr,
                this.cX,
                textY + 2.0F * Settings.scale, // 字体基线微调，使其在框内绝对居中
                new Color(0.6F, 1.0F, 1.0F, 1.0F));
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

    // ==========================================
    // 产生格挡时：科技感光环扩散脉冲
    // ==========================================
    private static class RegisterRingPulseEffect extends AbstractGameEffect {
        private float x;
        private float y;
        private float scaleMult;
        private com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion img;

        public RegisterRingPulseEffect(float x, float y) {
            this.x = x;
            this.y = y;
            this.duration = 0.5F;
            this.startingDuration = 0.5F;
            this.img = ImageMaster.WHITE_RING; // 原版标准的平滑光环

            // 亮青色，与你的待机粒子呼应
            this.color = new Color(0.2F, 1.0F, 1.0F, 0.8F);
        }

        @Override
        public void update() {
            this.duration -= Gdx.graphics.getDeltaTime();

            // 圆环迅速向外扩散放大
            this.scaleMult = Interpolation.exp10Out.apply(2.5F, 0.1F, this.duration / this.startingDuration);
            // 透明度快速衰减
            this.color.a = Interpolation.fade.apply(0.0F, 0.8F, this.duration / this.startingDuration);

            if (this.duration < 0.0F) {
                this.isDone = true;
            }
        }

        @Override
        public void render(SpriteBatch sb) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            float w = this.img.packedWidth;
            float h = this.img.packedHeight;

            sb.draw(this.img,
                    this.x - w / 2.0F, this.y - h / 2.0F,
                    w / 2.0F, h / 2.0F,
                    w, h,
                    this.scaleMult * Settings.scale, this.scaleMult * Settings.scale, 0.0F);
            sb.setBlendFunction(770, 771);
        }

        @Override
        public void dispose() {
        }
    }

    // ==========================================
    // 产生格挡时：先爆开、后追踪飞向玩家的小光点
    // ==========================================
    private static class RegisterHomingParticle extends AbstractGameEffect {
        private float x;
        private float y;
        private float vX;
        private float vY;
        private float targetX;
        private float targetY;
        private float size;
        private boolean isReturning = false;
        private float explodeTimer;
        private com.badlogic.gdx.graphics.Texture img;

        public RegisterHomingParticle(float x, float y) {
            this.img = ImageMaster.WHITE_SQUARE_IMG; // 和你的待机粒子用一样的正方形，靠渲染拉长
            this.x = x;
            this.y = y;

            // 爆炸阶段的随机方向和速度
            float angle = MathUtils.random(0.0F, 360.0F);
            float speed = MathUtils.random(200.0F, 600.0F) * Settings.scale;
            this.vX = MathUtils.cosDeg(angle) * speed;
            this.vY = MathUtils.sinDeg(angle) * speed;

            // 设置爆炸散射的持续时间（0.15秒~0.3秒不等，产生参差不齐的悬停感）
            this.explodeTimer = MathUtils.random(0.15F, 0.3F);

            this.color = new Color(MathUtils.random(0.6F, 1.0F), 1.0F, 1.0F, 1.0F); // 极亮的青白光点
            this.size = MathUtils.random(2.0F, 4.0F) * Settings.scale;
            this.duration = 1.5F; // 最大保底存活时间
        }

        @Override
        public void update() {
            this.x += this.vX * Gdx.graphics.getDeltaTime();
            this.y += this.vY * Gdx.graphics.getDeltaTime();

            if (!this.isReturning) {
                // 【阶段1：爆开与减速】
                this.vX *= 0.85F; // 空气阻力，迅速减速
                this.vY *= 0.85F;

                this.explodeTimer -= Gdx.graphics.getDeltaTime();
                if (this.explodeTimer <= 0.0F) {
                    this.isReturning = true; // 悬停结束，进入追踪阶段
                }
            } else {
                // 【阶段2：追踪并加速飞向玩家】
                // 实时获取玩家中心点作为目标
                this.targetX = AbstractDungeon.player.hb.cX;
                this.targetY = AbstractDungeon.player.hb.cY;

                float dx = this.targetX - this.x;
                float dy = this.targetY - this.y;
                float angleToTarget = (float) Math.atan2(dy, dx);

                // 施加极大的向心加速度
                float homingAccel = 4000.0F * Settings.scale * Gdx.graphics.getDeltaTime();
                this.vX += MathUtils.cos(angleToTarget) * homingAccel;
                this.vY += MathUtils.sin(angleToTarget) * homingAccel;

                // 如果距离玩家非常近，则直接消失（被吸收）
                if (Vector2.dst(this.x, this.y, this.targetX, this.targetY) < 40.0F * Settings.scale) {
                    this.isDone = true;
                }
            }

            this.duration -= Gdx.graphics.getDeltaTime();
            if (this.duration < 0.0F) {
                this.isDone = true;
            }
        }

        @Override
        public void render(SpriteBatch sb) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);

            // 获取当前运动方向，用于把方形拉长成线状
            float rot = MathUtils.atan2(this.vY, this.vX) * MathUtils.radiansToDegrees;

            // 根据速度动态拉长光点：速度越快，拉得越长
            float speedScale = (float) Math.sqrt(this.vX * this.vX + this.vY * this.vY) / (400.0F * Settings.scale);
            speedScale = MathUtils.clamp(speedScale, 0.5F, 3.0F);

            sb.draw(this.img,
                    this.x, this.y,
                    this.size / 2.0F, this.size / 2.0F,
                    this.size, this.size,
                    speedScale * 2.0F, 0.5F, rot,
                    0, 0, 32, 32, false, false);

            sb.setBlendFunction(770, 771);
        }

        @Override
        public void dispose() {
        }
    }
}
package foundry.veil.api.quasar.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.Veil;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.quasar.data.QuasarParticleData;
import foundry.veil.api.quasar.fx.Trail;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public class RenderData {

    @Deprecated
    private static final ResourceLocation BLANK = Veil.veilPath("textures/special/blank.png");

    private final Vector3d prevPosition;
    private final Vector3d renderPosition;
    private final Vector3f prevRotation;
    private final Vector3f renderRotation;
    private float prevRadius;
    private float renderRadius;
    private int lightColor;
    private float red;
    private float green;
    private float blue;
    private float alpha;
    public float renderAge;
    public float agePercent;
    private final boolean additive;
    private SpriteData spriteData;
    private TextureAtlasSprite atlasSprite;
    private RenderType renderType;
    private final List<Trail> trails;

    public RenderData(QuasarParticleData data) {
        this.prevPosition = new Vector3d();
        this.renderPosition = new Vector3d();
        this.prevRotation = new Vector3f();
        this.renderRotation = new Vector3f();
        this.prevRadius = 1.0F;
        this.renderRadius = 1.0F;
        this.lightColor = LightTexture.FULL_BRIGHT;
        this.red = 1.0F;
        this.green = 1.0F;
        this.blue = 1.0F;
        this.alpha = 1.0F;
        this.renderAge = 0.0F;
        this.additive = data.additive();
        this.spriteData = data.spriteData();
        this.atlasSprite = null;
        this.updateRenderType();
        this.trails = new ArrayList<>();
    }

    private void updateRenderType() {
        if (this.atlasSprite != null) {
            this.renderType = VeilRenderType.quasarParticle(this.atlasSprite.atlasLocation(), this.additive);
        } else if (this.spriteData != null) {
            this.renderType = VeilRenderType.quasarParticle(this.spriteData.sprite(), this.additive);
        } else {
            this.renderType = VeilRenderType.quasarParticle(BLANK, this.additive);
        }
    }

    @ApiStatus.Internal
    public void tick(QuasarParticle particle, int lightColor) {
        this.prevPosition.set(particle.getPosition());
        this.prevRotation.set(particle.getRotation());
        this.prevRadius = particle.getRadius();
        this.lightColor = lightColor;
    }

    @ApiStatus.Internal
    public void render(QuasarParticle particle, float partialTicks) {
        this.prevPosition.lerp(particle.getPosition(), partialTicks, this.renderPosition);
        this.prevRotation.lerp(particle.getRotation(), partialTicks, this.renderRotation);
        this.renderRadius = Mth.lerp(partialTicks, this.prevRadius, particle.getRadius());
        this.renderAge = particle.getAge() + partialTicks;
        this.agePercent = Math.min(this.renderAge / (float) particle.getLifetime(), 1.0F);
    }

    public Vector3dc getRenderPosition() {
        return this.renderPosition;
    }

    public Vector3fc getRenderRotation() {
        return this.renderRotation;
    }

    public float getRenderRadius() {
        return this.renderRadius;
    }

    public float getRenderAge() {
        return this.renderAge;
    }

    public float getAgePercent() {
        return this.agePercent;
    }

    public int getLightColor() {
        return this.lightColor;
    }

    public float getRed() {
        return this.red;
    }

    public float getGreen() {
        return this.green;
    }

    public float getBlue() {
        return this.blue;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public @Nullable SpriteData getSpriteData() {
        return this.spriteData;
    }

    public TextureAtlasSprite getAtlasSprite() {
        return this.atlasSprite;
    }

    public RenderType getRenderType() {
        return this.renderType;
    }

    public List<Trail> getTrails() {
        return this.trails;
    }

    // TODO move to renderer
    public void renderTrails(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 cameraPos, int packedLight) {
        if (this.trails.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
        for (Trail trail : this.trails) {
            trail.pushRotatedPoint(new Vec3(this.prevPosition.x, this.prevPosition.y, this.prevPosition.z), new Vec3(this.prevRotation.x, this.prevRotation.y, this.prevRotation.z));
            trail.render(poseStack, bufferSource.getBuffer(VeilRenderType.quasarTrail(trail.getTexture())), packedLight);
        }
        poseStack.popPose();
    }

    public void setRed(float red) {
        this.red = red;
    }

    public void setGreen(float green) {
        this.green = green;
    }

    public void setBlue(float blue) {
        this.blue = blue;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setColor(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void setColor(Vector4fc color) {
        this.red = color.x();
        this.green = color.y();
        this.blue = color.z();
        this.alpha = color.w();
    }

    public void setSpriteData(@Nullable SpriteData spriteData) {
        this.spriteData = spriteData;
        this.updateRenderType();
    }

    public void setAtlasSprite(@Nullable TextureAtlasSprite atlasSprite) {
        this.atlasSprite = atlasSprite;
        this.updateRenderType();
    }
}

package foundry.veil.mixin.performance.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;

@Mixin(ScreenEffectRenderer.class)
public class PerformanceScreenEffectRendererMixin {

    @Shadow
    @Final
    private static ResourceLocation UNDERWATER_LOCATION;

    @Unique
    private static final ResourceLocation BLIT_SCREEN_EFFECT_SHADER = Veil.veilPath("core/blit_screen_effect");

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void renderTex(TextureAtlasSprite texture, PoseStack poseStack, CallbackInfo ci) {
        ShaderProgram shader = VeilRenderSystem.setShader(BLIT_SCREEN_EFFECT_SHADER);
        if (shader == null) {
            return;
        }

        // Faster implementation that does everything on the GPU
        ci.cancel();

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        float xScale;
        float yScale;
        if (window.getWidth() > window.getHeight()) {
            xScale = 1;
            yScale = (float) window.getHeight() / window.getWidth();
        } else {
            xScale = (float) window.getWidth() / window.getHeight();
            yScale = 1;
        }

        float u0 = texture.getU0();
        float v0 = texture.getV0();
        float u1 = texture.getU1();
        float v1 = texture.getV1();
        float uWidth = u1 - u0;
        float vHeight = v1 - v0;

        shader.setVector("ColorModulator", 0.1F, 0.1F, 0.1F, 1.0F);
        shader.setVector("TexOffset", u0 + uWidth * (1.0F - xScale), v0 + vHeight * (1.0F - yScale), uWidth * xScale, vHeight * yScale);

        int activeTexture = GlStateManager._getActiveTexture();
        RenderSystem.activeTexture(GL_TEXTURE0);
        minecraft.getTextureManager().bindForSetup(texture.atlasLocation());

        shader.bind();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);
        ShaderProgram.unbind();

        RenderSystem.activeTexture(activeTexture);
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void renderWater(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        ShaderProgram shader = VeilRenderSystem.setShader(BLIT_SCREEN_EFFECT_SHADER);
        if (shader == null) {
            return;
        }

        // Faster implementation that does everything on the GPU
        ci.cancel();

        LocalPlayer player = minecraft.player;
        Window window = minecraft.getWindow();
        float xScale;
        float yScale;
        if (window.getWidth() > window.getHeight()) {
            xScale = 1;
            yScale = (float) window.getHeight() / window.getWidth();
        } else {
            xScale = (float) window.getWidth() / window.getHeight();
            yScale = 1;
        }

        float brightness = LightTexture.getBrightness(player.level().dimensionType(), player.level().getMaxLocalRawBrightness(player.blockPosition()));
        float u = player.getYRot() / 64.0F;
        float v = -player.getXRot() / 64.0F;

        shader.setVector("ColorModulator", brightness, brightness, brightness, 0.1F);
        shader.setVector("TexOffset", u + 2.0F * (1.0F - xScale), v + 2.0F * (1.0F - yScale), 2.0F * xScale, 2.0F * yScale);

        int activeTexture = GlStateManager._getActiveTexture();
        RenderSystem.activeTexture(GL_TEXTURE0);
        minecraft.getTextureManager().bindForSetup(UNDERWATER_LOCATION);

        RenderSystem.enableBlend();
        shader.bind();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 3);
        ShaderProgram.unbind();
        RenderSystem.disableBlend();

        RenderSystem.activeTexture(activeTexture);
    }
}

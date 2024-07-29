package foundry.veil.api.client.render.bloom;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.impl.client.render.BloomRendererImplCompute;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public class BloomManager {
    private static final ResourceLocation BLOOM_PREFILTER = Veil.veilPath("core/bloom_prefilter");
//    public static final ResourceLocation HDR_FINAL = Veil.veilPath("core/hdr_final");

    private BloomRenderer renderer;

    public BloomManager() {
        renderer = new BloomRendererImplCompute();
        initialize();
    }

    public void initialize(int width, int height) {
        if (renderer == null) return;
        renderer.initialize(width, height);
    }

    public void initialize() {
        initialize(
            Minecraft.getInstance().getMainRenderTarget().width,
            Minecraft.getInstance().getMainRenderTarget().height
        );
    }

    public void apply() {
        if (renderer == null) return;
        VeilRenderSystem.renderer().getPostProcessingManager()
                .runPipeline(BLOOM_PREFILTER, false);
        AdvancedFbo bloomFramebuffer = VeilRenderSystem.renderer().getFramebufferManager()
                .getFramebuffer(VeilFramebuffers.BLOOM);
        if (bloomFramebuffer == null) return;
        renderer.apply(bloomFramebuffer);
    }
}

package foundry.veil.impl.client.render.pipeline;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.ext.RenderTargetExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class VeilFirstPersonRenderer {

    private static final ResourceLocation FIRST_PERSON = Veil.veilPath("core/first_person");

    private static boolean printedError;
    private static boolean enabled;
    private static AdvancedFbo firstPerson;

    private VeilFirstPersonRenderer() {
    }

    public static void bind(int mask) {
        AdvancedFbo mainRenderTarget = AdvancedFbo.getMainFramebuffer();
        int w = mainRenderTarget.getWidth();
        int h = mainRenderTarget.getHeight();
        int framebufferTexture = mainRenderTarget.getColorTextureAttachment(0).getId();
        if (firstPerson == null || firstPerson.getWidth() != w || firstPerson.getHeight() != h) {
            free();
            firstPerson = AdvancedFbo.withSize(w, h)
                    .addColorTextureWrapper(framebufferTexture)
                    .setFormat(FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT)
                    .setDepthTextureBuffer()
                    .build(true);
        }
        VeilRenderSystem.renderer().getFramebufferManager().setFramebuffer(VeilFramebuffers.FIRST_PERSON, firstPerson);
        firstPerson.bind(false);
        firstPerson.clear(mask);
        // This redirects calls to the vanilla framebuffer to the first person buffer instead
        ((RenderTargetExtension) Minecraft.getInstance().getMainRenderTarget()).veil$setWrapper(firstPerson);
    }

    public static void unbind() {
        ((RenderTargetExtension) Minecraft.getInstance().getMainRenderTarget()).veil$setWrapper(null);

        VeilRenderer renderer = VeilRenderSystem.renderer();
        PostProcessingManager postProcessingManager = renderer.getPostProcessingManager();

        PostPipeline pipeline = postProcessingManager.getPipeline(FIRST_PERSON);
        if (pipeline == null) {
            if (!printedError) {
                Veil.LOGGER.error("Failed to apply first person pipeline");
                printedError = true;
            }
        } else {
            postProcessingManager.runPipeline(pipeline, false);
        }

        AdvancedFbo.unbind();
    }

    public static void free() {
        if (firstPerson != null) {
            VeilRenderSystem.renderer().getFramebufferManager().removeFramebuffer(VeilFramebuffers.FIRST_PERSON);
            firstPerson.free();
            firstPerson = null;
        }
        printedError = false;
    }
}

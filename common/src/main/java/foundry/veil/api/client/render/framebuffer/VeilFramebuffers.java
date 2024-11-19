package foundry.veil.api.client.render.framebuffer;

import foundry.veil.Veil;
import net.minecraft.resources.ResourceLocation;

/**
 * Default framebuffer names for use with {@link FramebufferManager#getFramebuffer(ResourceLocation)}.
 *
 * @author Ocelot
 */
public final class VeilFramebuffers {

    private VeilFramebuffers() {
    }

    public static final ResourceLocation MAIN = ResourceLocation.withDefaultNamespace("main");
    public static final ResourceLocation FIRST_PERSON = buffer("first_person");
    public static final ResourceLocation OPAQUE = buffer("opaque");
    public static final ResourceLocation OPAQUE_FINAL = buffer("opaque_final");
    public static final ResourceLocation TRANSPARENT = buffer("transparent");
    public static final ResourceLocation TRANSPARENT_FINAL = buffer("transparent_final");
    public static final ResourceLocation OPAQUE_LIGHT = buffer("opaque_light");
    public static final ResourceLocation TRANSPARENT_LIGHT = buffer("transparent_light");
    public static final ResourceLocation POST = buffer("post");

    private static ResourceLocation buffer(String name) {
        return Veil.veilPath(name);
    }
}

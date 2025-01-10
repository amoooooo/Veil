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
    public static final ResourceLocation BLOOM = buffer("bloom");
    public static final ResourceLocation LIGHT = buffer("light");
    public static final ResourceLocation POST = buffer("post");

    public static final ResourceLocation TRANSLUCENT_TARGET = transparency("translucent");
    public static final ResourceLocation ITEM_ENTITY_TARGET = transparency("item_entity");
    public static final ResourceLocation PARTICLES_TARGET = transparency("particles");
    public static final ResourceLocation WEATHER_TARGET = transparency("weather");
    public static final ResourceLocation CLOUDS_TARGET = transparency("clouds");

    private static ResourceLocation transparency(String name) {
        return ResourceLocation.withDefaultNamespace(name);
    }

    private static ResourceLocation buffer(String name) {
        return Veil.veilPath(name);
    }
}

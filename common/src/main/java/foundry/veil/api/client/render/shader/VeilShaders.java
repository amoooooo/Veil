package foundry.veil.api.client.render.shader;

import foundry.veil.Veil;
import net.minecraft.resources.ResourceLocation;

/**
 * Default shader names.
 */
public final class VeilShaders {

    private VeilShaders() {
    }

    public static final ResourceLocation PARTICLE = quasar("particle");

    public static final ResourceLocation LIGHT_DIRECTIONAL = light("directional");
    public static final ResourceLocation LIGHT_POINT = light("point");
    public static final ResourceLocation LIGHT_AREA = light("area");
    public static final ResourceLocation LIGHT_INDIRECT_SPHERE = light("indirect_sphere");

    public static final ResourceLocation SKINNED_MESH = necromancer("skinned_mesh");

    public static final ResourceLocation DEBUG_CUBEMAP = debug("cubemap");

    public static final ResourceLocation BLIT_SCREEN = Veil.veilPath("blit_screen");

    private static ResourceLocation quasar(String name) {
        return Veil.veilPath("quasar/" + name);
    }

    private static ResourceLocation light(String name) {
        return Veil.veilPath("light/" + name);
    }

    private static ResourceLocation necromancer(String name) {
        return Veil.veilPath("necromancer/" + name);
    }

    private static ResourceLocation debug(String name) {
        return Veil.veilPath("debug/" + name);
    }
}

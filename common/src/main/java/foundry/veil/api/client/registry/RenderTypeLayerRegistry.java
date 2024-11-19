package foundry.veil.api.client.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import foundry.veil.Veil;
import foundry.veil.api.client.render.rendertype.layer.*;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Registry for custom dynamic render type layers.
 */
public class RenderTypeLayerRegistry {

    public static final ResourceKey<Registry<LayerType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("render_type_layer"));
    private static final RegistrationProvider<LayerType<?>> VANILLA_PROVIDER = RegistrationProvider.get(REGISTRY_KEY, "minecraft");
    public static final Registry<LayerType<?>> REGISTRY = VANILLA_PROVIDER.asVanillaRegistry();

    public static final RegistryObject<LayerType<TextureLayer>> TEXTURE = register("texture", TextureLayer.CODEC);
    public static final RegistryObject<LayerType<MultiTextureLayer>> MULTI_TEXTURE = register("multi_texture", MultiTextureLayer.CODEC);
    public static final RegistryObject<LayerType<VanillaShaderLayer>> VANILLA_SHADER = register("shader", VanillaShaderLayer.CODEC);
    public static final RegistryObject<LayerType<VeilShaderLayer>> VEIL_SHADER = register(Veil.veilPath("shader"), VeilShaderLayer.CODEC);

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T extends RenderTypeLayer> RegistryObject<LayerType<T>> register(String name, MapCodec<T> codec) {
        return VANILLA_PROVIDER.register(name, () -> new LayerType<>(codec));
    }

    private static <T extends RenderTypeLayer> RegistryObject<LayerType<T>> register(ResourceLocation id, MapCodec<T> codec) {
        return VANILLA_PROVIDER.register(id, () -> new LayerType<>(codec));
    }

    public record LayerType<T extends RenderTypeLayer>(MapCodec<T> codec) {
    }
}

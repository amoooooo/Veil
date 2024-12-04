package foundry.veil.api.client.registry;

import foundry.veil.Veil;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.editor.*;
import foundry.veil.api.resource.type.BlockModelResource;
import foundry.veil.api.resource.type.FramebufferResource;
import foundry.veil.api.resource.type.VeilTextResource;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

public final class VeilResourceEditorRegistry {

    public static final ResourceKey<Registry<ResourceFileEditor.Factory<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Veil.veilPath("resource_editor"));
    private static final RegistrationProvider<ResourceFileEditor.Factory<?>> VANILLA_PROVIDER = RegistrationProvider.get(REGISTRY_KEY, Veil.MODID);
    public static final Registry<ResourceFileEditor.Factory<?>> REGISTRY = VANILLA_PROVIDER.asVanillaRegistry();

    public static final RegistryObject<ResourceFileEditor.Factory<VeilTextResource<?>>> TEXT = VANILLA_PROVIDER.register("text", () -> TextFileEditor::new);
    public static final RegistryObject<BlockModelEditor.Factory<BlockModelResource>> BLOCK_MODEL = VANILLA_PROVIDER.register("block_model", () -> BlockModelEditor::new);
    public static final RegistryObject<ResourceFileEditor.Factory<FramebufferResource>> FRAMEBUFFER = VANILLA_PROVIDER.register("framebuffer", () -> FramebufferFileEditor::new);

    public static final RegistryObject<ResourceOverrideEditor.Factory<VeilResource<?>>> OVERRIDE = VANILLA_PROVIDER.register("override", () -> ResourceOverrideEditor::new);

    private VeilResourceEditorRegistry() {
    }

    @ApiStatus.Internal
    public static void bootstrap() {
    }
}

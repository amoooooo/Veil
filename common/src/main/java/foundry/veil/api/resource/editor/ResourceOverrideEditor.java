package foundry.veil.api.resource.editor;

import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.ext.PackResourcesExtension;
import imgui.ImGui;
import imgui.flag.ImGuiSelectableFlags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

// TODO finish
public class ResourceOverrideEditor implements ResourceFileEditor<VeilResource<?>> {

    private static final Component NAME = Component.translatable("resource.veil.action.override");

    private final VeilEditorEnvironment environment;
    private final VeilResource<?> veilResource;
    private final List<Path> options = new ObjectArrayList<>();
    private boolean closed = false;
    private boolean opened = false;

    public ResourceOverrideEditor(VeilEditorEnvironment environment, VeilResource<?> veilResource) {
        this.environment = environment;
        this.veilResource = veilResource;

        VeilResourceManager resourceManager = this.environment.getResourceManager();

        for (PackResources pack : resourceManager.clientResources().listPacks().toList()) {
            if (!(pack instanceof PackResourcesExtension ext) || ext.veil$isStatic()) {
                continue;
            }

            List<Path> packRoots = ext.veil$getRawResourceRoots();
            if (packRoots.isEmpty()) {
                continue;
            }

            ResourceLocation location = this.veilResource.resourceInfo().location();
            for (Path devRoot : packRoots) {
                this.options.add(devRoot.resolve(PackType.CLIENT_RESOURCES.getDirectory())
                        .resolve(location.getNamespace())
                        .resolve(location.getPath()));
            }
        }
    }

    @Override
    public void render() {
        if (!this.opened) {
            this.opened = true;
            ImGui.openPopup("##asset_override");
        }

        if (!ImGui.beginPopup("##asset_override")) {
            this.closed = true;
            return;
        }

        VeilImGuiUtil.component(NAME);

        VeilResourceManager resourceManager = this.environment.getResourceManager();
        for (Path writePath : this.options) {
            if (ImGui.selectable(writePath.toString(), false, ImGuiSelectableFlags.AllowItemOverlap)) {
                Veil.LOGGER.info("Writing to {}", writePath);

                VeilResourceInfo info = this.veilResource.resourceInfo();
                try (InputStream stream = info.open(resourceManager)) {
                    Files.copy(stream, writePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    Veil.LOGGER.error("Failed to copy file: {}", info.location(), e);
                }
            }
            ImGui.setItemAllowOverlap();
            ImGui.sameLine();

            ImGui.text(writePath.toString());

            ImGui.setItemAllowOverlap();
        }

        if (this.options.isEmpty()) {
            ImGui.textDisabled("No valid packs");
        }

        ImGui.endPopup();
    }

    @Override
    public void loadFromDisk() {
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public VeilResource<?> getResource() {
        return this.veilResource;
    }
}

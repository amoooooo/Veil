package foundry.veil.api.resource.editor;

import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.ext.PackResourcesExtension;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiSelectableFlags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ResourceOverrideEditor implements ResourceFileEditor<VeilResource<?>> {

    private static final Component NAME = Component.translatable("editor.veil.resource.action.override");
    private final VeilEditorEnvironment environment;
    private final VeilResource<?> veilResource;
    private final List<Path> options = new ObjectArrayList<>();
    private boolean closed = false;
    private boolean opened = false;
    private int viablePackCount = 0;

    public ResourceOverrideEditor(VeilEditorEnvironment environment, VeilResource<?> veilResource) {
        this.environment = environment;
        this.veilResource = veilResource;

        VeilResourceManager resourceManager = this.environment.getResourceManager();

        for (PackResources pack : resourceManager.clientResources().listPacks().toList()) {
            if (!(pack instanceof PackResourcesExtension) || ((PackResourcesExtension) pack).veil$isStatic())
                continue;

            this.viablePackCount++;


            Path buildRoot = ((PackResourcesExtension) pack).veil$getModResourcePath();
            Collection<Path> devRoots = PackResourcesExtension.findDevPaths(buildRoot, buildRoot);

            ResourceLocation location = this.veilResource.resourceInfo().location();
            for (Path devRoot : devRoots) {
                Path writePath = devRoot.resolve(PackType.CLIENT_RESOURCES.getDirectory()).resolve(location.getNamespace());

                for (String dir : location.getPath().split("/")) {
                    writePath = writePath.resolve(dir);
                }

                options.add(writePath);
            }


        }
    }

    @Override
    public void render() {
        final String name = "##asset_override";

        if (!this.opened) {
            this.opened = true;
            ImGui.openPopup(name);
        }

        if (ImGui.beginPopup(name)) {
            VeilImGuiUtil.component(NAME);

            VeilResourceManager resourceManager = this.environment.getResourceManager();


            for (Path writePath : this.options) {


                if (ImGui.selectable("##" + writePath.toString(), false, ImGuiSelectableFlags.AllowItemOverlap)) {
                    try (BufferedReader reader = this.veilResource.resourceInfo().openAsReader(resourceManager)){
                        Veil.LOGGER.info("Writing to {}", writePath);

                        File file = writePath.toFile();
                        file.getParentFile().mkdirs();

                        FileWriter writer = new FileWriter(file);


                        char[] buffer = new char[1024];
                        int read;

                        while ((read = reader.read(buffer)) != -1) {
                            writer.write(buffer, 0, read);
                        }

                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                ImGui.setItemAllowOverlap();
                ImGui.sameLine();

                ImGui.text(writePath.toString());

                ImGui.setItemAllowOverlap();

            }


            if (this.viablePackCount == 0) {
                ImGui.textDisabled("No viable packs");
            }

            ImGui.endPopup();
        } else {
            this.closed = true;
        }
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

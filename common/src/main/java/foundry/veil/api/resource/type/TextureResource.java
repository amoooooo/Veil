package foundry.veil.api.resource.type;

import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.compat.SodiumCompat;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.util.CompositeReloadListener;
import foundry.veil.ext.TextureAtlasExtension;
import foundry.veil.mixin.resource.accessor.ResourceAtlasSetAccessor;
import foundry.veil.mixin.resource.accessor.ResourceModelManagerAccessor;
import foundry.veil.mixin.resource.accessor.ResourceTextureAtlasAccessor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record TextureResource(VeilResourceInfo resourceInfo) implements VeilResource<TextureResource> {

    @Override
    public void render(boolean dragging, boolean fullName) {
        float size = ImGui.getTextLineHeight();
        int texture = Minecraft.getInstance().getTextureManager().getTexture(this.resourceInfo.location()).getId();

        ImGui.pushStyleColor(ImGuiCol.Text, this.resourceInfo.isStatic() ? 0xFFAAAAAA : 0xFFFFFFFF);
        if (dragging) {
            ImGui.image(texture, size * 8, size * 8);
            VeilImGuiUtil.resourceLocation(this.resourceInfo().location());
        } else {
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
            ImGui.setItemAllowOverlap();
            ImGui.image(texture, size, size);
            ImGui.sameLine();
            ImGui.popStyleVar();

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.image(texture, size * 16, size * 16);
                ImGui.endTooltip();
            }
            ImGui.sameLine();

            if (fullName) {
                VeilImGuiUtil.resourceLocation(this.resourceInfo.location());
            } else {
                ImGui.text(this.resourceInfo.fileName());
            }
        }
        ImGui.popStyleColor();
    }

    @Override
    public List<VeilResourceAction<TextureResource>> getActions() {
        return List.of();
    }

    @Override
    public boolean canHotReload() {
        return true;
    }

    @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        ResourceLocation location = this.resourceInfo.location();
        ResourceManager resources = resourceManager.resources(this.resourceInfo);
        Minecraft client = Minecraft.getInstance();
        TextureManager textureManager = client.getTextureManager();
        AbstractTexture texture = textureManager.getTexture(location, null);
        if (texture != null) {
            texture.reset(textureManager, resources, location, client);
        }

        ModelManager modelManager = client.getModelManager();
        AtlasSet atlases = ((ResourceModelManagerAccessor) modelManager).getAtlases();
        ResourceLocation id = SpriteSource.TEXTURE_ID_CONVERTER.fileToId(location);

        boolean reloadRequired = false;
        for (Map.Entry<ResourceLocation, AtlasSet.AtlasEntry> entry : ((ResourceAtlasSetAccessor) atlases).getAtlases().entrySet()) {
            if (((TextureAtlasExtension) entry.getValue().atlas()).veil$hasTexture(id)) {
                reloadRequired = true;
                break;
            }
        }

        // FIXME fluids and item models still retain the old sprite objects, so they don't animate after this
        if (SodiumCompat.INSTANCE != null) {
            // The model manager has to be reloaded to make sure the sprites are correctly updated on Sodium
            if (reloadRequired) {
                CompositeReloadListener.of(modelManager, client.getBlockRenderer(), client.getItemRenderer()).reload(
                        CompletableFuture::completedFuture,
                        resources,
                        InactiveProfiler.INSTANCE,
                        InactiveProfiler.INSTANCE,
                        Util.backgroundExecutor(),
                        client
                ).thenRunAsync(VeilRenderSystem::rebuildChunks, VeilRenderSystem.renderThreadExecutor());
            }
        } else {
            for (Map.Entry<ResourceLocation, AtlasSet.AtlasEntry> entry : ((ResourceAtlasSetAccessor) atlases).getAtlases().entrySet()) {
                TextureAtlas atlas = entry.getValue().atlas();
                if (((TextureAtlasExtension) atlas).veil$hasTexture(id)) {
                    int mipLevel = ((ResourceTextureAtlasAccessor) atlas).getMipLevel();
                    SpriteLoader.create(atlas)
                            .loadAndStitch(resources, entry.getValue().atlasInfoLocation(), mipLevel, Util.backgroundExecutor())
                            .thenCompose(SpriteLoader.Preparations::waitForUpload)
                            .thenAcceptAsync(preparations -> {
                                atlas.upload(preparations);
                                VeilRenderSystem.rebuildChunks();
                            }, VeilRenderSystem.renderThreadExecutor());
                }
            }
        }
    }

    @Override
    public int getIconCode() {
        return 0xF3C5; // Image file icon
    }
}

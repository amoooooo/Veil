package foundry.veil.api.resource.type;

import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.ext.TextureAtlasExtension;
import foundry.veil.mixin.accessor.AtlasSetAccessor;
import foundry.veil.mixin.accessor.ModelManagerAccessor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Override
    public void hotReload(VeilResourceManager resourceManager) throws IOException {
        ResourceLocation location = this.resourceInfo.location();
        ResourceManager resources = resourceManager.resources(this.resourceInfo);
        Minecraft client = Minecraft.getInstance();
        client.getTextureManager().release(location);

        ModelManagerAccessor modelManager = (ModelManagerAccessor) client.getModelManager();
        int mipLevel = modelManager.getMaxMipmapLevels();
        AtlasSet atlases = modelManager.getAtlases();
        ResourceLocation id = SpriteSource.TEXTURE_ID_CONVERTER.fileToId(location);

        for (Map.Entry<ResourceLocation, AtlasSet.AtlasEntry> entry : ((AtlasSetAccessor) atlases).getAtlases().entrySet()) {
            TextureAtlas atlas = entry.getValue().atlas();
            if (((TextureAtlasExtension) atlas).veil$hasTexture(id)) {
                SpriteLoader.create(atlas)
                        .loadAndStitch(resources, entry.getValue().atlasInfoLocation(), mipLevel, Util.backgroundExecutor())
                        .thenAcceptAsync(preparations -> {
                            atlas.upload(preparations);
                            VeilRenderSystem.rebuildChunks();
                        }, VeilRenderSystem.renderThreadExecutor());
            }
        }
    }

    @Override
    public int getIconCode() {
        return 0xF3C5; // Image file icon
    }
}

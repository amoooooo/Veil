package foundry.veil.api.resource.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.type.BlockModelResource;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import org.joml.*;

import java.io.Reader;
import java.lang.Math;
import java.util.List;

/**
 * Viewer for block models
 *
 * @author ryanhcode
 */
public class BlockModelInspector implements ResourceFileEditor<BlockModelResource> {

    private static final Component TITLE = Component.translatable("inspector.veil.block_model.title");
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final PoseStack.Pose POSE = new PoseStack().last();

    private final ImBoolean open;
    private final VeilResourceManager resourceManager;
    private final BlockModelResource resource;

    private ObjectArrayList<BakedQuad> quads;

    public BlockModelInspector(VeilEditorEnvironment environment, BlockModelResource resource) {
        this.open = new ImBoolean(true);
        this.resourceManager = environment.getResourceManager();
        this.resource = resource;
        this.loadFromDisk();
    }

    @Override
    public void render() {
        if (this.resource == null || !this.open.get()) {
            return;
        }

        VeilResourceInfo resourceInfo = this.resource.resourceInfo();

        ImGui.setNextWindowSizeConstraints(256.0F, 256.0F, Float.MAX_VALUE, Float.MAX_VALUE);
        ImGui.setNextWindowSize(256.0F, 256.0F, ImGuiCond.Once);
        if (ImGui.begin(TITLE.getString() + "###model_editor_" + resourceInfo.fileName(), this.open, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoSavedSettings)) {
            VeilImGuiUtil.resourceLocation(resourceInfo.location());
            int desiredWidth = ((int) ImGui.getContentRegionAvailX() - 2) * 2;
            int desiredHeight = ((int) ImGui.getContentRegionAvailY() - 2) * 2;

            if (desiredWidth <= 0 || desiredHeight <= 0) {
                ImGui.end();
                return;
            }

            int texture = VeilImGuiUtil.renderArea(desiredWidth, desiredHeight, fbo -> {
                double time = ImGui.getTime();
                double yaw = Math.toRadians(time * 45.0);
                double pitch = Math.toRadians(30.0);

                Quaterniond cameraOrientation = new Quaterniond().rotateX(pitch).rotateY(yaw);
                Vector3d cameraPos = cameraOrientation.transformInverse(new Vector3d(0.0, 0.0, 2.8)).add(0.5, 0.5, 0.5);

                Matrix4f viewMatrix = new Matrix4f().rotate(new Quaternionf(cameraOrientation)).translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);

                float aspect = (float) desiredWidth / (float) desiredHeight;
                Matrix4f projMat = new Matrix4f().perspective((float) Math.toRadians(40.0), aspect, 0.3f, 1000.0f);
                Matrix4f modelView = new Matrix4f().mul(viewMatrix);

                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

                for (BakedQuad quad : quads) {
                    builder.putBulkData(POSE, quad, 1.0F, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                }

                // draw!
                MeshData data = builder.build();

                if (data != null) {
                    RenderType renderType = RenderType.translucent();
                    Matrix4fStack stack = RenderSystem.getModelViewStack();

                    stack.pushMatrix();
                    stack.set(modelView);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.backupProjectionMatrix();
                    RenderSystem.setProjectionMatrix(projMat, VertexSorting.ORTHOGRAPHIC_Z);

                    renderType.draw(data);

                    stack.popMatrix();
                    RenderSystem.restoreProjectionMatrix();
                    RenderSystem.applyModelViewMatrix();
                    renderType.clearRenderState();
                }
            });

            if (ImGui.beginChild("3D View", desiredWidth / 2.0F + 2, desiredHeight / 2.0F + 2, false, ImGuiWindowFlags.NoScrollbar)) {
                ImGui.image(texture, desiredWidth / 2.0F, desiredHeight / 2.0F, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.1F);
            }
            ImGui.endChild();
        }
        ImGui.end();
    }

    @Override
    public boolean isClosed() {
        return !this.open.get();
    }

    @Override
    public BlockModelResource getResource() {
        return this.resource;
    }

    @Override
    public void loadFromDisk() {
        Minecraft client = Minecraft.getInstance();
        this.quads = new ObjectArrayList<>();

        try (Reader reader = resource.resourceInfo().openAsReader(resourceManager)) {
            BlockModel unbaked = BlockModel.fromStream(reader);

            unbaked.resolveParents((location) -> {
                try (Reader parentReader = client.getResourceManager().openAsReader(ModelBakery.MODEL_LISTER.idToFile(location))) {
                    return BlockModel.fromStream(parentReader);
                } catch (Exception e) {
                    Veil.LOGGER.error("Failed to load block model", e);
                    return BlockModel.fromString(ModelBakery.MISSING_MODEL_MESH);
                }
            });

            List<BlockElement> elements = unbaked.getElements();

            for (BlockElement blockelement : elements) {
                for (Direction direction : blockelement.faces.keySet()) {
                    BlockElementFace blockelementface = blockelement.faces.get(direction);
                    Material material = unbaked.getMaterial(blockelementface.texture());
                    TextureAtlasSprite sprite = client.getTextureAtlas(material.atlasLocation()).apply(material.texture());

                    quads.add(FACE_BAKERY.bakeQuad(blockelement.from, blockelement.to, blockelementface, sprite, direction, BlockModelRotation.X0_Y0, blockelement.rotation, blockelement.shade));
                }
            }
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to load block model", e);
        }
    }
}

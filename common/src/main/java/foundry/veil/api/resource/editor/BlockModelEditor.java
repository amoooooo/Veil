package foundry.veil.api.resource.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.framebuffer.*;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.type.BlockModelResource;
import imgui.*;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import org.joml.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.Math;
import java.util.*;

import static org.lwjgl.opengl.GL11C.glClearColor;

/**
 * Viewer for block models
 */
public class BlockModelEditor implements ResourceFileEditor<BlockModelResource> {

    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final PoseStack.Pose POSE = new PoseStack().last();

    private final ImBoolean open;
    private final VeilResourceManager resourceManager;
    private final BlockModelResource resource;

    private AdvancedFbo fbo;
    private ObjectArrayList<BakedQuad> quads;

    public BlockModelEditor(VeilEditorEnvironment environment, BlockModelResource resource) {
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

        ImGui.setNextWindowSize(256.0F, 256.0F, ImGuiCond.Once);
        if (ImGui.begin("Model Viewer" + "###model_editor_" + this.resource.resourceInfo().fileName(), this.open)) {
            VeilImGuiUtil.resourceLocation(resource.resourceInfo().location());
            int desiredWidth = ((int) ImGui.getContentRegionAvailX() - 2) * 2;
            int desiredHeight = ((int) ImGui.getContentRegionAvailY() - 2) * 2;


            if (desiredWidth <= 0 || desiredHeight <= 0) {
                ImGui.end();
                return;
            }

            if (fbo == null || fbo.getWidth() != desiredWidth || fbo.getHeight() != desiredHeight) {
                if (fbo != null) fbo.free();

                fbo = AdvancedFbo.withSize(desiredWidth, desiredHeight).addColorTextureBuffer().setDepthRenderBuffer().build(true);
            }

            double time = ImGui.getTime();
            double yaw = Math.toRadians(time * 45.0);
            double pitch = Math.toRadians(30.0);

            Quaterniond cameraOrientation = new Quaterniond().rotateX(pitch).rotateY(yaw);
            Vector3d cameraPos = cameraOrientation.transformInverse(new Vector3d(0.0, 0.0, 2.8)).add(0.5, 0.5, 0.5);

            fbo.bind(true);

            ImVec4 clearColor = new ImVec4();
            ImGui.getStyleColorVec4(ImGuiCol.FrameBg, clearColor);

            glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
            fbo.clear();

            Matrix4f viewMatrix = new Matrix4f().rotate(new Quaternionf(cameraOrientation)).translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);

            float aspect = (float) desiredWidth / (float) desiredHeight;
            Matrix4f projMat = new Matrix4f().perspective((float) Math.toRadians(40.0), aspect, 0.3f, 1000.0f);
            Matrix4f modelView = new Matrix4f().mul(viewMatrix);

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

            for (BakedQuad quad : quads) {
                builder.putBulkData(POSE, quad, 1.0F, 1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }

            RenderType renderType = RenderType.translucent();
            Matrix4fStack stack = RenderSystem.getModelViewStack();

            stack.pushMatrix();
            stack.set(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(projMat, VertexSorting.ORTHOGRAPHIC_Z);

            // draw!
            MeshData data = builder.build();

            if (data != null) {
                renderType.draw(data);
            }

            stack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.applyModelViewMatrix();
            renderType.clearRenderState();

            AdvancedFboTextureAttachment attachment = fbo.getColorTextureAttachment(0);
            if (ImGui.beginChild("3D View", desiredWidth / 2 + 2, desiredHeight / 2 + 2, false, ImGuiWindowFlags.NoMove)) {
                ImGui.image(attachment.getId(), desiredWidth / 2, desiredHeight / 2, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.1F);
            }
            ImGui.endChild();

            AdvancedFbo.unbind();
        }
        ImGui.end();
    }

    @Override
    public boolean isClosed() {
        return !this.open.get();
    }

    @Override
    public void close() {
        this.fbo.free();
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
                } catch (IOException e) {
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
        } catch (IOException e) {
            Veil.LOGGER.error("Failed to load block model", e);
        }
    }
}

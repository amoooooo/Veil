package foundry.veil.impl.resource.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.CameraMatrices;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.Objects;

@ApiStatus.Internal
public class VeilShaderDefinitionResourceRenderer {

    private static final CameraMatrices CAMERA_MATRICES = new CameraMatrices();
    private static final Matrix4f TRANSFORM_MAT = new Matrix4f();
    private static final Matrix3f NORMAL_MAT = new Matrix3f();
    private static final Vector3f POSITION = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();

    private static final ResourceLocation DIRT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    private static final ResourceLocation WHITE_TEXTURE = Veil.veilPath("textures/special/blank.png");

    private static ResourceLocation last = null;
    private static boolean valid = false;

    static {
        CAMERA_MATRICES.getProjectionMatrix().identity();
        CAMERA_MATRICES.getInverseProjectionMatrix().identity();

        CAMERA_MATRICES.getViewMatrix().identity();
        CAMERA_MATRICES.getInverseViewMatrix().identity();
        CAMERA_MATRICES.getInverseViewRotMatrix().identity();

        CAMERA_MATRICES.setNearPlane(0.0F);
        CAMERA_MATRICES.setFarPlane(1.0F);
        CAMERA_MATRICES.getCameraPosition().set(0);
    }

    public static void render(ShaderProgram shader, float width, float height) {
        VertexFormat format = Objects.requireNonNull(shader.getFormat());
        if (!shader.getId().equals(last)) {
            last = shader.getId();
            valid = true;
        }

        if (!valid) {
            return;
        }

        float[] VERTICES = new float[]{
                // Back
                -0.5F, -0.5F, -0.5F,
                -0.5F, 0.5F, -0.5F,
                0.5F, 0.5F, -0.5F,
                0.5F, -0.5F, -0.5F,

                // Front
                0.5F, -0.5F, 0.5F,
                0.5F, 0.5F, 0.5F,
                -0.5F, 0.5F, 0.5F,
                -0.5F, -0.5F, 0.5F,

                // Left
                -0.5F, -0.5F, 0.5F,
                -0.5F, 0.5F, 0.5F,
                -0.5F, 0.5F, -0.5F,
                -0.5F, -0.5F, -0.5F,

                // Right
                0.5F, -0.5F, -0.5F,
                0.5F, 0.5F, -0.5F,
                0.5F, 0.5F, 0.5F,
                0.5F, -0.5F, 0.5F,

                // Top
                -0.5F, 0.5F, -0.5F,
                -0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, 0.5F,
                0.5F, 0.5F, -0.5F,
        };

        float[] UVS = {
                0, 0,
                0, 1,
                1, 1,
                1, 0,
        };

        float[] NORMALS = new float[]{
                0.0F, 0.0F, -1.0F,
                0.0F, 0.0F, 1.0F,
                -1.0F, 0.0F, 0.0F,
                1.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F
        };

        double time = ImGui.getTime();
        float yaw = (float) Math.toRadians(time * 45.0);
        float pitch = (float) Math.toRadians(30.0);

        Matrix4f transform = TRANSFORM_MAT.setPerspective((float) Math.toRadians(90.0), width / height, 0.3f, 1000.0f).translate(0, 0, -1).rotateX(pitch).rotateY(yaw);
        Matrix3f normalMat = transform.normal(NORMAL_MAT);

        Tesselator tesselator = Tesselator.getInstance();
        MeshData data;
        try {
            BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, format);
            for (int i = 0; i < VERTICES.length / 3; i++) {
                for (VertexFormatElement element : format.getElements()) {
                    if (element == VertexFormatElement.POSITION) {
                        transform.transformPosition(VERTICES[i * 3], VERTICES[i * 3 + 1], VERTICES[i * 3 + 2], POSITION);
                        builder.addVertex(POSITION.x, POSITION.y, POSITION.z);
                    } else if (element == VertexFormatElement.COLOR) {
                        builder.setColor(0xFFFFFFFF);
                    } else if (element == VertexFormatElement.UV0) {
                        builder.setUv(UVS[(i % 4) * 2], UVS[(i % 4) * 2 + 1]);
                    } else if (element == VertexFormatElement.UV1) {
                        builder.setOverlay(OverlayTexture.NO_OVERLAY);
                    } else if (element == VertexFormatElement.UV2) {
                        builder.setLight(LightTexture.FULL_BRIGHT);
                    } else if (element == VertexFormatElement.NORMAL) {
                        normalMat.transform(NORMALS[i / 4], NORMALS[i / 4 + 1], NORMALS[i / 4 + 2], NORMAL);
                        builder.setNormal(NORMAL.x, NORMAL.y, NORMAL.z);
                    }
                }
            }

            data = builder.build();
        } catch (Exception e) {
            valid = false;
            tesselator.clear();
            e.printStackTrace();
            return;
        }

        if (data == null) {
            return;
        }

        OverlayTexture overlayTexture = Minecraft.getInstance().gameRenderer.overlayTexture();
        RenderSystem.setShaderTexture(0, DIRT_TEXTURE);
        overlayTexture.setupOverlayColor();
        RenderSystem.setShaderTexture(2, WHITE_TEXTURE);
        int texture = VeilImGuiUtil.renderArea((int) width, (int) height, fbo -> {
            Matrix4fStack stack = RenderSystem.getModelViewStack();

            stack.pushMatrix();
            stack.identity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);

//            CAMERA_MATRICES.bind();
            shader.bind();
            shader.applyRenderSystem();
            shader.addRenderSystemTextures();
            shader.applyShaderSamplers(0);
            shader.setMatrix("NormalMat", NORMAL_MAT);

            BufferUploader.draw(data);

            ShaderProgram.unbind();
//            VeilRenderSystem.renderer().getCameraMatrices().bind();

            stack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.applyModelViewMatrix();
        });
        RenderSystem.setShaderTexture(1, 0);
        overlayTexture.teardownOverlayColor();
        RenderSystem.setShaderTexture(2, 0);

        if (ImGui.beginChild("3D View", width + 2, height + 2, false, ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoBackground)) {
            ImGui.image(texture, width, height, 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.1F);
        }
        ImGui.endChild();
    }
}

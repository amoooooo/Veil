package foundry.veil.api.client.necromancer.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import org.joml.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.ARBDirectStateAccess.glNamedBufferSubData;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferSubData;
import static org.lwjgl.opengl.GL30C.glUniform1ui;
import static org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER;

public class Skin implements NativeResource {

    private final VertexArray vertexArray;
    private final Object2IntMap<String> boneIds;
    private final Vector4f color;

    private int instances;

    private final Matrix3f normalMatrix;
    private Matrix4x3f[] matrixStack;
    private Quaternionf[] orientationStack;

    public Skin(VertexArray vertexArray, Object2IntMap<String> boneIds) {
        this.vertexArray = vertexArray;
        this.boneIds = boneIds;
        this.color = new Vector4f();

        this.normalMatrix = new Matrix3f();
        this.matrixStack = null;
        this.orientationStack = null;
    }

    @ApiStatus.Internal
    public void render(RenderType renderType, List<Matrix4x3f> transforms, List<Skeleton> skeletons, int instancedBuffer, ByteBufferBuilder boneBuilder, int boneBuffer, DynamicShaderBlock<?> boneBlock, FloatList partialTicks) {
        if (skeletons.isEmpty()) {
            return;
        }

        if (this.instances != skeletons.size()) {
            VertexArrayBuilder format = this.vertexArray.editFormat();
            // The instanced buffer has to be redefined each time it changes size, so re-attach it
            format.defineVertexBuffer(1, instancedBuffer, 0, 6, 1);
            if (this.instances == 0) {
                format.setVertexIAttribute(4, 1, 1, VertexArrayBuilder.DataType.UNSIGNED_BYTE, 0); // Overlay Coordinates
                format.setVertexIAttribute(5, 1, 1, VertexArrayBuilder.DataType.UNSIGNED_BYTE, 1); // Lightmap Coordinates
                format.setVertexAttribute(6, 1, 4, VertexArrayBuilder.DataType.UNSIGNED_BYTE, true, 2); // Color
            }
            this.instances = skeletons.size();
        }

        Skeleton first = skeletons.getFirst();
        int maxDepth = first.getMaxDepth();
        if (this.matrixStack == null || this.matrixStack.length < maxDepth) {
            this.matrixStack = new Matrix4x3f[maxDepth];
            this.orientationStack = new Quaternionf[maxDepth];
            for (int i = 0; i < maxDepth; i++) {
                this.matrixStack[i] = new Matrix4x3f();
                this.orientationStack[i] = new Quaternionf();
            }
        }

        // Store bone data in buffer
        int skeletonDataSize = Skeleton.UNIFORM_STRIDE * this.getSkeletonDataSize();
        int size = skeletonDataSize * skeletons.size();
        ByteBuffer buffer = MemoryUtil.memByteBuffer(boneBuilder.reserve(size), size);

        for (int i = 0; i < skeletons.size(); i++) {
            Skeleton skeleton = skeletons.get(i);
            for (int j = 0; j < maxDepth; j++) {
                this.matrixStack[j].identity();
            }
            buffer.position(i * skeletonDataSize);
            skeleton.storeInstancedData(buffer, skeleton.roots, this.boneIds, 0, this.color, this.normalMatrix, transforms.get(i), this.matrixStack, this.orientationStack, partialTicks.getFloat(i));
        }

        buffer.rewind();
        ByteBufferBuilder.Result result = boneBuilder.build();
        if (result != null) {
            result.close();
        }

        // Upload data
        if (VeilRenderSystem.directStateAccessSupported()) {
            glNamedBufferSubData(boneBuffer, 0, buffer);
        } else {
            glBindBuffer(GL_UNIFORM_BUFFER, boneBuffer);
            glBufferSubData(GL_UNIFORM_BUFFER, 0, buffer);
            glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        // Draw
        this.vertexArray.bind();
        renderType.setupRenderState();
        VeilRenderSystem.bind("NecromancerBones", boneBlock);
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            shader.apply();

            Uniform uniform = shader.getUniform("NecromancerBoneCount");
            if (uniform != null) {
                glUniform1ui(uniform.getLocation(), this.boneIds.size());
            }
        }

        // TODO query uniform block size
        this.vertexArray.drawInstanced(GL_TRIANGLES, skeletons.size());

        if (shader != null) {
            shader.clear();
        }

        VeilRenderSystem.unbind(boneBlock);
        renderType.clearRenderState();
    }

    public VertexArray getVertexArray() {
        return this.vertexArray;
    }

    public int getSkeletonDataSize() {
        return this.boneIds.size();
    }

    public static VertexArray createVertexArray() {
        RenderSystem.assertOnRenderThreadOrInit();
        VertexArray vertexArray = VertexArray.create();

        int vbo = vertexArray.getOrCreateBuffer(VertexArray.VERTEX_BUFFER);
        VertexArrayBuilder format = vertexArray.editFormat();
        format.defineVertexBuffer(0, vbo, 0, 24, 0);

        format.setVertexAttribute(0, 0, 3, VertexArrayBuilder.DataType.FLOAT, false, 0); // Position
        format.setVertexAttribute(1, 0, 2, VertexArrayBuilder.DataType.FLOAT, false, 12); // UV
        format.setVertexAttribute(2, 0, 3, VertexArrayBuilder.DataType.BYTE, true, 20); // Normal
        format.setVertexIAttribute(3, 0, 1, VertexArrayBuilder.DataType.UNSIGNED_BYTE, 23); // Bone Index

        VertexArray.unbind();
        return vertexArray;
    }

    @Override
    public void free() {
        this.vertexArray.free();
    }

    public static Builder builder(int textureWidth, int textureHeight) {
        return new Builder(textureWidth, textureHeight);
    }

    public static class Builder {

        private static final Vector3f POS = new Vector3f();
        private static final Vector3f NORMAL = new Vector3f();

        private final VertexArray vertexArray;
        private final ByteBufferBuilder vertices;
        private final IntList indices;
        private final List<String> boneNames;
        private final float textureWidth;
        private final float textureHeight;
        private final Matrix4f position;
        private final Matrix3f normal;

        private int nextIndex;

        public Builder(float textureWidth, float textureHeight) {
            this.vertexArray = createVertexArray();
            this.vertices = new ByteBufferBuilder(Skeleton.MAX_BONES * 24 * 24);
            this.indices = new IntArrayList();
            this.boneNames = new ArrayList<>();
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.position = new Matrix4f();
            this.normal = new Matrix3f();

            this.nextIndex = 0;
        }

        private static byte normalIntValue(float value) {
            return (byte) ((int) (Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
        }

        public Builder startBone(String boneId) {
            if (this.boneNames.contains(boneId)) {
                throw new IllegalStateException("Bone '" + boneId + "' has already defined mesh data");
            }
            if (this.boneNames.size() >= Skeleton.MAX_BONES) {
                throw new IllegalStateException("Too many bones defined. Max is " + Skeleton.MAX_BONES);
            }

            this.boneNames.add(boneId);
            return this;
        }

        public Builder setTransform(MatrixStack stack) {
            return this.setTransform(stack.position());
        }

        public Builder setTransform(PoseStack stack) {
            return this.setTransform(stack.last());
        }

        public Builder setTransform(PoseStack.Pose pose) {
            return this.setTransform(pose.pose());
        }

        public Builder setTransform(Matrix4fc position) {
            this.position.set(position);
            this.position.normal(this.normal);
            return this;
        }

        public Builder addVertex(float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ) {
            if (this.boneNames.isEmpty()) {
                throw new IllegalStateException("No bone specified. Call #startBone(String) to start building a mesh.");
            }

            this.position.transformPosition(x, y, z, POS);
            this.normal.transform(normalX, normalY, normalZ, NORMAL);
            long pointer = this.vertices.reserve(24);
            MemoryUtil.memPutFloat(pointer, POS.x);
            MemoryUtil.memPutFloat(pointer + 4, POS.y);
            MemoryUtil.memPutFloat(pointer + 8, POS.z);
            MemoryUtil.memPutFloat(pointer + 12, u);
            MemoryUtil.memPutFloat(pointer + 16, v);
            MemoryUtil.memPutByte(pointer + 20, normalIntValue(NORMAL.x));
            MemoryUtil.memPutByte(pointer + 21, normalIntValue(NORMAL.y));
            MemoryUtil.memPutByte(pointer + 22, normalIntValue(NORMAL.z));
            MemoryUtil.memPutByte(pointer + 23, (byte) (this.boneNames.size() - 1));
            return this;
        }

        public Builder addIndex(int index) {
            this.indices.add(index);
            if (index > this.nextIndex) {
                this.nextIndex = index + 1;
            }
            return this;
        }

        public Builder addQuadIndices(int index) {
            this.addIndex(index);
            this.addIndex(index + 1);
            this.addIndex(index + 2);
            this.addIndex(index + 2);
            this.addIndex(index + 3);
            this.addIndex(index);
            return this;
        }

        public Builder addMirroredQuadIndices(int index) {
            this.addIndex(index);
            this.addIndex(index + 3);
            this.addIndex(index + 2);
            this.addIndex(index + 2);
            this.addIndex(index + 1);
            this.addIndex(index);
            return this;
        }

        public Builder addCube(float xSize, float ySize, float zSize, float xOffset, float yOffset, float zOffset, float xInflate, float yInflate, float zInflate, float uOffset, float vOffset, boolean mirrored) {
            float minX = xOffset;
            float minY = yOffset;
            float minZ = zOffset;
            float maxX = xOffset + xSize;
            float maxY = yOffset + ySize;
            float maxZ = zOffset + zSize;
            minX -= xInflate;
            minY -= yInflate;
            minZ -= zInflate;
            maxX += xInflate;
            maxY += yInflate;
            maxZ += zInflate;
            if (mirrored) {
                float swap = maxX;
                maxX = minX;
                minX = swap;
            }

            float eastUStart = uOffset;
            float northUStart = uOffset + Mth.floor(zSize);
            float westUStart = uOffset + Mth.floor(zSize) + Mth.floor(xSize);
            float southUStart = uOffset + Mth.floor(zSize) + Mth.floor(xSize) + Mth.floor(xSize);
            float southUEnd = uOffset + Mth.floor(zSize) + Mth.floor(xSize) + Mth.floor(zSize) + Mth.floor(xSize);

            float topVStart = vOffset;
            float sideVStart = vOffset + Mth.floor(zSize);
            float sideVEnd = vOffset + Mth.floor(zSize) + Mth.floor(ySize);

            // Up
            this.addVertex(minX, maxY, minZ, northUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, 1.0F, 0.0F);
            this.addVertex(minX, maxY, maxZ, northUStart / this.textureWidth, topVStart / this.textureHeight, 0.0F, 1.0F, 0.0F);
            this.addVertex(maxX, maxY, maxZ, westUStart / this.textureWidth, topVStart / this.textureHeight, 0.0F, 1.0F, 0.0F);
            this.addVertex(maxX, maxY, minZ, westUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, 1.0F, 0.0F);

            // Down
            this.addVertex(maxX, minY, maxZ, westUStart / this.textureWidth, topVStart / this.textureHeight, 0.0F, -1.0F, 0.0F);
            this.addVertex(minX, minY, maxZ, southUStart / this.textureWidth, topVStart / this.textureHeight, 0.0F, -1.0F, 0.0F);
            this.addVertex(minX, minY, minZ, southUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, -1.0F, 0.0F);
            this.addVertex(maxX, minY, minZ, westUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, -1.0F, 0.0F);

            // East
            this.addVertex(maxX, minY, maxZ, eastUStart / this.textureWidth, sideVEnd / this.textureHeight, 1.0F, 0.0F, 0.0F);
            this.addVertex(maxX, minY, minZ, northUStart / this.textureWidth, sideVEnd / this.textureHeight, 1.0F, 0.0F, 0.0F);
            this.addVertex(maxX, maxY, minZ, northUStart / this.textureWidth, sideVStart / this.textureHeight, 1.0F, 0.0F, 0.0F);
            this.addVertex(maxX, maxY, maxZ, eastUStart / this.textureWidth, sideVStart / this.textureHeight, 1.0F, 0.0F, 0.0F);

            // West
            this.addVertex(minX, minY, minZ, westUStart / this.textureWidth, sideVEnd / this.textureHeight, -1.0F, 0.0F, 0.0F);
            this.addVertex(minX, minY, maxZ, southUStart / this.textureWidth, sideVEnd / this.textureHeight, -1.0F, 0.0F, 0.0F);
            this.addVertex(minX, maxY, maxZ, southUStart / this.textureWidth, sideVStart / this.textureHeight, -1.0F, 0.0F, 0.0F);
            this.addVertex(minX, maxY, minZ, westUStart / this.textureWidth, sideVStart / this.textureHeight, -1.0F, 0.0F, 0.0F);

            // North
            this.addVertex(maxX, minY, minZ, northUStart / this.textureWidth, sideVEnd / this.textureHeight, 0.0F, 0.0F, -1.0F);
            this.addVertex(minX, minY, minZ, westUStart / this.textureWidth, sideVEnd / this.textureHeight, 0.0F, 0.0F, -1.0F);
            this.addVertex(minX, maxY, minZ, westUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, 0.0F, -1.0F);
            this.addVertex(maxX, maxY, minZ, northUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, 0.0F, -1.0F);

            // South
            this.addVertex(minX, minY, maxZ, southUStart / this.textureWidth, sideVEnd / this.textureHeight, 0.0F, 0.0F, 1.0F);
            this.addVertex(maxX, minY, maxZ, southUEnd / this.textureWidth, sideVEnd / this.textureHeight, 0.0F, 0.0F, 1.0F);
            this.addVertex(maxX, maxY, maxZ, southUEnd / this.textureWidth, sideVStart / this.textureHeight, 0.0F, 0.0F, 1.0F);
            this.addVertex(minX, maxY, maxZ, southUStart / this.textureWidth, sideVStart / this.textureHeight, 0.0F, 0.0F, 1.0F);

            for (int i = 0; i < 6; i++) {
                if (mirrored) {
                    this.addMirroredQuadIndices(this.nextIndex);
                } else {
                    this.addQuadIndices(this.nextIndex);
                }
            }

            return this;
        }

        public Builder addTri(
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3,
                float normalX, float normalY, float normalZ) {
            this.vertices.reserve(72);
            this.addVertex(x1, y1, z1, u1, v1, normalX, normalY, normalZ);
            this.addVertex(x2, y2, z2, u2, v2, normalX, normalY, normalZ);
            this.addVertex(x3, y3, z3, u3, v3, normalX, normalY, normalZ);
            this.addIndex(this.nextIndex);
            this.addIndex(this.nextIndex);
            this.addIndex(this.nextIndex);
            return this;
        }

        public Builder addFace(
                float x1, float y1, float z1, float u1, float v1,
                float x2, float y2, float z2, float u2, float v2,
                float x3, float y3, float z3, float u3, float v3,
                float x4, float y4, float z4, float u4, float v4,
                float normalX, float normalY, float normalZ
        ) {
            this.vertices.reserve(96);
            this.addVertex(x1, y1, z1, u1, v1, normalX, normalY, normalZ);
            this.addVertex(x2, y2, z2, u2, v2, normalX, normalY, normalZ);
            this.addVertex(x3, y3, z3, u3, v3, normalX, normalY, normalZ);
            this.addVertex(x4, y4, z4, u4, v4, normalX, normalY, normalZ);
            this.addQuadIndices(this.nextIndex);
            return this;
        }

        private void storeIndices(VertexArray.IndexType indexType, ByteBuffer buffer) {
            for (int i = 0; i < this.indices.size(); i++) {
                int index = this.indices.getInt(i);
                switch (indexType) {
                    case BYTE -> buffer.put(i, (byte) index);
                    case SHORT -> buffer.putShort(i * 2, (short) index);
                    case INT -> buffer.putInt(i * 4, index);
                }
            }
        }

        public Skin build() {
            ByteBuffer indices = null;
            try (this.vertices; ByteBufferBuilder.Result result = this.vertices.build()) {
                if (result == null) {
                    throw new IllegalStateException("No mesh data provides to skin");
                }
                int vertexBuffer = this.vertexArray.getOrCreateBuffer(VertexArray.VERTEX_BUFFER);
                VertexArray.upload(vertexBuffer, result.byteBuffer(), VertexArray.DrawUsage.STATIC);

                // Allocate and store index buffer
                VertexArray.IndexType indexType = VertexArray.IndexType.least(this.nextIndex - 1);
                indices = MemoryUtil.memAlloc(this.indices.size() * indexType.getBytes());
                this.storeIndices(indexType, indices);
                this.vertexArray.setIndexCount(this.indices.size(), indexType);
                this.vertexArray.uploadIndexBuffer(indices);

                Object2IntMap<String> boneIds = new Object2IntArrayMap<>(this.boneNames.size());
                for (int i = 0; i < this.boneNames.size(); i++) {
                    boneIds.put(this.boneNames.get(i), i);
                }

                return new Skin(this.vertexArray, Object2IntMaps.unmodifiable(boneIds));
            } finally {
                MemoryUtil.memFree(indices);
            }
        }
    }
}

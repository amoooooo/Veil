package foundry.veil.api.client.render.mesh;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.mesh.ARBVertexAttribBindingVertexArray;
import foundry.veil.impl.client.render.mesh.DSAVertexAttribBindingVertexArray;
import foundry.veil.impl.client.render.mesh.LegacyVertexArray;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.IntFunction;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.opengl.GL31C.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL43C.glMultiDrawElementsIndirect;

/**
 * More genetic alternative to {@link com.mojang.blaze3d.vertex.VertexBuffer VertexBuffer} that uses the latest available OpenGL version.
 *
 * @author Ocelot
 */
public abstract class VertexArray implements NativeResource {

    public static final int VERTEX_BUFFER = 0;
    public static final int ELEMENT_ARRAY_BUFFER = 1;

    private static VertexArrayType vertexArrayType;

    protected final int id;
    protected final Int2IntMap buffers;
    protected int indexCount;
    protected IndexType indexType;

    @ApiStatus.Internal
    protected VertexArray(int id) {
        this.id = id;
        this.buffers = new Int2IntArrayMap();
        this.indexCount = 0;
        this.indexType = IndexType.BYTE;
    }

    private static void loadType() {
        if (vertexArrayType == null) {
            if (VeilRenderSystem.directStateAccessSupported()) {
                vertexArrayType = VertexArrayType.DSA;
            } else {
                GLCapabilities caps = GL.getCapabilities();
                if (caps.OpenGL43 || caps.GL_ARB_vertex_attrib_binding) {
                    vertexArrayType = VertexArrayType.ARB;
                } else {
                    vertexArrayType = VertexArrayType.LEGACY;
                }
            }
        }
    }

    /**
     * Creates a single new vertex array.
     *
     * @return A new vertex array
     */
    public static VertexArray create() {
        RenderSystem.assertOnRenderThreadOrInit();
        loadType();
        return vertexArrayType.factory.apply(glGenVertexArrays());
    }

    /**
     * Creates an array of vertex arrays.
     *
     * @param count The number of arrays to create
     * @return An array of new vertex arrays
     */
    public static VertexArray[] create(int count) {
        VertexArray[] fill = new VertexArray[count];
        create(fill);
        return fill;
    }

    /**
     * Replaces each element of the specified array with a new vertex array.
     *
     * @param fill The array to fill
     */
    public static void create(VertexArray[] fill) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (fill.length == 0) {
            return;
        }

        loadType();
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            IntBuffer arrays = stack.mallocInt(fill.length);
            glGenVertexArrays(arrays);

            for (int i = 0; i < arrays.limit(); i++) {
                fill[i] = vertexArrayType.factory.apply(arrays.get(i));
            }
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    /**
     * Creates a new buffer object owned by this vertex array or retrieves an existing buffer.
     *
     * @param index The index of the buffer to get
     * @return A vertex array object
     */
    public int getOrCreateBuffer(int index) {
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return this.buffers.computeIfAbsent(index, unused -> this.createBuffer());
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public IndexType getIndexType() {
        return this.indexType;
    }

    /**
     * Uploads vanilla mc mesh data into this vertex array. Only a single mesh can be uploaded this way.
     *
     * @param meshData The data to upload
     * @param usage    The draw usage
     */
    public void upload(MeshData meshData, DrawUsage usage) {
        this.upload(0, meshData, usage);
    }

    /**
     * Uploads vanilla mc mesh data into this vertex array. Only a single mesh can be uploaded this way.
     *
     * @param attributeStart The attribute to start uploading vertex data to
     * @param meshData       The data to upload
     * @param usage          The draw usage
     */
    public void upload(int attributeStart, MeshData meshData, DrawUsage usage) {
        try (meshData) {
            RenderSystem.assertOnRenderThread();
            MeshData.DrawState drawState = meshData.drawState();
            VertexArrayBuilder builder = this.editFormat();

            int vertexBuffer = this.getOrCreateBuffer(VERTEX_BUFFER);
            this.uploadVertexBuffer(vertexBuffer, meshData.vertexBuffer(), usage.getGlType());
            builder.applyFrom(VERTEX_BUFFER, vertexBuffer, attributeStart, drawState.format());

            this.uploadIndexBuffer(drawState, meshData.indexBuffer(), usage.getGlType());
            this.indexCount = drawState.indexCount();
            this.indexType = IndexType.fromBlaze3D(drawState.indexType());
        }
    }

    protected void uploadIndexBuffer(MeshData.DrawState drawState, @Nullable ByteBuffer buffer, int usage) {
        if (buffer != null) {
            GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.getOrCreateBuffer(ELEMENT_ARRAY_BUFFER));
            RenderSystem.glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, usage);
        } else {
            RenderSystem.getSequentialBuffer(drawState.mode()).bind(drawState.indexCount());
        }
    }

    protected abstract int createBuffer();

    protected abstract void uploadVertexBuffer(int buffer, ByteBuffer data, int usage);

    /**
     * @return A builder for applying changes to this array
     */
    public abstract VertexArrayBuilder editFormat();

    /**
     * Binds this vertex array and applies any changes to the format automatically.
     */
    public void bind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(this.id);
    }

    public static void unbind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(0);
    }

    public void draw(int mode) {
        GlStateManager._drawElements(mode, this.indexCount, this.indexType.getGlType(), 0L);
    }

    public void drawInstanced(int mode, int instances) {
        glDrawElementsInstanced(mode, this.indexCount, this.indexType.getGlType(), 0L, instances);
    }

    public void drawIndirect(int mode, long indirect, int drawCount, int stride) {
        glMultiDrawElementsIndirect(mode, this.indexType.getGlType(), indirect, drawCount, stride);
    }

    public void setIndexCount(int indexCount) {
        this.indexCount = indexCount;
        this.indexType = IndexType.least(indexCount);
    }

    @Override
    public void free() {
        RenderSystem.assertOnRenderThreadOrInit();
        glDeleteBuffers(this.buffers.values().toIntArray());
        glDeleteVertexArrays(this.id);
        this.buffers.clear();
    }

    private enum VertexArrayType {
        LEGACY(LegacyVertexArray::new),
        ARB(ARBVertexAttribBindingVertexArray::new),
        DSA(DSAVertexAttribBindingVertexArray::new);

        private final IntFunction<VertexArray> factory;

        VertexArrayType(IntFunction<VertexArray> factory) {
            this.factory = factory;
        }
    }

    public enum IndexType {
        BYTE(GL_UNSIGNED_BYTE),
        SHORT(GL_UNSIGNED_SHORT),
        INT(GL_UNSIGNED_INT);

        private final int glType;
        private final int bytes;

        IndexType(int glType) {
            this.glType = glType;
            this.bytes = 1 << this.ordinal();
        }

        public int getGlType() {
            return this.glType;
        }

        public int getBytes() {
            return this.bytes;
        }

        public static IndexType fromBlaze3D(VertexFormat.IndexType type) {
            return switch (type) {
                case SHORT -> SHORT;
                case INT -> INT;
            };
        }

        public static IndexType least(int indexCount) {
            if ((indexCount & 0xFFFFFF00) == 0) {
                return BYTE;
            }
            if ((indexCount & 0xFFFF0000) == 0) {
                return SHORT;
            }
            return INT;
        }
    }

    public enum DrawUsage {
        STATIC(GL_STATIC_DRAW),
        DYNAMIC(GL_DYNAMIC_DRAW),
        STREAM(GL_STATIC_DRAW);

        private final int glType;

        DrawUsage(int glType) {
            this.glType = glType;
        }

        public int getGlType() {
            return this.glType;
        }

        public static DrawUsage fromBlaze3D(VertexBuffer.Usage type) {
            return switch (type) {
                case STATIC -> STATIC;
                case DYNAMIC -> DYNAMIC;
            };
        }
    }
}

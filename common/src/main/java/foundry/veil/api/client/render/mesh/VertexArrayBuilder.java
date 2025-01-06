package foundry.veil.api.client.render.mesh;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.api.client.render.VeilRenderSystem;

import java.util.List;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.opengl.GL41C.GL_FIXED;

/**
 * Builder for modifying vertex buffer state.
 *
 * @author Ocelot
 */
public interface VertexArrayBuilder {

    static void validateRelativeOffset(int offset) {
        if (offset < 0 || offset > VeilRenderSystem.maxVertexAttributeRelativeOffset()) {
            throw new IllegalArgumentException("Vertex array attribute relative offset must be between 0 and " + VeilRenderSystem.maxVertexAttributeRelativeOffset() + ". Was " + offset);
        }
    }

    /**
     * Applies the vanilla mc format at the specified index.
     *
     * @param index          The index to map the buffer to
     * @param buffer         The buffer to get data from
     * @param attributeStart The first attribute index to start applying the format from
     * @param format         The format to apply
     */
    default VertexArrayBuilder applyFrom(int index, int buffer, int attributeStart, VertexFormat format) {
        this.defineVertexBuffer(index, buffer, 0, format.getVertexSize());
        List<VertexFormatElement> elements = format.getElements();
        for (int i = 0; i < elements.size(); ++i) {
            VertexFormatElement element = elements.get(i);

            VertexFormatElement.Usage usage = element.usage();
            if (usage == VertexFormatElement.Usage.UV && element.type() != VertexFormatElement.Type.FLOAT) {
                this.setVertexIAttribute(
                        attributeStart + i,
                        index,
                        element.count(),
                        DataType.fromType(element.type()),
                        format.getOffset(element),
                        0
                );
            } else {
                this.setVertexAttribute(
                        attributeStart + i,
                        index,
                        element.count(),
                        DataType.fromType(element.type()),
                        usage == VertexFormatElement.Usage.NORMAL || usage == VertexFormatElement.Usage.COLOR,
                        format.getOffset(element),
                        0
                );
            }
            element.setupBufferState(i, format.getOffset(element), i);
        }
        return this;
    }

    /**
     * Maps a buffer region to the specified index. Allows swapping out vertex data with a single GL call.
     *
     * @param index  The index to assign to. It must be between 0 and {@link VeilRenderSystem#maxVertexAttributes()}
     * @param buffer The buffer to assign
     * @param offset The offset into the buffer to bind to
     * @param size   The size of the region to map
     */
    VertexArrayBuilder defineVertexBuffer(int index, int buffer, int offset, int size);

    /**
     * Defines a floating-point vertex attribute.
     *
     * @param index          The index of the attribute to assign
     * @param bufferIndex    The defined buffer index to use. Defined with {@link VertexArrayBuilder#defineVertexBuffer}
     * @param size           The size of the attribute. Can be 1, 2, 3, or 4
     * @param type           The type of data the shader will use
     * @param normalized     Whether to normalize the data from <code>-1</code> to <code>1</code> automatically
     * @param relativeOffset The offset in the buffer region the vertex data is defined at. It must be between 0 and {@link VeilRenderSystem#maxVertexAttributeRelativeOffset()}
     * @param divisor        The number of instances that have to pass to increment this attribute or <code>0</code> to increment per vertex
     */
    VertexArrayBuilder setVertexAttribute(int index, int bufferIndex, int size, DataType type, boolean normalized, int relativeOffset, int divisor);

    /**
     * Defines an integer vertex attribute.
     *
     * @param index          The index of the attribute to assign
     * @param bufferIndex    The defined buffer index to use. Defined with {@link VertexArrayBuilder#defineVertexBuffer}
     * @param size           The size of the attribute. Can be 1, 2, 3, or 4
     * @param type           The type of data the shader will use
     * @param relativeOffset The offset in the buffer region the vertex data is defined at. It must be between 0 and {@link VeilRenderSystem#maxVertexAttributeRelativeOffset()}
     * @param divisor        The number of instances that have to pass to increment this attribute or <code>0</code> to increment per vertex
     */
    VertexArrayBuilder setVertexIAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor);

    /**
     * Defines a long vertex attribute.
     *
     * @param index          The index of the attribute to assign
     * @param bufferIndex    The defined buffer index to use. Defined with {@link VertexArrayBuilder#defineVertexBuffer}
     * @param size           The size of the attribute. Can be 1, 2, 3, or 4
     * @param type           The type of data the shader will use
     * @param relativeOffset The offset in the buffer region the vertex data is defined at. It must be between 0 and {@link VeilRenderSystem#maxVertexAttributeRelativeOffset()}
     * @param divisor        The number of instances that have to pass to increment this attribute or <code>0</code> to increment per vertex
     */
    VertexArrayBuilder setVertexLAttribute(int index, int bufferIndex, int size, DataType type, int relativeOffset, int divisor);

    /**
     * Removes the buffer mapping with the specified index.
     *
     * @param index The index of the buffer to remove
     */
    VertexArrayBuilder removeVertexBuffer(int index);

    /**
     * Removes the attribute with the specified index.
     *
     * @param index The index of the attribute to remove
     */
    VertexArrayBuilder removeAttribute(int index);

    /**
     * Clears all mapped buffer regions.
     */
    VertexArrayBuilder clearVertexBuffers();

    /**
     * Clears all defined vertex attributes.
     */
    VertexArrayBuilder clearVertexAttributes();

    enum DataType {
        BYTE(GL_BYTE),
        SHORT(GL_SHORT),
        INT(GL_INT),
        FIXED(GL_FIXED),
        FLOAT(GL_FLOAT),
        HALF_FLOAT(GL_HALF_FLOAT),
        DOUBLE(GL_DOUBLE),

        UNSIGNED_BYTE(GL_UNSIGNED_BYTE),
        UNSIGNED_SHORT(GL_UNSIGNED_SHORT),
        UNSIGNED_INT(GL_UNSIGNED_INT),

        INT_2_10_10_10_REV(GL_INT_2_10_10_10_REV),
        UNSIGNED_INT_2_10_10_10_REV(GL_UNSIGNED_INT_2_10_10_10_REV),
        UNSIGNED_INT_10F_11F_11F_REV(GL_UNSIGNED_INT_10F_11F_11F_REV);

        private final int glType;

        DataType(int glType) {
            this.glType = glType;
        }

        public int getGlType() {
            return this.glType;
        }

        public static DataType fromType(VertexFormatElement.Type type) {
            return switch (type) {
                case FLOAT -> FLOAT;
                case UBYTE -> UNSIGNED_BYTE;
                case BYTE -> BYTE;
                case USHORT -> UNSIGNED_SHORT;
                case SHORT -> SHORT;
                case UINT -> UNSIGNED_INT;
                case INT -> INT;
            };
        }
    }
}

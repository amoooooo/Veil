package foundry.veil.api.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.util.Arrays;

import static com.mojang.blaze3d.vertex.VertexFormatElement.*;

public class VeilVertexFormat {

    public static final VertexFormatElement BONE_INDEX = register(0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.GENERIC, 1);

    // todo: padding???
    public static final VertexFormat SKINNED_MESH = VertexFormat.builder()
            .add("Position", POSITION)
            .add("Color", COLOR)
            .add("UV0", UV0) // texture coordinates
            .add("UV1", UV1) // lightmap coordinates
            .add("UV2", UV2) // overlay coordinates
            .add("Normal", NORMAL)
            .add("BoneIndex", BONE_INDEX)
            .build();

    /**
     * Registers a new vertex format element by assigning it to the next open ID, expanding the array if necessary.
     *
     * @param index The index of the element
     * @param type  The type of data to store
     * @param usage The way the element is used
     * @param count The number of components
     * @return A new element
     */
    public static VertexFormatElement register(int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        for (int i = 0; i < VertexFormatElement.BY_ID.length; i++) {
            if (VertexFormatElement.BY_ID[i] == null) {
                return VertexFormatElement.register(i, index, type, usage, count);
            }
        }

        VertexFormatElement.BY_ID = Arrays.copyOf(VertexFormatElement.BY_ID, VertexFormatElement.BY_ID.length + 1);
        return VertexFormatElement.register(VertexFormatElement.BY_ID.length - 2, index, type, usage, count);
    }
}

package foundry.veil.api.client.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;

public class VeilVertexFormat {
    public static final VertexFormatElement ELEMENT_BONE_INDEX = new VertexFormatElement(0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.GENERIC, 1);

    // todo: padding???
    public static final VertexFormat SKINNED_MESH = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement> builder()
                    .put("Position", ELEMENT_POSITION)
                    .put("Color", ELEMENT_COLOR)
                    .put("UV0", ELEMENT_UV0) // texture coordinates
                    .put("UV1", ELEMENT_UV1) // lightmap coordinates
                    .put("UV2", ELEMENT_UV2) // overlay coordinates
                    .put("Normal", ELEMENT_NORMAL)
                    .put("BoneIndex", ELEMENT_BONE_INDEX)
                    .build());
}

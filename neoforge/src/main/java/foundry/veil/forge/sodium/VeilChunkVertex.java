package foundry.veil.forge.sodium;

import foundry.veil.ext.ChunkVertexEncoderVertexExtension;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.DefaultChunkMeshAttributes;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

public class VeilChunkVertex implements ChunkVertexType {

    public static final int STRIDE = 23;
    public static final GlVertexFormat VERTEX_FORMAT = GlVertexFormat.builder(STRIDE)
            .addElement(DefaultChunkMeshAttributes.POSITION, 0, 0)
            .addElement(DefaultChunkMeshAttributes.COLOR, 1, 8)
            .addElement(DefaultChunkMeshAttributes.TEXTURE, 2, 12)
            .addElement(DefaultChunkMeshAttributes.LIGHT_MATERIAL_INDEX, 3, 16)
            .addElement(new VertexFormatAttribute("NORMAL_INDEX", GlVertexAttributeFormat.BYTE, 3, true, false), 4, 20)
            .build();
    private static final int POSITION_MAX_VALUE = 1048576;
    private static final int TEXTURE_MAX_VALUE = 32768;
    private static final float MODEL_ORIGIN = 8.0F;
    private static final float MODEL_RANGE = 32.0F;

    @Override
    public GlVertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, materialBits, vertices, section) -> {
            float texCentroidU = 0.0F;
            float texCentroidV = 0.0F;

            for (ChunkVertexEncoder.Vertex vertex : vertices) {
                texCentroidU += vertex.u;
                texCentroidV += vertex.v;
            }

            texCentroidU *= 0.25F;
            texCentroidV *= 0.25F;

            for (int i = 0; i < 4; ++i) {
                ChunkVertexEncoder.Vertex vertex = vertices[i];
                int x = quantizePosition(vertex.x);
                int y = quantizePosition(vertex.y);
                int z = quantizePosition(vertex.z);
                int u = encodeTexture(texCentroidU, vertex.u);
                int v = encodeTexture(texCentroidV, vertex.v);
                int light = encodeLight(vertex.light);
                MemoryUtil.memPutInt(ptr, packPositionHi(x, y, z));
                MemoryUtil.memPutInt(ptr + 4L, packPositionLo(x, y, z));
                MemoryUtil.memPutInt(ptr + 8L, ColorARGB.mulRGB(vertex.color, vertex.ao));
                MemoryUtil.memPutInt(ptr + 12L, packTexture(u, v));
                MemoryUtil.memPutInt(ptr + 16L, packLightAndData(light, materialBits, section));
                ChunkVertexEncoderVertexExtension ext = (ChunkVertexEncoderVertexExtension) vertex;
                MemoryUtil.memPutInt(ptr + 20L, ext.veil$getPackedNormal());
                ptr += STRIDE;
            }

            return ptr;
        };
    }

    private static int packPositionHi(int x, int y, int z) {
        return (x >>> 10 & 1023) | (y >>> 10 & 1023) << 10 | (z >>> 10 & 1023) << 20;
    }

    private static int packPositionLo(int x, int y, int z) {
        return (x & 1023) | (y & 1023) << 10 | (z & 1023) << 20;
    }

    private static int quantizePosition(float position) {
        return (int) (normalizePosition(position) * POSITION_MAX_VALUE) & (POSITION_MAX_VALUE - 1);
    }

    private static float normalizePosition(float v) {
        return (MODEL_ORIGIN + v) / MODEL_RANGE;
    }

    private static int packTexture(int u, int v) {
        return (u & 65535) | (v & 65535) << 16;
    }

    private static int encodeTexture(float center, float x) {
        int bias = x < center ? 1 : -1;
        int quantized = floorInt(x * TEXTURE_MAX_VALUE) + bias;
        return quantized & (TEXTURE_MAX_VALUE - 1) | sign(bias) << 15;
    }

    private static int encodeLight(int light) {
        int sky = Mth.clamp(light >>> 16 & 0xFF, 8, 248);
        int block = Mth.clamp(light & 0xFF, 8, 248);
        return block | sky << 8;
    }

    private static int packLightAndData(int light, int material, int section) {
        return (light & 65535) | (material & 0xFF) << 16 | (section & 0xFF) << 24;
    }

    private static int sign(int x) {
        return x >>> 31;
    }

    private static int floorInt(float x) {
        return (int) Math.floor(x);
    }
}

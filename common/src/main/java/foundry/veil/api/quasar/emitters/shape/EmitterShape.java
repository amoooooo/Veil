package foundry.veil.api.quasar.emitters.shape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import foundry.veil.api.quasar.registry.EmitterShapeRegistry;
import foundry.veil.api.util.CodecUtil;
import net.minecraft.util.RandomSource;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public interface EmitterShape {

    Vector3d getPoint(RandomSource randomSource, Vector3fc dimensions, Vector3fc rotation, Vector3dc position, boolean fromSurface);

    void renderShape(PoseStack stack, VertexConsumer consumer, Vector3fc dimensions, Vector3fc rotation);

    Codec<EmitterShape> CODEC = CodecUtil.registryOrLegacyCodec(EmitterShapeRegistry.REGISTRY);
}

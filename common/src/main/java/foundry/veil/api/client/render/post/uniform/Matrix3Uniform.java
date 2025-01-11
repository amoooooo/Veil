package foundry.veil.api.client.render.post.uniform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;

import java.util.List;

@ApiStatus.Internal
public record Matrix3Uniform(Matrix3fc value) implements UniformValue {

    public static final MapCodec<Matrix3Uniform> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.listOf(9, 9)
                    .<Matrix3fc>xmap(floats -> {
                        float[] values = new float[floats.size()];
                        for (int i = 0; i < floats.size(); i++) {
                            values[i] = floats.get(i);
                        }
                        return new Matrix3f().set(values);
                    }, matrix -> List.of(
                            matrix.m00(), matrix.m01(), matrix.m02(),
                            matrix.m10(), matrix.m11(), matrix.m12(),
                            matrix.m20(), matrix.m21(), matrix.m22()
                    ))
                    .fieldOf("values")
                    .forGetter(Matrix3Uniform::value)
    ).apply(instance, Matrix3Uniform::new));

    @Override
    public void apply(String name, MutableUniformAccess access) {
        access.setMatrix(name, this.value);
    }

    @Override
    public Type type() {
        return Type.MAT3;
    }
}

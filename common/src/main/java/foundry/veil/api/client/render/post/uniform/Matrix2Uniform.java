package foundry.veil.api.client.render.post.uniform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix2f;
import org.joml.Matrix2fc;

import java.util.List;

@ApiStatus.Internal
public record Matrix2Uniform(Matrix2fc value) implements UniformValue {

    public static final MapCodec<Matrix2Uniform> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.listOf(4, 4)
                    .<Matrix2fc>xmap(floats -> {
                        float[] values = new float[floats.size()];
                        for (int i = 0; i < floats.size(); i++) {
                            values[i] = floats.get(i);
                        }
                        return new Matrix2f().set(values);
                    }, matrix -> List.of(
                            matrix.m00(), matrix.m01(),
                            matrix.m10(), matrix.m11()
                    ))
                    .fieldOf("values")
                    .forGetter(Matrix2Uniform::value)
    ).apply(instance, Matrix2Uniform::new));

    @Override
    public void apply(String name, MutableUniformAccess access) {
        access.setMatrix(name, this.value);
    }

    @Override
    public Type type() {
        return Type.MAT2;
    }
}

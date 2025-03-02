package foundry.veil.api.client.render.post.uniform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.List;

public record Matrix4Uniform(Matrix4fc value) implements UniformValue {

    public static final MapCodec<Matrix4Uniform> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.listOf(16, 16)
                    .<Matrix4fc>xmap(floats -> {
                        float[] values = new float[floats.size()];
                        for (int i = 0; i < floats.size(); i++) {
                            values[i] = floats.get(i);
                        }
                        return new Matrix4f().set(values);
                    }, matrix -> List.of(
                            matrix.m00(), matrix.m01(), matrix.m02(), matrix.m03(),
                            matrix.m10(), matrix.m11(), matrix.m12(), matrix.m13(),
                            matrix.m20(), matrix.m21(), matrix.m22(), matrix.m23(),
                            matrix.m30(), matrix.m31(), matrix.m32(), matrix.m33()
                    ))
                    .fieldOf("values")
                    .forGetter(Matrix4Uniform::value)
    ).apply(instance, Matrix4Uniform::new));

    @Override
    public void apply(String name, MutableUniformAccess access) {
        access.setMatrix(name, this.value);
    }

    @Override
    public Type type() {
        return Type.MAT4;
    }
}

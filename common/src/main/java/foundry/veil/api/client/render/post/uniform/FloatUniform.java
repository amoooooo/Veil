package foundry.veil.api.client.render.post.uniform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public record FloatUniform(float[] values) implements UniformValue {

    public static final MapCodec<FloatUniform> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.listOf(1, 4)
                    .xmap(floats -> {
                        float[] values = new float[floats.size()];
                        for (int i = 0; i < floats.size(); i++) {
                            values[i] = floats.get(i);
                        }
                        return values;
                    }, values -> {
                        List<Float> floats = new ArrayList<>(values.length);
                        for (float value : values) {
                            floats.add(value);
                        }
                        return floats;
                    })
                    .fieldOf("value")
                    .forGetter(FloatUniform::values)
    ).apply(instance, FloatUniform::new));

    @Override
    public void apply(String name, MutableUniformAccess access) {
        access.setFloats(name, this.values);
    }

    @Override
    public Type type() {
        return Type.FLOAT;
    }
}

package foundry.veil.api.client.render.post.uniform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public sealed interface UniformValue permits FloatUniform, IntUniform, Matrix2Uniform, Matrix3Uniform, Matrix4Uniform {

    Codec<UniformValue> CODEC = Type.CODEC.dispatch(UniformValue::type, Type::getCodec);

    void apply(String name, MutableUniformAccess access);

    Type type();

    enum Type {
        FLOAT(FloatUniform.CODEC),
        INT(IntUniform.CODEC),
        MAT2(Matrix2Uniform.CODEC),
        MAT3(Matrix3Uniform.CODEC),
        MAT4(Matrix4Uniform.CODEC);

        private static final Type[] VALUES = values();
        private static final String VALID_OPTIONS = Arrays.stream(VALUES).map(type -> type.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", "));
        public static final Codec<Type> CODEC = Codec.STRING.flatXmap(name -> {
            for (Type type : VALUES) {
                if (type.name().equalsIgnoreCase(name)) {
                    return DataResult.success(type);
                }
            }
            return DataResult.error(() -> "Unknown uniform type: " + name + ". Valid types: " + VALID_OPTIONS);
        }, type -> DataResult.success(type.name().toLowerCase(Locale.ROOT)));

        private final MapCodec<? extends UniformValue> codec;

        Type(MapCodec<? extends UniformValue> codec) {
            this.codec = codec;
        }

        public MapCodec<? extends UniformValue> getCodec() {
            return this.codec;
        }
    }
}

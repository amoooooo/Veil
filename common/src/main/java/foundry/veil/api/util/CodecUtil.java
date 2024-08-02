package foundry.veil.api.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.util.ExtraCodecs;
import org.joml.*;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class CodecUtil {

    public static final Codec<Vector2fc> VECTOR2F_CODEC = Codec.FLOAT.listOf()
            .flatXmap(list -> check(3, list), list -> check(2, list))
            .xmap(list -> new Vector2f(list.get(0), list.get(1)),
                    vector -> List.of(vector.x(), vector.y()));

    public static final Codec<Vector3fc> VECTOR3F_CODEC = Codec.FLOAT.listOf()
            .flatXmap(list -> check(3, list), list -> check(3, list))
            .xmap(list -> new Vector3f(list.get(0), list.get(1), list.get(2)),
                    vector -> List.of(vector.x(), vector.y(), vector.z()));

    public static final Codec<Vector4fc> VECTOR4F_CODEC = Codec.FLOAT.listOf()
            .flatXmap(list -> check(4, list), list -> check(4, list))
            .xmap(list -> new Vector4f(list.get(0), list.get(1), list.get(2), list.get(3)),
                    vector -> List.of(vector.x(), vector.y(), vector.z(), vector.w()));

    public static final Codec<Vector3dc> VECTOR3D_CODEC = Codec.DOUBLE.listOf()
            .flatXmap(list -> check(3, list), list -> check(3, list))
            .xmap(list -> new Vector3d(list.get(0), list.get(1), list.get(2)),
                    vector -> List.of(vector.x(), vector.y(), vector.z()));

    private static <T> DataResult<List<T>> check(int size, List<T> list) {
        if (list.size() != size) {
            return DataResult.error(() -> "Vector" + size + "f must have " + size + " elements!");
        }
        return DataResult.success(list);
    }

    public static <T> Codec<List<T>> singleOrList(Codec<T> codec) {
        return Codec.either(
                codec.flatComapMap(List::of,
                                l -> l.size() == 1
                                        ? DataResult.success(l.get(0))
                                        : DataResult.error(() -> "List must have exactly one element.")),
                ExtraCodecs.nonEmptyList(codec.listOf()))
                .xmap(e -> e.map(Function.identity(), Function.identity()),
                        l -> l.size() == 1 ? Either.left(l) : Either.right(l));
    }
}

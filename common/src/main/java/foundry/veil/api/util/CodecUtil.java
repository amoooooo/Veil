package foundry.veil.api.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import foundry.veil.Veil;
import foundry.veil.api.quasar.emitters.shape.EmitterShape;
import foundry.veil.api.quasar.registry.EmitterShapeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.joml.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

    /**
     * Creates a codec which can accept either resource locations like `veil:cube`
     * but also accepts legacy-style names like `CUBE` (used when things used to be
     * enums, but are now registries)
     */
    public static <T> Codec<T> registryOrLegacyCodec(Registry<T> registry) {
        Codec<T> legacyCodec = Codec.STRING
                .comapFlatMap(
                        name -> ResourceLocation.read(Veil.MODID + ":" + name.toLowerCase(Locale.ROOT)),
                        ResourceLocation::toString)
                .flatXmap(
                        loc -> Optional.ofNullable(registry.get(loc))
                                .map(DataResult::success)
                                .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + registry.key() + ": " + loc)),
                        object -> registry.getResourceKey(object)
                                .map(ResourceKey::location)
                                .map(DataResult::success)
                                .orElseGet(() -> DataResult.error(() -> "Unknown registry element in " + registry.key() + ":" + object)));

        return Codec.either(registry.byNameCodec(), legacyCodec)
                .xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);
    }
}

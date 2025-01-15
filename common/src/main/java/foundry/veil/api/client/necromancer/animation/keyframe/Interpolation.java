package foundry.veil.api.client.necromancer.animation.keyframe;

import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

// todo: support cubic interpolation w/ derivatives
public enum Interpolation {
    NEAREST_NEIGHBOR(
            (a, b, t) -> t < 0.5 ? a : b,
            (a, b, t, result) -> result.set(t < 0.5 ? a : b)
    ),
    LINEAR(
            (a, b, t) -> Mth.lerp(t, a, b),
            Quaternionfc::slerp
    );

    private final FloatInterpolator fInterpolator;
    private final QuaternionInterpolator qInterpolator;

    Interpolation(FloatInterpolator fInterpolator, QuaternionInterpolator qInterpolator) {
        this.fInterpolator = fInterpolator;
        this.qInterpolator = qInterpolator;
    }

    public float interpolate(float a, float b, float t) {
        return this.fInterpolator.interpolate(a, b, t);
    }

    public Quaternionf interpolate(Quaternionfc a, Quaternionfc b, float t, Quaternionf result) {
        this.qInterpolator.interpolate(a, b, t, result);
        return result;
    }

    public interface FloatInterpolator {
        float interpolate(float a, float b, float t);
    }
    public interface QuaternionInterpolator {
        void interpolate(Quaternionfc a, Quaternionfc b, float t, Quaternionf result);
    }
}

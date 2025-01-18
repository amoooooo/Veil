package foundry.veil.mixin.fix;

import org.joml.Matrix3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sun.misc.Unsafe;

@Mixin(targets = "org.joml.MemUtil$MemUtilUnsafe", remap = false)
public class MemUtilMixin {

    @Shadow
    @Final
    public static Unsafe UNSAFE;

    @Shadow
    @Final
    public static long Matrix3f_m00;

    /**
     * @author Ocelot
     * @reason Apply the fix from <a href="https://github.com/JOML-CI/JOML/commit/933eb412bb19ce7b6f98062e1c94449c50d92dca">933eb41</a>
     */
    @Overwrite
    public static void put3x4(Matrix3f m, long destAddr) {
        for (int i = 0; i < 3; i++) {
            UNSAFE.putLong(null, destAddr + (i << 4), UNSAFE.getLong(m, Matrix3f_m00 + 12 * i));
            UNSAFE.putFloat(null, destAddr + (i << 4) + 8, UNSAFE.getFloat(m, Matrix3f_m00 + 8 + 12 * i));
            UNSAFE.putFloat(null, destAddr + (i << 4) + 12, 0.0f);
        }
    }
}

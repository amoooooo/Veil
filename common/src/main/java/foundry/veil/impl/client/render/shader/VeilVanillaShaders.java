package foundry.veil.impl.client.render.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class VeilVanillaShaders {

    private static ShaderInstance clouds;
    private static ShaderInstance worldborder;

    public static void registerShaders(Context context) throws IOException {
        context.register(ResourceLocation.withDefaultNamespace("worldborder"), DefaultVertexFormat.POSITION_TEX, value -> worldborder = value);
    }

    public static @Nullable ShaderInstance getClouds() {
        return clouds;
    }

    public static @Nullable ShaderInstance getWorldborder() {
        return worldborder;
    }

    public static void free() {
        if (clouds != null) {
            clouds.close();
            clouds = null;
        }
        if (worldborder != null) {
            worldborder.close();
            worldborder = null;
        }
    }

    @FunctionalInterface
    public interface Context {

        void register(ResourceLocation id, VertexFormat vertexFormat, Consumer<ShaderInstance> loadCallback) throws IOException;
    }
}

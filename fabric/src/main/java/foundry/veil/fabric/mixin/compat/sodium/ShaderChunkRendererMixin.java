package foundry.veil.fabric.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.fabric.ext.ShaderChunkRendererExtension;
import foundry.veil.impl.client.render.shader.SimpleShaderProcessor;
import foundry.veil.impl.compat.SodiumShaderProcessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.caffeinemc.mods.sodium.client.gl.shader.*;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

@Mixin(ShaderChunkRenderer.class)
public abstract class ShaderChunkRendererMixin implements ShaderChunkRendererExtension {

    @Shadow
    @Final
    private Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs;

    @Shadow
    protected abstract GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options);

    @Unique
    private CompletableFuture<?> veil$sources;
    @Unique
    private Map<ShaderType, String> veil$shaderSource;

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    public void delete(CallbackInfo ci) {
        if (this.veil$sources != null) {
            this.veil$sources.cancel(false);
        }
    }

    @WrapOperation(method = "createShader", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderLoader;loadShader(Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderType;Lnet/minecraft/resources/ResourceLocation;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Lnet/caffeinemc/mods/sodium/client/gl/shader/GlShader;"), remap = false)
    public GlShader createShader(ShaderType type, ResourceLocation name, ShaderConstants constants, Operation<GlShader> original) {
        if (this.veil$shaderSource != null) {
            String source = this.veil$shaderSource.get(type);
            if (source != null) {
                return new GlShader(type, name, source);
            }
        }
        return original.call(type, name, constants);
    }

    @Override
    public void veil$recompile() {
        if (this.veil$sources != null) {
            this.veil$sources.cancel(false);
        }

        List<ChunkShaderOptions> keys = new ArrayList<>(this.programs.keySet());
        int activeBuffers = VeilRenderSystem.renderer().getDynamicBufferManger().getActiveBuffers();
        this.veil$sources = CompletableFuture.supplyAsync(() -> {
            Map<ChunkShaderOptions, Map<ShaderType, String>> sources = new HashMap<>(2);
            ResourceLocation vsh = ResourceLocation.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque.vsh");
            ResourceLocation fsh = ResourceLocation.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque.fsh");
            Map<ResourceLocation, String> shaders = Map.of(
                    vsh,
                    ShaderLoader.getShaderSource(vsh),
                    fsh,
                    ShaderLoader.getShaderSource(fsh)
            );

            SimpleShaderProcessor.setup(Minecraft.getInstance().getResourceManager(), activeBuffers, Collections.singleton(new SodiumShaderProcessor()));
            for (ChunkShaderOptions option : keys) {
                Map<ShaderType, String> map = new Object2ObjectArrayMap<>();
                for (Map.Entry<ResourceLocation, String> entry : shaders.entrySet()) {
                    ResourceLocation shader = entry.getKey();
                    String src = ShaderParser.parseShader(entry.getValue(), option.constants());
                    boolean vertex = shader.getPath().endsWith(".vsh");
                    try {
                        src = SimpleShaderProcessor.modify(null, ResourceLocation.fromNamespaceAndPath(shader.getNamespace(), "shaders/" + shader.getPath()), null, vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER, src);
                    } catch (Exception e) {
                        Veil.LOGGER.error("Failed to apply Veil shader modifiers to shader: {}", shader, e);
                    }
                    map.put(vertex ? ShaderType.VERTEX : ShaderType.FRAGMENT, src);
                }
                sources.put(option, map);
            }
            SimpleShaderProcessor.free();

            return sources;
        }, Util.backgroundExecutor()).thenAcceptAsync(sources -> {
            for (Map.Entry<ChunkShaderOptions, Map<ShaderType, String>> entry : sources.entrySet()) {
                ChunkShaderOptions options = entry.getKey();
                this.veil$shaderSource = entry.getValue();
                GlProgram<ChunkShaderInterface> old = this.programs.put(options, this.createShader("blocks/block_layer_opaque", options));
                if (old != null) {
                    old.delete();
                }
            }
            this.veil$shaderSource = null;
            Veil.LOGGER.info("Recompiled {} Sodium Shaders", sources.values().stream().mapToInt(Map::size).sum());
        }, VeilRenderSystem.renderThreadExecutor());
    }

    @Override
    public Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> veil$getPrograms() {
        return this.programs;
    }
}

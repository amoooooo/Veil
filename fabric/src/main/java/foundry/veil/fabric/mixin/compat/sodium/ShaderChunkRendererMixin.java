package foundry.veil.fabric.mixin.compat.sodium;

import com.google.common.base.Stopwatch;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.ext.sodium.ChunkShaderOptionsExtension;
import foundry.veil.fabric.ext.ShaderChunkRendererExtension;
import foundry.veil.impl.ThreadTaskScheduler;
import foundry.veil.impl.client.render.shader.processor.SodiumShaderProcessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.caffeinemc.mods.sodium.client.gl.shader.*;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;

@SuppressWarnings("ConstantValue")
@Mixin(ShaderChunkRenderer.class)
public abstract class ShaderChunkRendererMixin implements ShaderChunkRendererExtension {

    @Shadow(remap = false)
    @Final
    private Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> programs;

    @Shadow(remap = false)
    protected abstract GlProgram<ChunkShaderInterface> createShader(String path, ChunkShaderOptions options);

    @Unique
    private ThreadTaskScheduler scheduler;
    @Unique
    private Map<ShaderType, String> veil$shaderSource;
    @Unique
    private int veil$activeBuffers;

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    public void delete(CallbackInfo ci) {
        if (this.scheduler != null) {
            this.scheduler.cancel();
        }
        this.veil$activeBuffers = 0;
    }

    @Inject(method = "compileProgram", at = @At("HEAD"), remap = false)
    public void updateActiveProgram(ChunkShaderOptions options, CallbackInfoReturnable<GlProgram<ChunkShaderInterface>> cir) {
        ((ChunkShaderOptionsExtension) (Object) options).veil$setActiveBuffers(this.veil$activeBuffers);
    }

    @WrapOperation(method = "createShader", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderLoader;loadShader(Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderType;Lnet/minecraft/resources/ResourceLocation;Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderConstants;)Lnet/caffeinemc/mods/sodium/client/gl/shader/GlShader;", remap = true), require = 2, remap = false)
    private GlShader createShader(ShaderType type, ResourceLocation name, ShaderConstants constants, Operation<GlShader> original) {
        if (this.veil$shaderSource != null) {
            String source = this.veil$shaderSource.get(type);
            if (source != null) {
                return new GlShader(type, name, source);
            }
        }
        return original.call(type, name, constants);
    }

    @Unique
    private void recompile(Queue<ChunkShaderOptions> keys, int activeBuffers) {
        if (this.scheduler != null) {
            this.scheduler.cancel();
        }

        ResourceLocation vsh = ResourceLocation.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque.vsh");
        ResourceLocation fsh = ResourceLocation.fromNamespaceAndPath("sodium", "blocks/block_layer_opaque.fsh");
        Map<ResourceLocation, String> shaders = Map.of(
                vsh,
                ShaderLoader.getShaderSource(vsh),
                fsh,
                ShaderLoader.getShaderSource(fsh)
        );

        int shaderCount = keys.size();
        Stopwatch stopwatch = Stopwatch.createStarted();
        this.scheduler = new ThreadTaskScheduler("VeilSodiumShaderCompile", 1, () -> {
            ChunkShaderOptions option = keys.poll();
            if (option == null) {
                return null;
            }
            return () -> {
                Map<ShaderType, String> map = new Object2ObjectArrayMap<>();
                SodiumShaderProcessor.setup(Minecraft.getInstance().getResourceManager());
                for (Map.Entry<ResourceLocation, String> entry : shaders.entrySet()) {
                    ResourceLocation shader = entry.getKey();
                    String src = ShaderParser.parseShader(entry.getValue(), option.constants());
                    boolean vertex = shader.getPath().endsWith(".vsh");
                    try {
                        src = SodiumShaderProcessor.modify(shader.withPrefix("shaders/"), activeBuffers, vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER, src);
                    } catch (Exception e) {
                        Veil.LOGGER.error("Failed to apply Veil shader modifiers to shader: {}", shader, e);
                    }
                    map.put(vertex ? ShaderType.VERTEX : ShaderType.FRAGMENT, src);
                }
                SodiumShaderProcessor.free();

                RenderSystem.recordRenderCall(() -> {
                    this.veil$shaderSource = map;
                    GlProgram<ChunkShaderInterface> old = this.programs.put(option, this.createShader("blocks/block_layer_opaque", option));
                    if (old != null) {
                        old.delete();
                    }
                    this.veil$shaderSource = null;
                });
            };
        });
        this.scheduler.getCompletedFuture().thenRunAsync(() -> {
            this.veil$shaderSource = null;
            this.veil$activeBuffers = activeBuffers;
            if (!this.scheduler.isCancelled()) {
                Veil.LOGGER.info("Compiled {} Sodium Shaders in {}", shaderCount, stopwatch.stop());
            }
        }, VeilRenderSystem.renderThreadExecutor());
    }

    @SuppressWarnings("RedundantOperationOnEmptyContainer")
    @Unique
    private Queue<ChunkShaderOptions> getActiveKeys(int activeBuffers) {
        Queue<ChunkShaderOptions> keys = new ConcurrentLinkedDeque<>();
        for (ChunkShaderOptions key : this.programs.keySet()) {
            boolean unique = true;
            for (ChunkShaderOptions options : keys) {
                if (options.fog() == key.fog() && options.pass() == key.pass() && options.vertexType() == key.vertexType()) {
                    unique = false;
                    break;
                }
            }
            if (unique) {
                ChunkShaderOptions options = new ChunkShaderOptions(key.fog(), key.pass(), key.vertexType());
                ((ChunkShaderOptionsExtension) (Object) options).veil$setActiveBuffers(activeBuffers);
                keys.add(options);
            }
        }
        return keys;
    }

    @Override
    public void veil$recompile() {
        this.recompile(this.getActiveKeys(this.veil$activeBuffers), this.veil$activeBuffers);
    }

    @Override
    public void veil$setActiveBuffers(int activeBuffers) {
        if (this.veil$activeBuffers == activeBuffers) {
            return;
        }

        if (this.scheduler != null) {
            this.scheduler.cancel();
        }

        Queue<ChunkShaderOptions> keys = this.getActiveKeys(activeBuffers);
        keys.removeIf(this.programs::containsKey);
        if (keys.isEmpty()) {
            return;
        }

        this.recompile(keys, activeBuffers);
    }

    @Override
    public Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> veil$getPrograms() {
        return this.programs;
    }
}

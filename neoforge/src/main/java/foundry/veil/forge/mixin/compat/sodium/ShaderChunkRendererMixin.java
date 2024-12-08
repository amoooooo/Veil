package foundry.veil.forge.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.forge.ext.ShaderChunkRendererExtension;
import foundry.veil.impl.ThreadTaskScheduler;
import foundry.veil.impl.client.render.shader.SimpleShaderProcessor;
import foundry.veil.impl.compat.SodiumShaderProcessor;
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

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

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
    private ThreadTaskScheduler scheduler;
    @Unique
    private Map<ShaderType, String> veil$shaderSource;

    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    public void delete(CallbackInfo ci) {
        if (this.scheduler != null) {
            this.scheduler.cancel();
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
    public void veil$recompile(int activeBuffers) {
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

        Queue<ChunkShaderOptions> keys = new ConcurrentLinkedDeque<>(this.programs.keySet());
        int shaderCount = keys.size();
        this.scheduler = new ThreadTaskScheduler("VeilSodiumShaderCompile", 1, () -> {
            ChunkShaderOptions option = keys.poll();
            if (option == null) {
                return null;
            }
            return () -> {
                Map<ShaderType, String> map = new Object2ObjectArrayMap<>();
                for (Map.Entry<ResourceLocation, String> entry : shaders.entrySet()) {
                    ResourceLocation shader = entry.getKey();
                    String src = ShaderParser.parseShader(entry.getValue(), option.constants());
                    boolean vertex = shader.getPath().endsWith(".vsh");
                    try {
                        SimpleShaderProcessor.setup(Minecraft.getInstance().getResourceManager(), activeBuffers, Collections.singleton(new SodiumShaderProcessor()));
                        src = SimpleShaderProcessor.modify(null, ResourceLocation.fromNamespaceAndPath(shader.getNamespace(), "shaders/" + shader.getPath()), null, vertex ? GL_VERTEX_SHADER : GL_FRAGMENT_SHADER, src);
                        SimpleShaderProcessor.free();
                    } catch (Exception e) {
                        Veil.LOGGER.error("Failed to apply Veil shader modifiers to shader: {}", shader, e);
                    }
                    map.put(vertex ? ShaderType.VERTEX : ShaderType.FRAGMENT, src);
                }
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
            if (!this.scheduler.isCancelled()) {
                Veil.LOGGER.info("Recompiled {} Sodium Shaders", shaderCount);
            }
        }, VeilRenderSystem.renderThreadExecutor());
    }

    @Override
    public Map<ChunkShaderOptions, GlProgram<ChunkShaderInterface>> veil$getPrograms() {
        return this.programs;
    }
}

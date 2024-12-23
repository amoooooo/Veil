package foundry.veil.impl.client.render.shader.program;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.program.TextureUniformAccess;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;

@ApiStatus.Internal
public class ShaderTextureCache {

    private final ShaderProgram program;
    private final Object2IntMap<CharSequence> textures;
    private final Object2IntMap<CharSequence> boundSamplers;
    private final ObjectSet<TextureUniformAccess.SamplerListener> listeners;
    private boolean dirty;
    private IntBuffer bindings;

    public ShaderTextureCache(ShaderProgram program) {
        this.program = program;
        this.textures = new Object2IntArrayMap<>();
        this.textures.defaultReturnValue(-1);
        this.boundSamplers = new Object2IntArrayMap<>();
        this.listeners = new ObjectArraySet<>();
        this.bindings = null;
    }

    private void uploadTextures(int start, BiConsumer<Integer, Integer> textureConsumer) {
        this.boundSamplers.clear();

        int maxSampler = VeilRenderSystem.maxCombinedTextureUnits();
        int count = 1;
        textureConsumer.accept(start, MissingTextureAtlasSprite.getTexture().getId());

        for (Object2IntMap.Entry<CharSequence> entry : this.textures.object2IntEntrySet()) {
            CharSequence name = entry.getKey();

            // If there are too many samplers, then refer back to the missing texture
            int sampler = start + count;
            if (sampler >= maxSampler) {
                this.program.setInt(name, 0);
                Veil.LOGGER.error("Too many samplers were bound for shader (max {}): {}", maxSampler, this.program.getId());
                break;
            }

            // If the texture is "missing", then refer back to the bound missing texture
            int textureId = entry.getIntValue();
            if (textureId == 0) {
                this.program.setInt(name, 0);
                continue;
            }

            textureConsumer.accept(sampler, textureId);
            this.program.setInt(name, sampler);
            this.boundSamplers.put(name, sampler);
            count++;
        }

        Object2IntMap<CharSequence> view = Object2IntMaps.unmodifiable(this.boundSamplers);
        for (TextureUniformAccess.SamplerListener listener : this.listeners) {
            listener.onUpdateSamplers(view);
        }
    }

    public void bind(int start) {
        if (this.textures.isEmpty()) {
            return;
        }

        if (VeilRenderSystem.textureMultibindSupported()) {
            if (this.dirty) {
                this.dirty = false;

                // Not enough space, so realloc
                if (this.bindings == null || this.bindings.capacity() < 1 + this.textures.size()) {
                    this.bindings = MemoryUtil.memRealloc(this.bindings, 1 + this.textures.size());
                }

                this.bindings.clear();
                this.uploadTextures(start, (sampler, id) -> this.bindings.put(id));
                this.bindings.flip();

                // Delete texture buffer if there aren't any textures
                if (this.bindings.limit() == 0) {
                    MemoryUtil.memFree(this.bindings);
                    this.bindings = null;
                }
            }
            if (this.bindings != null && this.bindings.limit() > 0) {
                VeilRenderSystem.bindTextures(start, this.bindings);
            }
            return;
        }

        // Ignored for normal binding
        this.dirty = false;

        int activeTexture = GlStateManager._getActiveTexture();
        this.uploadTextures(start, (sampler, id) -> {
            RenderSystem.activeTexture(GL_TEXTURE0 + sampler);
            if (sampler >= 12) {
                glBindTexture(GL_TEXTURE_2D, id);
            } else {
                RenderSystem.bindTexture(id);
            }
        });
        RenderSystem.activeTexture(activeTexture);
    }

    public void addSamplerListener(TextureUniformAccess.SamplerListener listener) {
        this.listeners.add(listener);
    }

    public void removeSamplerListener(TextureUniformAccess.SamplerListener listener) {
        this.listeners.remove(listener);
    }

    public void put(CharSequence name, int textureId) {
        this.dirty |= this.textures.put(name, textureId) != textureId;
    }

    public void remove(CharSequence name) {
        if (this.textures.removeInt(name) != 0) {
            this.dirty = true;
        }
    }

    public void clear() {
        this.textures.clear();
        this.boundSamplers.clear();
        if (this.bindings != null) {
            MemoryUtil.memFree(this.bindings);
            this.bindings = null;
        }
        this.dirty = true;
    }
}

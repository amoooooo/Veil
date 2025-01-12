package foundry.veil.impl.client.render.shader.program;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.program.TextureUniformAccess;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.Map;

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
        this.boundSamplers = new Object2IntArrayMap<>();
        this.listeners = new ObjectArraySet<>();
        this.bindings = null;
    }

    private void uploadTextures(ShaderUniformCache cache, int samplerStart) {
        this.boundSamplers.clear();

        int maxSampler = VeilRenderSystem.maxCombinedTextureUnits();
        int count = 0;
        int missingTexture = MissingTextureAtlasSprite.getTexture().getId();
        boolean hasMissing = false;
        String last = null;

        for (Map.Entry<String, ShaderUniformCache.Uniform> entry : cache.getSamplers().entrySet()) {
            String name = entry.getKey();

            // If the texture is "missing", then refer back to the bound missing texture and remove
            int textureId = this.textures.getInt(name);
            if (textureId == 0 || textureId == missingTexture) {
                if (!hasMissing) {
                    hasMissing = true;
                    long address = MemoryUtil.memAddress0(this.bindings);
                    int position = this.bindings.position();
                    MemoryUtil.memCopy(address, address + 1, position);
                    this.bindings.position(position + 1);
                    this.bindings.put(0, MissingTextureAtlasSprite.getTexture().getId());
                }
                this.program.setInt(name, 0);
                this.textures.removeInt(name);
                continue;
            }

            // If there are too many samplers, then delete the latest texture and refer back to the missing texture
            int sampler = samplerStart + count;
            if (sampler >= maxSampler) {
                if (!hasMissing) {
                    hasMissing = true;
                    long address = MemoryUtil.memAddress0(this.bindings);
                    int position = this.bindings.position();
                    MemoryUtil.memCopy(address, address + 1, position - 1);
                    this.bindings.put(0, MissingTextureAtlasSprite.getTexture().getId());

                    // Delete the last texture binding
                    if (last != null) {
                        this.program.setInt(last, 0);
                        this.boundSamplers.removeInt(last);
                    }
                }
                this.program.setInt(name, 0);
            } else {
                this.program.setInt(name, sampler);
                this.bindings.put(textureId);
                this.boundSamplers.put(name, sampler);
            }

            count++;
            last = name;
        }

        if (samplerStart + count >= maxSampler) {
            Veil.LOGGER.error("Too many samplers were bound for shader (max {}): {}", maxSampler, this.program.getName());
        }

        Object2IntMap<CharSequence> view = Object2IntMaps.unmodifiable(this.boundSamplers);
        for (TextureUniformAccess.SamplerListener listener : this.listeners) {
            listener.onUpdateSamplers(view);
        }
    }

    public void bind(ShaderUniformCache cache, int samplerStart) {
        if (this.textures.isEmpty()) {
            return;
        }

        if (this.dirty) {
            this.dirty = false;

            // Not enough space, so realloc
            if (this.bindings == null || this.bindings.capacity() < 1 + this.textures.size()) {
                this.bindings = MemoryUtil.memRealloc(this.bindings, 1 + this.textures.size());
            }

            this.bindings.clear();
            this.uploadTextures(cache, samplerStart);
            this.bindings.flip();

            // Delete texture buffer if there aren't any textures
            if (this.bindings.limit() == 0) {
                MemoryUtil.memFree(this.bindings);
                this.bindings = null;
            }
        }
        if (this.bindings != null && this.bindings.limit() > 0) {
            VeilRenderSystem.bindTextures(samplerStart, this.bindings);
        }
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
            this.textures.put(name, 0);
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

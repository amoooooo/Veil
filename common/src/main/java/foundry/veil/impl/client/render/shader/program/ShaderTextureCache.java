package foundry.veil.impl.client.render.shader.program;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.program.ShaderUniformCache;
import foundry.veil.api.client.render.shader.program.TextureUniformAccess;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
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
    private final Int2IntMap samplers;
    private final Object2IntMap<CharSequence> boundSamplers;
    private final ObjectSet<TextureUniformAccess.SamplerListener> listeners;

    private boolean textureDirty;
    private boolean samplerDirty;
    private IntBuffer textureBindings;
    private IntBuffer samplerBindings;

    public ShaderTextureCache(ShaderProgram program) {
        this.program = program;
        this.textures = new Object2IntArrayMap<>();
        this.samplers = new Int2IntArrayMap();
        this.boundSamplers = new Object2IntArrayMap<>();
        this.listeners = new ObjectArraySet<>();

        this.textureBindings = null;
        this.samplerBindings = null;
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
                    long address = MemoryUtil.memAddress0(this.textureBindings);
                    int position = this.textureBindings.position();
                    MemoryUtil.memCopy(address, address + Integer.BYTES, position);
                    this.textureBindings.position(position + 1);
                    this.textureBindings.put(0, MissingTextureAtlasSprite.getTexture().getId());

                    // Increment all existing samplers
                    for (CharSequence boundSampler : this.boundSamplers.keySet()) {
                        this.boundSamplers.computeInt(boundSampler, (unused, i) -> i + 1);
                    }
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
                    long address = MemoryUtil.memAddress0(this.textureBindings);
                    int position = this.textureBindings.position();
                    MemoryUtil.memCopy(address, address + Integer.BYTES, position - 1);
                    this.textureBindings.put(0, MissingTextureAtlasSprite.getTexture().getId());

                    // Delete the last texture binding
                    if (last != null) {
                        this.boundSamplers.removeInt(last);
                    }

                    // Increment all existing samplers
                    for (CharSequence boundSampler : this.boundSamplers.keySet()) {
                        this.boundSamplers.computeInt(boundSampler, (unused, i) -> i + 1);
                    }
                }
                this.program.setInt(name, 0);
            } else {
                this.textureBindings.put(textureId);
                this.boundSamplers.put(name, sampler);
            }

            count++;
            last = name;
        }

        for (Object2IntMap.Entry<CharSequence> entry : this.boundSamplers.object2IntEntrySet()) {
            this.program.setInt(entry.getKey(), entry.getIntValue());
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

        if (this.textureDirty) {
            this.textureDirty = false;
            // If textures change, then samplers must also change
            this.samplerDirty = true;

            // Not enough space, so realloc
            if (this.textureBindings == null || this.textureBindings.capacity() < 1 + this.textures.size()) {
                this.textureBindings = MemoryUtil.memRealloc(this.textureBindings, 1 + this.textures.size());
            }

            this.textureBindings.clear();
            this.uploadTextures(cache, samplerStart);
            this.textureBindings.flip();

            // Delete texture and sampler buffers if there aren't any textures
            if (this.textureBindings.limit() == 0) {
                MemoryUtil.memFree(this.textureBindings);
                MemoryUtil.memFree(this.samplerBindings);
                this.textureBindings = null;
                this.samplerBindings = null;
            }
        }

        if (this.textureBindings != null && this.textureBindings.limit() > 0) {
            VeilRenderSystem.bindTextures(samplerStart, this.textureBindings);

            if (this.samplerDirty) {
                this.samplerDirty = false;

                // Not enough space, so realloc
                if (this.samplerBindings == null || this.samplerBindings.capacity() < this.textureBindings.limit()) {
                    this.samplerBindings = MemoryUtil.memRealloc(this.samplerBindings, this.textureBindings.limit());
                }

                // Unbound sampler bindings refer back to the innate binding of the texture, so this is fine
                this.samplerBindings.limit(this.textureBindings.limit());
                for (int i = 0; i < this.textureBindings.limit(); i++) {
                    this.samplerBindings.put(i, this.samplers.get(this.textureBindings.get(i)));
                }
            }

            VeilRenderSystem.bindSamplers(samplerStart, this.samplerBindings);
        }
    }

    public void addSamplerListener(TextureUniformAccess.SamplerListener listener) {
        this.listeners.add(listener);
    }

    public void removeSamplerListener(TextureUniformAccess.SamplerListener listener) {
        this.listeners.remove(listener);
    }

    public void put(CharSequence name, int textureId, int samplerId) {
        this.textureDirty |= this.textures.put(name, textureId) != textureId;
        if (samplerId != 0) {
            this.samplerDirty |= this.samplers.put(textureId, samplerId) != samplerId;
        } else {
            this.samplerDirty |= this.samplers.remove(textureId) != samplerId;
        }
    }

    public void remove(CharSequence name) {
        int oldId = this.textures.removeInt(name);
        if (oldId != 0) {
            this.textures.put(name, 0);
            this.textureDirty = true;
            if (this.samplers.remove(oldId) != 0) {
                this.samplerDirty = true;
            }
        }
    }

    public void clear() {
        this.textures.clear();
        this.samplers.clear();
        this.boundSamplers.clear();
        if (this.textureBindings != null) {
            MemoryUtil.memFree(this.textureBindings);
            this.textureBindings = null;
        }
        if (this.samplerBindings != null) {
            MemoryUtil.memFree(this.samplerBindings);
            this.samplerBindings = null;
        }
        this.textureDirty = true;
        this.samplerDirty = true;
    }
}

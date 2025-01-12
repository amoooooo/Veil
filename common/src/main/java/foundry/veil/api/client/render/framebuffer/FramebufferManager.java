package foundry.veil.api.client.render.framebuffer;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import foundry.veil.Veil;
import foundry.veil.api.CodecReloadListener;
import foundry.veil.api.client.render.ext.VeilDebug;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.NativeResource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;

/**
 * <p>Manages all framebuffers and custom definitions specified in files.
 * All framebuffers except for the main one can be customized from the
 * <code>modid:pinwheel/framebuffers</code> folder in the assets.</p>
 *
 * @author Ocelot
 */
public class FramebufferManager extends CodecReloadListener<FramebufferDefinition> implements NativeResource {

    private static final ResourceLocation MAIN = ResourceLocation.withDefaultNamespace("main");

    public static final Codec<ResourceLocation> FRAMEBUFFER_CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            if (!name.contains(":")) {
                ResourceLocation id = ResourceLocation.tryBuild("temp", name);
                return id != null ? DataResult.success(id) : DataResult.error(() -> "Invalid path: " + name);
            }

            ResourceLocation id = ResourceLocation.tryParse(name);
            return id != null ? DataResult.success(id) : DataResult.error(() -> "Invalid path: " + name);
        } catch (ResourceLocationException e) {
            return DataResult.error(() -> "Not a valid resource location: " + name + ". " + e.getMessage());
        }
    }, location -> "temp".equals(location.getNamespace()) ? location.getPath() : location.toString()).stable();
    public static final FileToIdConverter FRAMEBUFFER_LISTER = FileToIdConverter.json("pinwheel/framebuffers");

    private final Map<ResourceLocation, FramebufferDefinition> framebufferDefinitions;
    private final Map<ResourceLocation, AdvancedFbo> framebuffers;
    private final Map<ResourceLocation, AdvancedFbo> framebuffersView;
    private final Set<ResourceLocation> screenFramebuffers;
    private final Set<ResourceLocation> manualFramebuffers;

    /**
     * Creates a new instance of the framebuffer manager.
     */
    public FramebufferManager() {
        super(FramebufferDefinition.CODEC, FRAMEBUFFER_LISTER);
        this.framebufferDefinitions = new HashMap<>();
        this.framebuffers = new Object2ObjectArrayMap<>();
        this.framebuffersView = Collections.unmodifiableMap(this.framebuffers);
        this.screenFramebuffers = new ObjectArraySet<>();
        this.manualFramebuffers = new ObjectArraySet<>();
    }

    private void initFramebuffer(ResourceLocation name, FramebufferDefinition definition, MolangEnvironment runtime) {
        try {
            AdvancedFbo fbo = definition.createBuilder(runtime).build(true);
            fbo.clear();
            AdvancedFbo old = this.framebuffers.put(name, fbo);
            VeilDebug.get().objectLabel(GL_FRAMEBUFFER, fbo.getId(), "Framebuffer " + name);
            if (old != null) {
                old.free();
            }
            if (!definition.autoClear()) {
                this.manualFramebuffers.add(name);
            } else {
                this.manualFramebuffers.remove(name);
            }
        } catch (Exception e) {
            Veil.LOGGER.error("Failed to initialize framebuffer: {}", name, e);
        }
    }

    @ApiStatus.Internal
    public void resizeFramebuffers(int width, int height) {
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("screen_width", width)
                .setQuery("screen_height", height)
                .create();

        for (ResourceLocation name : this.screenFramebuffers) {
            FramebufferDefinition definition = this.framebufferDefinitions.get(name);
            if (definition != null) {
                this.initFramebuffer(name, definition, runtime);
            }
        }
    }

    @ApiStatus.Internal
    public void clear() {
        this.framebuffers.forEach((name, fbo) -> {
            if (!this.manualFramebuffers.contains(name)) {
                fbo.clear();
            }
        });
    }

    /**
     * Updates a single framebuffer definition and initializes a new framebuffer.
     *
     * @param name       The name of the framebuffer to set
     * @param definition The new definition
     */
    public void setDefinition(ResourceLocation name, FramebufferDefinition definition) {
        Window window = Minecraft.getInstance().getWindow();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("screen_width", window.getWidth())
                .setQuery("screen_height", window.getHeight())
                .create();

        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.framebufferDefinitions.put(name, definition);
        this.initFramebuffer(name, definition, runtime);
        if (!definition.width().isConstant() || !definition.height().isConstant()) {
            this.screenFramebuffers.add(name);
        } else {
            this.screenFramebuffers.remove(name);
        }
        AdvancedFbo.unbind();
    }

    /**
     * Sets the value of the specified framebuffer to a manually defined buffer.
     * Old buffers defined using the data-driven API will be overwritten and deleted.
     *
     * @param name The name of the framebuffer to add
     * @param fbo  The framebuffer to add
     */
    public void setFramebuffer(ResourceLocation name, AdvancedFbo fbo) {
        if (this.manualFramebuffers.add(name)) {
            AdvancedFbo oldBuffer = this.framebuffers.remove(name);
            if (oldBuffer != null) {
                oldBuffer.free();
                Veil.LOGGER.warn("Replaced defined framebuffer {} with manual buffer", name);
            }
        }

        this.framebuffers.put(name, fbo);
        VeilDebug.get().objectLabel(GL_FRAMEBUFFER, fbo.getId(), "Framebuffer " + name);
    }

    /**
     * Removes the specified manual framebuffer without freeing it.
     *
     * @param name The name of the framebuffer to remove
     * @return The framebuffer previously defined or <code>null</code> if there was no manual buffer defined
     */
    public @Nullable AdvancedFbo removeFramebuffer(ResourceLocation name) {
        if (this.manualFramebuffers.remove(name)) {
            AdvancedFbo old = this.framebuffers.remove(name);
            if (old != null) {
                VeilDebug.get().objectLabel(GL_FRAMEBUFFER, old.getId(), null);
            }
            return old;
        }
        return null;
    }

    /**
     * Retrieves a framebuffer by the specified name.
     *
     * @param name The name of the framebuffer to retrieve
     * @return The framebuffer by that name
     */
    public @Nullable AdvancedFbo getFramebuffer(ResourceLocation name) {
        return this.framebuffers.get(name);
    }

    /**
     * Retrieves a framebuffer definition by the specified name.
     *
     * @param name The name of the definition to retrieve
     * @return The definition by that name
     */
    public @Nullable FramebufferDefinition getFramebufferDefinition(ResourceLocation name) {
        return this.framebufferDefinitions.get(name);
    }

    /**
     * @return An immutable view of all custom framebuffers loaded
     */
    public Map<ResourceLocation, AdvancedFbo> getFramebuffers() {
        return this.framebuffersView;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, FramebufferDefinition> data, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        this.framebufferDefinitions.clear();
        this.framebufferDefinitions.putAll(data);
        Veil.LOGGER.info("Loaded {} framebuffers", this.framebufferDefinitions.size());

        this.free();
        Window window = Minecraft.getInstance().getWindow();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("screen_width", window.getWidth())
                .setQuery("screen_height", window.getHeight())
                .create();

        this.framebufferDefinitions.forEach((name, definition) -> {
            this.initFramebuffer(name, definition, runtime);
            if (!definition.width().isConstant() || !definition.height().isConstant()) {
                this.screenFramebuffers.add(name);
            }
        });

        this.setFramebuffer(MAIN, AdvancedFbo.getMainFramebuffer());
    }

    @Override
    public void free() {
        this.framebuffers.keySet().removeAll(this.manualFramebuffers);
        this.framebuffers.values().forEach(AdvancedFbo::free);
        this.framebuffers.clear();
        this.manualFramebuffers.clear();
        this.screenFramebuffers.clear();
    }
}

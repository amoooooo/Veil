package foundry.veil.api.client.editor;

import foundry.veil.Veil;
import foundry.veil.VeilClient;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.api.resource.editor.ResourceFileEditor;
import foundry.veil.api.util.CompositeReloadListener;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * <p>Manages all editors for Veil. Editors are ImGui powered panels that can be dynamically registered and unregistered with {@link #add(Inspector)}.</p>
 *
 * @author Ocelot
 */
public class EditorManager implements VeilEditorEnvironment, PreparableReloadListener {

    public static final ResourceLocation DEFAULT_FONT = Veil.veilPath("jetbrains_mono");

    private final Map<Inspector, ImBoolean> editors;
    private final Map<ResourceLocation, ResourceFileEditor<?>> resourceFileEditors;
    private final ImGuiFontManager fonts;
    private boolean enabled;

    @ApiStatus.Internal
    public EditorManager(ReloadableResourceManager resourceManager) {
        this.editors = new TreeMap<>(Comparator.comparing(inspector -> inspector.getClass().getSimpleName()));
        this.resourceFileEditors = new Object2ObjectArrayMap<>();
        this.fonts = new ImGuiFontManager();
        this.enabled = false;

        resourceManager.registerReloadListener(this);
    }

    public ImFont getFont(ResourceLocation name, boolean bold, boolean italic) {
        return this.fonts.getFont(name, bold, italic);
    }

    public ImFont getFont(boolean bold, boolean italic) {
        return this.getFont(DEFAULT_FONT, bold, italic);
    }

    @ApiStatus.Internal
    public void render() {
        if (!this.enabled) {
            return;
        }

        if (ImGui.beginMainMenuBar()) {
            ImFont font = ImGui.getFont();
            float dingleWidth = font.calcTextSizeAX(ImGui.getFontSize(), Float.MAX_VALUE, 0, " Veil ") + 1;
            float dingleHeight = ImGui.getTextLineHeightWithSpacing() + 6;
            ImGui.getWindowDrawList().addRectFilled(0f, 0f, dingleWidth, dingleHeight, ImGui.getColorU32(ImGuiCol.FrameBgHovered));
            ImGui.text("Veil ");

            for (Map.Entry<Inspector, ImBoolean> entry : this.editors.entrySet()) {
                Inspector inspector = entry.getKey();
                Component group = inspector.getGroup();
                if (group == null) {
                    if (Veil.platform().isDevelopmentEnvironment()) {
                        Veil.LOGGER.error("Editor '{}' should return Editor#DEFAULT_GROUP instead of null", inspector.getClass());
                    }
                    group = Inspector.DEFAULT_GROUP;
                }
                if (ImGui.beginMenu(group.getString())) {
                    ImBoolean enabled = entry.getValue();

                    ImGui.beginDisabled(!inspector.isEnabled());
                    if (ImGui.menuItem(inspector.getDisplayName().getString(), null, enabled.get())) {
                        if (!enabled.get()) {
                            this.show(inspector);
                        } else {
                            this.hide(inspector);
                        }
                    }
                    ImGui.endDisabled();
                    ImGui.endMenu();
                }
            }

            for (Map.Entry<Inspector, ImBoolean> entry : this.editors.entrySet()) {
                Inspector inspector = entry.getKey();
                if (entry.getValue().get() && inspector.isMenuBarEnabled()) {
                    ImGui.separator();
                    ImGui.textColored(0xFFAAAAAA, inspector.getDisplayName().getString());
                    inspector.renderMenuBar();
                }
            }

            ImGui.endMainMenuBar();
        }

        for (Map.Entry<Inspector, ImBoolean> entry : this.editors.entrySet()) {
            Inspector inspector = entry.getKey();
            ImBoolean enabled = entry.getValue();

            if (!inspector.isEnabled()) {
                enabled.set(false);
            }
            if (!enabled.get()) {
                continue;
            }

            inspector.render();
        }

        Iterator<ResourceFileEditor<?>> iterator = this.resourceFileEditors.values().iterator();
        while (iterator.hasNext()) {
            ResourceFileEditor<?> next = iterator.next();
            if (next.isClosed()) {
                next.close();
                iterator.remove();
                continue;
            }

            next.render();
        }
    }

    @ApiStatus.Internal
    public void renderLast() {
        if (!this.enabled) {
            return;
        }

        for (Map.Entry<Inspector, ImBoolean> entry : this.editors.entrySet()) {
            Inspector inspector = entry.getKey();
            ImBoolean enabled = entry.getValue();
            if (enabled.get()) {
                inspector.renderLast();
            }
        }
    }

    @ApiStatus.Internal
    public void onFileChange(VeilResource<?> resource) {
        ResourceFileEditor<?> editor = this.resourceFileEditors.get(resource.resourceInfo().location());
        if (editor != null) {
            editor.loadFromDisk();
        }
    }

    public void show(Inspector inspector) {
        ImBoolean enabled = this.editors.get(inspector);
        if (enabled != null && !enabled.get()) {
            inspector.onShow();
            enabled.set(true);
        }
    }

    public void hide(Inspector inspector) {
        ImBoolean enabled = this.editors.get(inspector);
        if (enabled != null && enabled.get()) {
            inspector.onHide();
            enabled.set(false);
        }
    }

    public boolean isVisible(Inspector inspector) {
        ImBoolean visible = this.editors.get(inspector);
        return visible != null && visible.get();
    }

    public synchronized void add(Inspector inspector) {
        this.editors.computeIfAbsent(inspector, unused -> new ImBoolean());
    }

    public synchronized void remove(Inspector inspector) {
        this.hide(inspector);
        this.editors.remove(inspector);
    }

    /**
     * Toggles visibility of the ImGui overlay.
     */
    public void toggle() {
        this.enabled = !this.enabled;
    }

    /**
     * @return Whether the overlay is active
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether the overlay should be active.
     *
     * @param enabled Whether to enable the ImGui overlay
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(@NotNull PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller prepareProfiler, @NotNull ProfilerFiller applyProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
        List<PreparableReloadListener> listeners = new ArrayList<>(this.editors.size());
        listeners.add(this.fonts);
        for (Inspector inspector : this.editors.keySet()) {
            if (inspector instanceof PreparableReloadListener listener) {
                listeners.add(listener);
            }
        }
        for (ResourceFileEditor<?> editor : this.resourceFileEditors.values()) {
            if (editor instanceof PreparableReloadListener listener) {
                listeners.add(listener);
            }
        }
        PreparableReloadListener listener = CompositeReloadListener.of(listeners.toArray(PreparableReloadListener[]::new));
        return listener.reload(preparationBarrier, resourceManager, prepareProfiler, applyProfiler, backgroundExecutor, gameExecutor);
    }

    @Override
    public <T extends VeilResource<?>> void open(T resource, ResourceFileEditor.Factory<T> editor) {
        try {
            ResourceFileEditor<T> open = editor.open(this, resource);
            if (open != null) {
                this.resourceFileEditors.put(resource.resourceInfo().location(), open);
            }
        } catch (Throwable t) {
            Veil.LOGGER.error("Failed to open editor for resource: {}", resource.resourceInfo().location(), t);
        }
    }

    @Override
    public VeilResourceManager getResourceManager() {
        return VeilClient.resourceManager();
    }
}

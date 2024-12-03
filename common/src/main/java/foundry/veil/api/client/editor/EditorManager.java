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
 * <p>Manages all editors for Veil. Editors are ImGui powered panels that can be dynamically registered and unregistered with {@link #add(Editor)}.</p>
 *
 * @author Ocelot
 */
public class EditorManager implements VeilEditorEnvironment, PreparableReloadListener {

    public static final ResourceLocation DEFAULT_FONT = Veil.veilPath("jetbrains_mono");

    private final Map<Editor, ImBoolean> editors;
    private final Map<ResourceLocation, ResourceFileEditor<?>> resourceFileEditors;
    private final EditorFontManager fonts;
    private boolean enabled;

    @ApiStatus.Internal
    public EditorManager(ReloadableResourceManager resourceManager) {
        this.editors = new TreeMap<>(Comparator.comparing(editor -> editor.getClass().getSimpleName()));
        this.resourceFileEditors = new Object2ObjectArrayMap<>();
        this.fonts = new EditorFontManager();
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
            float dingleWidth = font.calcTextSizeAX(ImGui.getFontSize(), Float.MAX_VALUE, 0, " Veil ") + 4;
            float dingleHeight = ImGui.getTextLineHeightWithSpacing() + 2;
            ImGui.getWindowDrawList().addRectFilled(0f, 0f, dingleWidth, dingleHeight, ImGui.getColorU32(ImGuiCol.FrameBgHovered));
            ImGui.text("Veil ");

            for (Map.Entry<Editor, ImBoolean> entry : this.editors.entrySet()) {
                Editor editor = entry.getKey();
                Component group = editor.getGroup();
                if (group == null) {
                    if (Veil.platform().isDevelopmentEnvironment()) {
                        Veil.LOGGER.error("Editor '{}' should return Editor#DEFAULT_GROUP instead of null", editor.getClass());
                    }
                    group = Editor.DEFAULT_GROUP;
                }
                if (ImGui.beginMenu(group.getString())) {
                    ImBoolean enabled = entry.getValue();

                    ImGui.beginDisabled(!editor.isEnabled());
                    if (ImGui.menuItem(editor.getDisplayName().getString(), null, enabled.get())) {
                        if (!enabled.get()) {
                            this.show(editor);
                        } else {
                            this.hide(editor);
                        }
                    }
                    ImGui.endDisabled();
                    ImGui.endMenu();
                }
            }

            for (Map.Entry<Editor, ImBoolean> entry : this.editors.entrySet()) {
                Editor editor = entry.getKey();
                if (entry.getValue().get() && editor.isMenuBarEnabled()) {
                    ImGui.separator();
                    ImGui.textColored(0xFFAAAAAA, editor.getDisplayName().getString());
                    editor.renderMenuBar();
                }
            }

            ImGui.endMainMenuBar();
        }

        for (Map.Entry<Editor, ImBoolean> entry : this.editors.entrySet()) {
            Editor editor = entry.getKey();
            ImBoolean enabled = entry.getValue();

            if (!editor.isEnabled()) {
                enabled.set(false);
            }
            if (!enabled.get()) {
                continue;
            }

            editor.render();
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

        for (Map.Entry<Editor, ImBoolean> entry : this.editors.entrySet()) {
            Editor editor = entry.getKey();
            ImBoolean enabled = entry.getValue();
            if (enabled.get()) {
                editor.renderLast();
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

    public void show(Editor editor) {
        ImBoolean enabled = this.editors.get(editor);
        if (enabled != null && !enabled.get()) {
            editor.onShow();
            enabled.set(true);
        }
    }

    public void hide(Editor editor) {
        ImBoolean enabled = this.editors.get(editor);
        if (enabled != null && enabled.get()) {
            editor.onHide();
            enabled.set(false);
        }
    }

    public boolean isVisible(Editor editor) {
        ImBoolean visible = this.editors.get(editor);
        return visible != null && visible.get();
    }

    public synchronized void add(Editor editor) {
        this.editors.computeIfAbsent(editor, unused -> new ImBoolean());
    }

    public synchronized void remove(Editor editor) {
        this.hide(editor);
        this.editors.remove(editor);
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
        for (Editor editor : this.editors.keySet()) {
            if (editor instanceof PreparableReloadListener listener) {
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

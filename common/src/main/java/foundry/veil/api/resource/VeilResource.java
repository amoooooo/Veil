package foundry.veil.api.resource;

import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public interface VeilResource<T extends VeilResource<?>> {

    /**
     * Rebders this resource into the resource panel.
     *
     * @param dragging Whether the user is dragging the resource
     */
    default void render(boolean dragging) {
        VeilImGuiUtil.icon(this.getIconCode());
        ImGui.sameLine();

        VeilResourceInfo resourceInfo = this.resourceInfo();
        ImGui.pushStyleColor(ImGuiCol.Text, resourceInfo.isStatic() ? 0xFFAAAAAA : 0xFFFFFFFF);
        if (dragging) {
            VeilImGuiUtil.resourceLocation(resourceInfo.location());
        } else {
            ImGui.text(resourceInfo.fileName());
        }
        ImGui.popStyleColor();
    }

    /**
     * Called from the watcher thread when this resource updates on disc.
     *
     * @param resourceManager The resource manager this event was triggered from
     * @param event           The event received from the file watcher
     * @return A future for when the key can be reset. All events are ignored until this future completes
     */
    default CompletableFuture<?> onFileSystemChange(VeilResourceManager resourceManager, WatchEvent<Path> event) {
        if (this.canHotReload() && (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY)) {
            Veil.LOGGER.info("Hot swapping {} after file system change", this.resourceInfo().location());

            Minecraft client = Minecraft.getInstance();
            return CompletableFuture.runAsync(() -> {
                try {
                    this.copyToResources();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, task -> client.tell(() -> Util.ioPool().execute(task))).thenRunAsync(() -> {
                try {
                    this.hotReload(resourceManager);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, client).exceptionally(e -> {
                while (e instanceof CompletionException) {
                    e = e.getCause();
                }
                Veil.LOGGER.error("Failed to hot swap file system change", e);
                return null;
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    VeilResourceInfo resourceInfo();

    /**
     * @return All actions that can be performed on this resource
     */
    List<VeilResourceAction<T>> getActions();

    /**
     * @return If this resource can be hot-reloaded
     */
    boolean canHotReload();

    /**
     * Hot-reloads the resource.
     *
     * @param resourceManager The resource manager reloading the resource
     * @throws IOException If any error occurs while trying to reload
     */
    void hotReload(VeilResourceManager resourceManager) throws IOException;

    default void copyToResources() throws IOException {
        VeilResourceInfo info = this.resourceInfo();
        Path filePath = info.filePath();
        if (filePath == null) {
            return;
        }

        Path modPath = info.modResourcePath();
        if (modPath == null) {
            return;
        }

        try (InputStream is = Files.newInputStream(modPath); OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.TRUNCATE_EXISTING)) {
            IOUtils.copyLarge(is, os);
        }
    }

    /**
     * Gets the icon code for this resource (ex. 0xED0F)
     */
    int getIconCode();
}

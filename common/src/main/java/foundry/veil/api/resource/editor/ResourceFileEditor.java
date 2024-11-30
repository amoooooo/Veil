package foundry.veil.api.resource.editor;

import foundry.veil.Veil;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public interface ResourceFileEditor<T extends VeilResource<?>> {

    /**
     * Renders this editor to the screen.
     */
    void render();

    /**
     * Opens the specified resource in the environment.
     *
     * @param environment The environment to open the resource in
     * @param resource    The resource to open
     */
    void open(VeilEditorEnvironment environment, T resource);

    /**
     * Saves the specified data to the specified resource and hot reloads it.
     *
     * @param data            The data to write to the resource file
     * @param resourceManager The resource manager to write the file to
     * @param resource        The resource to write to
     */
    default void save(byte[] data, VeilResourceManager resourceManager, VeilResource<?> resource) {
        VeilResourceInfo info = resource.resourceInfo();
        CompletableFuture.runAsync(() -> {
            try {
                if (info.isStatic()) {
                    throw new IOException("Read-only resource");
                }

                Path path = info.filePath();
                try (OutputStream os = Files.newOutputStream(path)) {
                    os.write(data);
                }

                Path modPath = info.modResourcePath();
                if (modPath == null) {
                    return;
                }

                // Copy from build to resources
                try (InputStream is = Files.newInputStream(path); OutputStream os = Files.newOutputStream(modPath, StandardOpenOption.TRUNCATE_EXISTING)) {
                    IOUtils.copyLarge(is, os);
                }
            } catch (Exception e) {
                Veil.LOGGER.error("Failed to write resource: {}", info.location(), e);
            }
        }, Util.ioPool()).thenRunAsync(() -> {
            try {
                resource.hotReload(resourceManager);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Minecraft.getInstance()).exceptionally(e -> {
            while (e instanceof CompletionException) {
                e = e.getCause();
            }
            Veil.LOGGER.error("Failed to hot-swap resource: {}", info.location(), e);
            return null;
        });
    }
}

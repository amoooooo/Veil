package foundry.veil.mixin.debug;

import foundry.veil.Veil;
import foundry.veil.platform.VeilPlatform;
import net.minecraft.Util;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Main.class)
public class RenderDocker {

    @Unique
    private static final int ENABLE_TIME = 4000;

    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void preMain(String[] pArgs, CallbackInfo ci) {
        VeilPlatform platform = Veil.platform();
        if (!platform.isDevelopmentEnvironment() || platform.isModLoaded("tracky")) {
            return;
        }

        String path = System.getProperty("java.library.path");
        String name = System.mapLibraryName("renderdoc");
        boolean detected = false;
        for (String folder : path.split(Util.getPlatform() == Util.OS.WINDOWS ? ";" : ":")) {
            if (Files.exists(Path.of(folder + "/" + name))) {
                detected = true;
                break;
            }
        }

        if (!detected) {
            return;
        }

        AtomicBoolean enable = new AtomicBoolean();
        Thread td = new Thread(() -> {
            Veil.LOGGER.warn("Renderdoc detected, would you like to load it? y/N");

            long start = System.currentTimeMillis();
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (System.currentTimeMillis() - start <= ENABLE_TIME) {
                try {
                    if (reader.ready()) {
                        String ln = reader.readLine().trim().toLowerCase(Locale.ROOT);
                        if (ln.startsWith("y")) {
                            enable.set(true);
                            return;
                        } else if (ln.startsWith("n")) {
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, "Veil-RenderDocker");

        td.setDaemon(true);
        td.start();

        try {
            // We just need to wait for the thread to stop
            td.join();

            if (enable.get()) {
                System.loadLibrary("renderdoc");
                Veil.LOGGER.info("Renderdoc Loaded");
            }
        } catch (Throwable e) {
            Veil.LOGGER.info("Failed to load Renderdoc", e);
        }
    }
}
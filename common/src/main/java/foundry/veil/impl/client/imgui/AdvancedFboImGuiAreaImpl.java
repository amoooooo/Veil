package foundry.veil.impl.client.imgui;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@ApiStatus.Internal
public class AdvancedFboImGuiAreaImpl {

    private static final List<AdvancedFbo> FBO_STACK = new ArrayList<>();
    private static int pointer;

    public static AdvancedFbo allocate(int width, int height) {
        if (pointer >= FBO_STACK.size()) {
            AdvancedFbo fbo = AdvancedFbo.withSize(width, height)
                    .addColorTextureBuffer()
                    .setDepthRenderBuffer()
                    .build(true);
            FBO_STACK.add(fbo);
            pointer++;
            return fbo;
        }

        AdvancedFbo fbo = FBO_STACK.get(pointer);
        if (fbo.getWidth() != width || fbo.getHeight() != height) {
            fbo.free();
            fbo = AdvancedFbo.withSize(width, height)
                    .addColorTextureBuffer()
                    .setDepthRenderBuffer()
                    .build(true);
            FBO_STACK.set(pointer, fbo);
        }

        pointer++;
        return fbo;
    }

    public static void begin() {
        pointer = 0;
    }

    public static void end() {
        ListIterator<AdvancedFbo> iterator = FBO_STACK.listIterator(pointer);
        while (iterator.hasNext()) {
            AdvancedFbo next = iterator.next();
            next.free();
            iterator.remove();
        }
    }
}

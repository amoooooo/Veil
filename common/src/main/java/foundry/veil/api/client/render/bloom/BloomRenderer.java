package foundry.veil.api.client.render.bloom;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;

import java.io.Closeable;

public abstract class BloomRenderer implements Closeable {
    public abstract void initialize(int width, int height);

    public abstract void apply(AdvancedFbo framebuffer);

    public abstract void close();
}

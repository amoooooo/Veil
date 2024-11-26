package foundry.veil.api.client.render.light;

import foundry.veil.api.client.render.light.renderer.InstancedLightRenderer;

import java.nio.ByteBuffer;

/**
 * A light that can be rendered with an implementation of {@link InstancedLightRenderer}.
 *
 * @author Ocelot
 */
public interface InstancedLight {

    /**
     * Stores the data of this light into the specified buffer.
     *
     * @param buffer The buffer to fill
     */
    void store(ByteBuffer buffer);
}

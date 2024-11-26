package foundry.veil.api.client.render.light;

import foundry.veil.api.client.render.light.renderer.InstancedLightRenderer;

/**
 * A light that can be rendered with an implementation of {@link InstancedLightRenderer}.
 *
 * @author Ocelot
 */
public interface IndirectLight<T extends PositionedLight<T>> extends PositionedLight<T>, InstancedLight {

    /**
     * @return The maximum distance the light can travel
     */
    float getRadius();

    /**
     * Sets the maximum radius the light can influence.
     *
     * @param radius The maximum area of influence for the light
     */
    T setRadius(float radius);
}

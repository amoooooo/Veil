package foundry.veil.api.client.render.deferred.light.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.deferred.light.Light;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.NativeResource;

import java.util.List;
import java.util.Set;

/**
 * Renders all lights of a specific type.
 *
 * @param <T> The type of lights to render
 */
public interface LightTypeRenderer<T extends Light> extends NativeResource {

    /**
     * Prepares the specified lights to be rendered.
     *
     * @param lightRenderer The light renderer instance
     * @param lights        The lights to render
     * @param removedLights The lights that will be removed this frame
     * @param frustum       The culling view frustum
     */
    @ApiStatus.OverrideOnly
    void prepareLights(LightRenderer lightRenderer, List<T> lights, Set<T> removedLights, CullFrustum frustum);

    /**
     * <p>Renders all prepared lights with this renderer.</p>
     * <p>Shaders, custom uniforms, and the way lights are rendered is up to the individual renderer.</p>
     *
     * @param lightRenderer The light renderer instance
     * @param lights        The lights to render
     */
    @ApiStatus.OverrideOnly
    void renderLights(LightRenderer lightRenderer, List<T> lights);

    /**
     * @return The number of lights visible last frame
     */
    int getVisibleLights();

    /**
     * Draws a unit quad into the specified buffer
     *
     * @param builder The builder to draw into
     */
    static void createQuad(VertexConsumer builder) {
        builder.addVertex(-1, -1, 0);
        builder.addVertex(1, -1, 0);
        builder.addVertex(-1, 1, 0);
        builder.addVertex(1, 1, 0);
    }

    /**
     * Draws a unit inverted cube into the specified buffer
     *
     * @param builder The builder to draw into
     */
    static void createInvertedCube(VertexConsumer builder) {
        builder.addVertex(-1, 1, 1); // Front-top-left
        builder.addVertex(1, 1, 1); // Front-top-right
        builder.addVertex(-1, -1, 1); // Front-bottom-left
        builder.addVertex(1, -1, 1); // Front-bottom-right
        builder.addVertex(1, -1, -1); // Back-bottom-right
        builder.addVertex(1, 1, 1); // Front-top-right
        builder.addVertex(1, 1, -1); // Back-top-right
        builder.addVertex(-1, 1, 1); // Front-top-left
        builder.addVertex(-1, 1, -1); // Back-top-left
        builder.addVertex(-1, -1, 1); // Front-bottom-left
        builder.addVertex(-1, -1, -1); // Back-bottom-left
        builder.addVertex(1, -1, -1); // Back-bottom-right
        builder.addVertex(-1, 1, -1); // Back-top-left
        builder.addVertex(1, 1, -1); // Back-top-right
    }
}

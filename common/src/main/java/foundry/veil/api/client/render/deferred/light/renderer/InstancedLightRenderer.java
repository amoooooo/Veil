package foundry.veil.api.client.render.deferred.light.renderer;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.light.InstancedLight;
import foundry.veil.api.client.render.deferred.light.Light;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

/**
 * Draws lights as instanced quads in the scene.
 *
 * @param <T> The type of lights to render
 * @author Ocelot
 */
public abstract class InstancedLightRenderer<T extends Light & InstancedLight> implements LightTypeRenderer<T> {

    private static final int MAX_UPLOADS = 400;

    protected final int lightSize;
    protected int maxLights;

    private final List<T> visibleLights;
    private final VertexBuffer vbo;
    private final int instancedVbo;

    /**
     * Creates a new instanced light renderer with a resizeable light buffer.
     *
     * @param lightSize The size of each light in bytes
     */
    public InstancedLightRenderer(int lightSize) {
        this.lightSize = lightSize;
        this.maxLights = 100;
        this.visibleLights = new ArrayList<>();
        this.vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.instancedVbo = glGenBuffers();

        this.vbo.bind();
        this.vbo.upload(this.createMesh());

        glBindBuffer(GL_ARRAY_BUFFER, this.instancedVbo);
        glBufferData(GL_ARRAY_BUFFER, (long) this.maxLights * this.lightSize, GL_DYNAMIC_DRAW);
        this.setupBufferState();
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        VertexBuffer.unbind();
    }

    /**
     * @return The mesh data each instanced light will be rendered with use
     */
    protected abstract MeshData createMesh();

    /**
     * Sets up the instanced buffer state.
     */
    protected abstract void setupBufferState();

    /**
     * Sets up the render state for drawing all lights.
     *
     * @param lightRenderer The renderer instance
     * @param lights        All lights in the order they are in the instanced buffer
     */
    protected abstract void setupRenderState(LightRenderer lightRenderer, List<T> lights);

    /**
     * Clears the render state after drawing all lights.
     *
     * @param lightRenderer The renderer instance
     * @param lights        All lights in the order they are in the instanced buffer
     */
    protected abstract void clearRenderState(LightRenderer lightRenderer, List<T> lights);

    /**
     * Checks whether the specified light can be seen in the specified frustum.
     *
     * @param light   The light to check
     * @param frustum The frustum to check visibility with
     * @return Whether that light is visible
     */
    protected abstract boolean isVisible(T light, CullFrustum frustum);

    private void updateAllLights(List<T> lights) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int pointer = 0;
            long offset = 0;
            ByteBuffer dataBuffer = stack.malloc(Math.min(MAX_UPLOADS, lights.size()) * this.lightSize);
            for (T light : lights) {
                light.clean();
                dataBuffer.position((pointer++) * this.lightSize);
                light.store(dataBuffer);
                if (pointer >= MAX_UPLOADS) {
                    dataBuffer.rewind();
                    glBufferSubData(GL_ARRAY_BUFFER, offset, dataBuffer);
                    offset += dataBuffer.capacity();
                    pointer = 0;
                }
            }

            if (pointer > 0) {
                dataBuffer.rewind();
                nglBufferSubData(GL_ARRAY_BUFFER, offset, (long) pointer * this.lightSize, memAddress(dataBuffer));
            }
        }
    }

    @Override
    public void prepareLights(LightRenderer lightRenderer, List<T> lights, Set<T> removedLights, CullFrustum frustum) {
        this.visibleLights.clear();
        for (T light : lights) {
            if (this.isVisible(light, frustum)) {
                this.visibleLights.add(light);
            }
        }

        if (this.visibleLights.isEmpty()) {
            return;
        }

        glBindBuffer(GL_ARRAY_BUFFER, this.instancedVbo);

        // If there is no space, then resize
        boolean rebuild = false;
        if (this.visibleLights.size() > this.maxLights) {
            rebuild = true;
            this.maxLights = (int) Math.max(Math.ceil(this.maxLights / 2.0), this.visibleLights.size() * 1.5);
            glBufferData(GL_ARRAY_BUFFER, (long) this.maxLights * this.lightSize, GL_STREAM_DRAW);
        }

        if (rebuild || !removedLights.isEmpty()) {
            this.updateAllLights(this.visibleLights);
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer buffer = stack.malloc(this.lightSize);
                for (int i = 0; i < this.visibleLights.size(); i++) {
                    T light = this.visibleLights.get(i);
                    if (light.isDirty()) {
                        light.clean();
                        light.store(buffer);
                        buffer.rewind();
                        glBufferSubData(GL_ARRAY_BUFFER, (long) i * this.lightSize, buffer);
                    }
                }
            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void renderLights(LightRenderer lightRenderer, List<T> lights) {
        if (this.visibleLights.isEmpty()) {
            return;
        }

        this.vbo.bind();
        this.setupRenderState(lightRenderer, this.visibleLights);
        lightRenderer.applyShader();
        VeilRenderSystem.drawInstanced(this.vbo, this.visibleLights.size());
        this.clearRenderState(lightRenderer, this.visibleLights);
        VertexBuffer.unbind();
    }

    @Override
    public int getVisibleLights() {
        return this.visibleLights.size();
    }

    @Override
    public void free() {
        this.vbo.close();
        glDeleteBuffers(this.instancedVbo);
    }
}

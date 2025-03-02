package foundry.veil.api.client.render.light.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.Veil;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.IndirectLight;
import foundry.veil.api.client.render.light.Light;
import foundry.veil.api.client.render.shader.block.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.block.ShaderBlock;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3dc;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL30C.glBindBufferRange;
import static org.lwjgl.opengl.GL40C.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL42C.*;
import static org.lwjgl.opengl.GL43C.glDispatchCompute;
import static org.lwjgl.opengl.GL45C.glNamedBufferData;

/**
 * Draws lights as indirect instanced quads in the scene.
 *
 * @param <T> The type of lights to render
 * @author Ocelot
 */
public abstract class IndirectLightRenderer<T extends Light & IndirectLight<T>> implements LightTypeRenderer<T> {

    private static final ResourceLocation CULL_SHADER = Veil.veilPath("light/indirect_sphere");
    private static final int MIN_LIGHTS = 20;

    protected final int lightSize;
    protected final int highResSize;
    protected final int lowResSize;
    protected final int positionOffset;
    protected final int rangeOffset;
    protected int maxLights;

    private final VertexArray vertexArray;
    private final int instancedVbo;
    private final int indirectVbo;
    private final int sizeVbo;
    private final DynamicShaderBlock<?> instancedBlock;
    private final DynamicShaderBlock<?> indirectBlock;

    private int visibleLights;

    /**
     * Creates a new instanced light renderer with a resizeable light buffer.
     *
     * @param lightSize  The size of each light in bytes
     * @param lowResSize The size of the low-resolution mesh or <code>0</code> to only use the high-detail mesh
     */
    public IndirectLightRenderer(int lightSize, int lowResSize, int positionOffset, int rangeOffset) {
        if (!VeilRenderSystem.multiDrawIndirectSupported()) {
            throw new IllegalStateException("Indirect light renderer is not supported");
        }

        this.lightSize = lightSize;
        this.maxLights = MIN_LIGHTS;
        this.vertexArray = VertexArray.create();
        this.instancedVbo = this.vertexArray.getOrCreateBuffer(2);
        this.indirectVbo = this.vertexArray.getOrCreateBuffer(3);

        // TODO fix compute shader
        if (false && VeilRenderSystem.computeSupported() && VeilRenderSystem.atomicCounterSupported()) {
            Veil.LOGGER.info("Using GPU Frustum Culling for {} renderer", this.getClass().getSimpleName());
            this.sizeVbo = this.vertexArray.getOrCreateBuffer(4);
            this.instancedBlock = ShaderBlock.wrapper(ShaderBlock.BufferBinding.SHADER_STORAGE, this.instancedVbo);
            this.indirectBlock = ShaderBlock.wrapper(ShaderBlock.BufferBinding.SHADER_STORAGE, this.indirectVbo);

            if (VeilRenderSystem.directStateAccessSupported()) {
                glNamedBufferData(this.sizeVbo, Integer.BYTES, GL_DYNAMIC_DRAW);
            } else {
                RenderSystem.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, this.sizeVbo);
                glBufferData(GL_ATOMIC_COUNTER_BUFFER, Integer.BYTES, GL_DYNAMIC_DRAW);
                RenderSystem.glBindBuffer(GL_ATOMIC_COUNTER_BUFFER, 0);
            }
        } else {
            Veil.LOGGER.info("Using CPU Frustum Culling for {} renderer", this.getClass().getSimpleName());
            this.sizeVbo = 0;
            this.instancedBlock = null;
            this.indirectBlock = null;
        }

        MeshData mesh = this.createMesh();
        this.vertexArray.upload(mesh, VertexArray.DrawUsage.STATIC);

        this.highResSize = this.vertexArray.getIndexCount() - lowResSize;
        this.lowResSize = lowResSize;
        this.positionOffset = positionOffset;
        this.rangeOffset = rangeOffset;

        // Initialize data buffers
        this.initBuffers();

        VertexArrayBuilder builder = this.vertexArray.editFormat();
        builder.defineVertexBuffer(2, this.instancedVbo, 0, this.lightSize, 1);
        this.setupBufferState(builder); // Only set up state for instanced buffer

        VertexBuffer.unbind();
    }

    /**
     * @return The mesh data each instanced light will be rendered with use
     */
    protected abstract MeshData createMesh();

    /**
     * Sets up the instanced buffer state.
     */
    protected abstract void setupBufferState(VertexArrayBuilder builder);

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

    private void initBuffers() {
        if (VeilRenderSystem.directStateAccessSupported()) {
            glNamedBufferData(this.instancedVbo, (long) this.maxLights * this.lightSize, GL_DYNAMIC_DRAW);
            glNamedBufferData(this.indirectVbo, (long) this.maxLights * Integer.BYTES * 5, GL_DYNAMIC_DRAW);
        } else {
            RenderSystem.glBindBuffer(GL_ARRAY_BUFFER, this.instancedVbo);
            RenderSystem.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.indirectVbo);
            glBufferData(GL_ARRAY_BUFFER, (long) this.maxLights * this.lightSize, GL_DYNAMIC_DRAW);
            glBufferData(GL_DRAW_INDIRECT_BUFFER, (long) this.maxLights * Integer.BYTES * 5, GL_DYNAMIC_DRAW);
        }
        if (this.sizeVbo != 0) {
            this.instancedBlock.setSize((long) this.maxLights * this.lightSize);
            this.indirectBlock.setSize((long) this.maxLights * Integer.BYTES * 5);
        }
    }

    private boolean shouldDrawHighResolution(T light, CullFrustum frustum) {
        float radius = light.getRadius();
        return frustum.getPosition().distanceSquared(light.getPosition()) <= radius * radius;
    }

    private boolean isVisible(T light, CullFrustum frustum) {
        Vector3dc position = light.getPosition();
        float radius = light.getRadius();
        return frustum.testSphere(position, radius * 1.414F);
    }

    private void updateAllLights(List<T> lights) {
        ByteBuffer dataBuffer = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, (long) lights.size() * this.lightSize, null);
        if (dataBuffer == null) {
            return;
        }

        for (int i = 0; i < lights.size(); i++) {
            T light = lights.get(i);
            light.clean();
            dataBuffer.position(i * this.lightSize);
            light.store(dataBuffer);
        }
        glUnmapBuffer(GL_ARRAY_BUFFER);
    }

    private int updateVisibility(List<T> lights, CullFrustum frustum) {
        if (this.sizeVbo != 0) {
            VeilRenderSystem.setShader(CULL_SHADER);
            ShaderProgram shader = VeilRenderSystem.getShader();
            if (shader != null && shader.isCompute()) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    VeilRenderSystem.bind("VeilLightInstanced", this.instancedBlock);
                    VeilRenderSystem.bind("VeilLightIndirect", this.indirectBlock);

                    glBindBufferRange(GL_ATOMIC_COUNTER_BUFFER, 0, this.sizeVbo, 0, Integer.BYTES);
                    glBufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, stack.callocInt(1));

                    int maxX = VeilRenderSystem.maxComputeWorkGroupCountX();
                    int maxY = VeilRenderSystem.maxComputeWorkGroupCountY();

                    shader.setInt("HighResSize", this.highResSize);
                    shader.setInt("LowResSize", this.lowResSize);
                    shader.setInt("LightSize", this.lightSize / Float.BYTES);
                    shader.setInt("PositionOffset", this.positionOffset);
                    shader.setInt("RangeOffset", this.rangeOffset);

                    Vector4fc[] planes = frustum.getPlanes();
                    float[] values = new float[4 * planes.length];
                    for (int i = 0; i < planes.length; i++) {
                        Vector4fc plane = planes[i];
                        values[i * 4] = plane.x();
                        values[i * 4 + 1] = plane.y();
                        values[i * 4 + 2] = plane.z();
                        values[i * 4 + 3] = plane.w();
                    }
                    shader.setFloats("FrustumPlanes", values);
                    shader.setInt("Width", maxX);

                    shader.bind();

                    glDispatchCompute(Math.min(lights.size(), maxX), Math.min(1 + lights.size() / maxX, maxY), 1);
                    glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT | GL_ATOMIC_COUNTER_BARRIER_BIT);

                    ShaderProgram.unbind();

                    ByteBuffer counter = glMapBufferRange(GL_ATOMIC_COUNTER_BUFFER, 0, Integer.BYTES, GL_MAP_READ_BIT);
                    return counter != null ? counter.getInt(0) : 0;
                } finally {
                    VeilRenderSystem.unbind(this.instancedBlock);
                    VeilRenderSystem.unbind(this.indirectBlock);
                    glUnmapBuffer(GL_ATOMIC_COUNTER_BUFFER);
                    glBindBufferRange(GL_ATOMIC_COUNTER_BUFFER, 0, 0, 0, Integer.BYTES);
                }
            }
        }

        int count = 0;
        RenderSystem.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.indirectVbo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(this.lowResSize > 0 ? Integer.BYTES * 5 : Integer.BYTES);

            int index = 0;
            for (T light : lights) {
                if (this.isVisible(light, frustum)) {
                    if (this.lowResSize > 0) {
                        boolean highRes = this.shouldDrawHighResolution(light, frustum);
                        buffer.putInt(0, highRes ? this.highResSize : this.lowResSize);
                        buffer.putInt(4, 1);
                        buffer.putInt(8, !highRes ? this.highResSize : 0);
                        buffer.putInt(12, 0);
                        buffer.putInt(16, index);
                        glBufferSubData(GL_DRAW_INDIRECT_BUFFER, count * Integer.BYTES * 5L, buffer);
                    } else {
                        buffer.putInt(0, index);
                        glBufferSubData(GL_DRAW_INDIRECT_BUFFER, count * Integer.BYTES * 5L + 16, buffer);
                    }
                    count++;
                }
                index++;
            }
        }
        return count;
    }

    @Override
    public void prepareLights(LightRenderer lightRenderer, List<T> lights, Set<T> removedLights, CullFrustum frustum) {
        ProfilerFiller profiler = Minecraft.getInstance().getProfiler();
        RenderSystem.glBindBuffer(GL_ARRAY_BUFFER, this.instancedVbo);

        profiler.push("resize");

        // If there is no space, then resize
        boolean rebuild = false;
        if (lights.size() > this.maxLights) {
            rebuild = true;
            this.maxLights = (int) Math.max(Math.max(Math.ceil(this.maxLights / 2.0), MIN_LIGHTS), lights.size() * 1.5);
            this.initBuffers();
        }
        profiler.popPush("update");

        // The instanced buffer needs to be updated
        RenderSystem.glBindBuffer(GL_ARRAY_BUFFER, this.instancedVbo);
        if (rebuild || !removedLights.isEmpty()) {
            this.updateAllLights(lights);
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer buffer = stack.malloc(this.lightSize);
                for (int i = 0; i < lights.size(); i++) {
                    T light = lights.get(i);
                    if (light.isDirty()) {
                        light.clean();
                        light.store(buffer);
                        buffer.rewind();
                        glBufferSubData(GL_ARRAY_BUFFER, (long) i * this.lightSize, buffer);
                    }
                }
            }
        }

        profiler.popPush("visibility");

        // Fill indirect buffer draw calls
        this.visibleLights = !lights.isEmpty() ? this.updateVisibility(lights, frustum) : 0;

        profiler.pop();
    }

    @Override
    public void renderLights(LightRenderer lightRenderer, List<T> lights) {
        this.setupRenderState(lightRenderer, lights);
        if (lightRenderer.applyShader()) {
            this.clearRenderState(lightRenderer, lights);
            return;
        }

        this.vertexArray.bind();
        RenderSystem.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.indirectVbo);
        this.vertexArray.drawIndirect(0L, this.visibleLights, 0);
        RenderSystem.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        VertexBuffer.unbind();

        ShaderProgram.unbind();
        this.clearRenderState(lightRenderer, lights);
    }

    @Override
    public int getVisibleLights() {
        return this.visibleLights;
    }

    @Override
    public void free() {
        this.vertexArray.free();
        if (this.sizeVbo != 0) {
            this.instancedBlock.free();
            this.indirectBlock.free();
        }
    }
}

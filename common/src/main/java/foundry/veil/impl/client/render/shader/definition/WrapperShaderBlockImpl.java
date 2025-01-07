package foundry.veil.impl.client.render.shader.definition;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.opengl.GL30C.glBindBufferRange;

@ApiStatus.Internal
public class WrapperShaderBlockImpl extends ShaderBlockImpl<Object> implements DynamicShaderBlock<Object> {

    private long size;

    public WrapperShaderBlockImpl(BufferBinding binding, int buffer) {
        super(binding);
        this.buffer = buffer;
    }

    @Override
    public void bind(int index) {
        int binding = this.binding.getGlType();
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(binding), index);
        glBindBufferRange(binding, index, this.buffer, 0, this.size);
    }

    @Override
    public void unbind(int index) {
        int binding = this.binding.getGlType();
        Validate.inclusiveBetween(0, VeilRenderSystem.maxTargetBindings(binding), index);
        glBindBufferRange(binding, index, 0, 0, this.size);
    }

    @Override
    public void set(@Nullable Object value) {
        throw new UnsupportedOperationException("Wrapper Shader Block cannot be set to a java object");
    }

    @Override
    public @Nullable Object getValue() {
        throw new UnsupportedOperationException("Wrapper Shader Block cannot be read as a java object");
    }

    @Override
    public void setSize(long newSize) {
        this.size = newSize;
    }

    @Override
    public void free() {
        VeilRenderSystem.unbind(this);
    }
}

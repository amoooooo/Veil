package foundry.veil.impl.client.render;

import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilShaderBufferLayout;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.impl.client.render.shader.definition.LayoutShaderBlockImpl;
import foundry.veil.impl.client.render.shader.definition.SizedShaderBlockImpl;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.ARBProgramInterfaceQuery.*;
import static org.lwjgl.opengl.GL31C.*;

@ApiStatus.Internal
public record LayoutSerializer<T>(Field<T>[] fields) implements BiConsumer<T, ByteBuffer> {

    @SuppressWarnings("unchecked")
    public static <T> LayoutShaderBlockImpl<T> create(VeilShaderBufferLayout<T> layout, ShaderProgram shader, String name, int blockIndex) {
        int program = shader.getProgram();
        ShaderBlock.BufferBinding binding = layout.binding();

        if (VeilRenderSystem.programInterfaceQuerySupported()) {
            int bufferInterface = binding == ShaderBlock.BufferBinding.UNIFORM ? GL_UNIFORM_BLOCK : GL_SHADER_STORAGE_BLOCK;
            int fieldInterface = binding == ShaderBlock.BufferBinding.UNIFORM ? GL_UNIFORM : GL_BUFFER_VARIABLE;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer fieldProperties = stack.ints(GL_OFFSET);
                IntBuffer values = stack.mallocInt(1);
                glGetProgramResourceiv(program, bufferInterface, blockIndex, stack.ints(GL_BUFFER_DATA_SIZE), null, values);

                int size = values.get(0);
                List<Field<T>> layoutFields = new ArrayList<>();
                for (Map.Entry<String, VeilShaderBufferLayout.FieldSerializer<T>> entry : layout.fields().entrySet()) {
                    String fieldName = name + "." + entry.getKey();
                    int index = glGetProgramResourceIndex(program, fieldInterface, fieldName);
                    if (index == -1) {
                        Veil.LOGGER.warn("Failed to find buffer field '{}' in shader: {}", fieldName, shader.getId());
                        continue;
                    }

                    glGetProgramResourceiv(program, fieldInterface, index, fieldProperties, null, values);
                    layoutFields.add(new Field<>(values.get(0), entry.getValue()));
                }

                layoutFields.sort(Comparator.comparingInt(Field::offset));
                LayoutSerializer<T> serializer = new LayoutSerializer<>(layoutFields.toArray(Field[]::new));
                return new LayoutShaderBlockImpl<>(binding, size, VeilRenderSystem.directStateAccessSupported() ? new SizedShaderBlockImpl.DSASerializer<>(serializer) : new SizedShaderBlockImpl.LegacySerializer<>(serializer));
            }
        }

        List<Field<T>> layoutFields = new ArrayList<>();
        for (Map.Entry<String, VeilShaderBufferLayout.FieldSerializer<T>> entry : layout.fields().entrySet()) {
            String fieldName = name + "." + entry.getKey();
            int index = glGetUniformIndices(program, fieldName);
            if (index == -1) {
                Veil.LOGGER.warn("Failed to find buffer field '{}' in shader: {}", fieldName, shader.getId());
                continue;
            }

            int offset = glGetActiveUniformsi(program, index, GL_UNIFORM_OFFSET);
            if (offset == -1) {
                Veil.LOGGER.warn("Buffer field '{}' in shader '{}' it not in a uniform block", fieldName, shader.getId());
                continue;
            }

            layoutFields.add(new Field<>(offset, entry.getValue()));
        }

        int size = glGetActiveUniformBlocki(program, blockIndex, GL_UNIFORM_BLOCK_DATA_SIZE);

        layoutFields.sort(Comparator.comparingInt(Field::offset));
        LayoutSerializer<T> serializer = new LayoutSerializer<>(layoutFields.toArray(Field[]::new));
        return new LayoutShaderBlockImpl<>(binding, size, VeilRenderSystem.directStateAccessSupported() ? new SizedShaderBlockImpl.DSASerializer<>(serializer) : new SizedShaderBlockImpl.LegacySerializer<>(serializer));
    }

    @Override
    public void accept(T value, ByteBuffer buffer) {
        for (Field<T> field : this.fields) {
            field.serializer.write(value, field.offset, buffer);
        }
    }

    private record Field<T>(int offset, VeilShaderBufferLayout.FieldSerializer<T> serializer) {
    }
}

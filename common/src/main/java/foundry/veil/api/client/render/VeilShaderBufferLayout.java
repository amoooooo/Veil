package foundry.veil.api.client.render;

import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import foundry.veil.api.glsl.grammar.*;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.variable.GlslNewNode;
import foundry.veil.api.glsl.node.variable.GlslStructNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record VeilShaderBufferLayout<T>(String name,
                                        @Nullable String interfaceName,
                                        Map<String, FieldSerializer<T>> fields,
                                        ShaderBlock.BufferBinding requestedBinding,
                                        ShaderBlock.MemoryLayout memoryLayout,
                                        GlslStructSpecifier structSpecifier) {

    /**
     * Creates a GLSL node representation of this layout.
     *
     * @param shaderStorageSupported Whether shader storage blocks are supported and can be attempted
     * @return A node for
     */
    public GlslNode createNode(boolean shaderStorageSupported) {
        GlslTypeQualifier.StorageType storageType = switch (this.requestedBinding) {
            case UNIFORM -> GlslTypeQualifier.StorageType.UNIFORM;
            case SHADER_STORAGE ->
                    shaderStorageSupported ? GlslTypeQualifier.StorageType.BUFFER : GlslTypeQualifier.StorageType.UNIFORM;
        };

        GlslSpecifiedType structSpecifier = new GlslSpecifiedType(this.structSpecifier, GlslTypeQualifier.layout(this.memoryLayout.getLayoutId()), storageType);
        GlslNode node;
        if (this.interfaceName != null) {
            node = new GlslNewNode(structSpecifier, this.interfaceName, null);
        } else {
            node = new GlslStructNode(structSpecifier);
        }
        return node;
    }

    public ShaderBlock.BufferBinding binding() {
        return VeilRenderSystem.shaderStorageBufferSupported() ? this.requestedBinding : ShaderBlock.BufferBinding.UNIFORM;
    }

    public static <T> Builder<T> builder(String name) {
        return new Builder<>(name);
    }

    @FunctionalInterface
    public interface FieldSerializer<T> {

        void write(T value, int index, ByteBuffer buffer);
    }

    public static class Builder<T> {

        private final String name;
        private final List<GlslStructField> structFields;
        private final Map<String, FieldSerializer<T>> fields;
        private ShaderBlock.BufferBinding binding;
        private ShaderBlock.MemoryLayout memoryLayout;
        private String interfaceName;

        public Builder(String name) {
            this.name = name;
            this.structFields = new ArrayList<>();
            this.fields = new Object2ObjectArrayMap<>();
            this.binding = ShaderBlock.BufferBinding.UNIFORM;
            this.memoryLayout = ShaderBlock.MemoryLayout.SHARED;
            this.interfaceName = null;
        }

        public Builder<T> binding(ShaderBlock.BufferBinding binding) {
            this.binding = binding;
            return this;
        }

        public Builder<T> memoryLayout(ShaderBlock.MemoryLayout memoryLayout) {
            this.memoryLayout = memoryLayout;
            return this;
        }

        public Builder<T> interfaceName(@Nullable String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        public Builder<T> floating(String name, FloatSerializer<T> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.FLOAT, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putFloat(index, serializer.serialize(value)));
            return this;
        }

        public Builder<T> vec2(String name, Function<T, Vector2fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec2(String name, FloatSerializer<T> x, FloatSerializer<T> y) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC2, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putFloat(index, x.serialize(value))
                    .putFloat(index + Float.BYTES, y.serialize(value)));
            return this;
        }

        public Builder<T> vec3(String name, Function<T, Vector3fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec3(String name, FloatSerializer<T> x, FloatSerializer<T> y, FloatSerializer<T> z) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC3, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putFloat(index, x.serialize(value))
                    .putFloat(index + Float.BYTES, y.serialize(value))
                    .putFloat(index + Float.BYTES * 2, z.serialize(value)));
            return this;
        }

        public Builder<T> vec4(String name, Function<T, Vector4fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec4(String name, FloatSerializer<T> x, FloatSerializer<T> y, FloatSerializer<T> z, FloatSerializer<T> w) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC4, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putFloat(index, x.serialize(value))
                    .putFloat(index + Float.BYTES, y.serialize(value))
                    .putFloat(index + Float.BYTES * 2, z.serialize(value))
                    .putFloat(index + Float.BYTES * 3, w.serialize(value)));
            return this;
        }

        public Builder<T> integer(String name, IntSerializer<T> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.INT, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putInt(index, serializer.serialize(value)));
            return this;
        }

        public Builder<T> vec2i(String name, Function<T, Vector2ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec2i(String name, IntSerializer<T> x, IntSerializer<T> y) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC2, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value)));
            return this;
        }

        public Builder<T> vec3i(String name, Function<T, Vector3ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec3i(String name, IntSerializer<T> x, IntSerializer<T> y, IntSerializer<T> z) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC3, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value))
                    .putInt(index + Integer.BYTES * 2, z.serialize(value)));
            return this;
        }

        public Builder<T> vec4i(String name, Function<T, Vector4ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec4i(String name, IntSerializer<T> x, IntSerializer<T> y, IntSerializer<T> z, IntSerializer<T> w) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC4, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value))
                    .putInt(index + Integer.BYTES * 2, z.serialize(value))
                    .putInt(index + Integer.BYTES * 3, w.serialize(value)));
            return this;
        }

        public VeilShaderBufferLayout<T> build() {
            if (this.memoryLayout == ShaderBlock.MemoryLayout.STD430 && this.binding != ShaderBlock.BufferBinding.SHADER_STORAGE) {
                throw new IllegalArgumentException("std430 only supports shader storage buffers");
            }
            return new VeilShaderBufferLayout<>(this.name, this.interfaceName, Collections.unmodifiableMap(this.fields), this.binding, this.memoryLayout, new GlslStructSpecifier(this.name, this.structFields));
        }

        @FunctionalInterface
        public interface FloatSerializer<T> {

            float serialize(T value);
        }

        @FunctionalInterface
        public interface IntSerializer<T> {

            int serialize(T value);
        }
    }
}

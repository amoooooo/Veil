package foundry.veil.api.client.render;

import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import io.github.ocelot.glslprocessor.api.grammar.*;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslNewNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslStructNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.*;
import java.util.function.Function;

/**
 * Defines the full layout of a shader block.
 *
 * @param name             The name of the block. This is the name of the block referenced in the code
 * @param fields           Each field and how to serialize it from the java type
 * @param requestedBinding The user requested binding to use. This will only be respected if the requested buffer type is supported on the hardware
 * @param memoryLayout     The memory layout OpenGL should use
 * @param structSpecifier  The specified for the struct data in the layout
 * @param <T>              The type of data the shader block will serialize
 */
public record VeilShaderBufferLayout<T>(String name,
                                        Map<String, FieldSerializer<T>> fields,
                                        ShaderBlock.BufferBinding requestedBinding,
                                        ShaderBlock.MemoryLayout memoryLayout,
                                        GlslStructSpecifier structSpecifier) {

    /**
     * Creates a GLSL node representation of this layout.
     *
     * @param shaderStorageSupported Whether shader storage blocks are supported and can be attempted
     * @param interfaceName          The namespace of the shader block in the shader
     * @return A node for
     */
    public GlslNode createNode(boolean shaderStorageSupported, @Nullable String interfaceName) {
        GlslTypeQualifier.StorageType storageType = switch (this.requestedBinding) {
            case UNIFORM -> GlslTypeQualifier.StorageType.UNIFORM;
            case SHADER_STORAGE ->
                    shaderStorageSupported ? GlslTypeQualifier.StorageType.BUFFER : GlslTypeQualifier.StorageType.UNIFORM;
        };

        GlslSpecifiedType structSpecifier = new GlslSpecifiedType(this.structSpecifier, GlslTypeQualifier.layout(this.memoryLayout.getLayoutId()), storageType);
        GlslNode node;
        if (interfaceName != null) {
            node = new GlslNewNode(structSpecifier, interfaceName, null);
        } else {
            node = new GlslStructNode(structSpecifier);
        }
        return node;
    }

    /**
     * @return The actual binding this block uses
     */
    public ShaderBlock.BufferBinding binding() {
        return VeilRenderSystem.shaderStorageBufferSupported() ? this.requestedBinding : ShaderBlock.BufferBinding.UNIFORM;
    }

    /**
     * Creates a new shader buffer builder.
     *
     * @param <T> The type of data the shader block will serialize
     * @return A new builder for creating a block
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Serializes a single field to the internal buffer.
     *
     * @param <T> The type of data to write to the buffer
     */
    @FunctionalInterface
    public interface FieldSerializer<T> {

        /**
         * Writes data from the specified value to the buffer.
         *
         * @param value  The value to write data from
         * @param index  The location to place the data at
         * @param buffer The buffer to write to
         */
        void write(T value, int index, ByteBuffer buffer);
    }

    /**
     * Creates a new layout for a shader block. This automatically figures out what the size and offsets need to be to create the block.
     *
     * @param <T> The type of data the shader block will serialize
     * @author Ocelot
     */
    public static class Builder<T> {

        private static final Random RANDOM = new Random();

        private final String name;
        private final List<GlslStructField> structFields;
        private final Map<String, FieldSerializer<T>> fields;
        private ShaderBlock.BufferBinding binding;
        private ShaderBlock.MemoryLayout memoryLayout;

        public Builder() {
            this.name = "VeilBuffer" + Math.abs(RANDOM.nextInt());
            this.structFields = new ArrayList<>();
            this.fields = new Object2ObjectArrayMap<>();
            this.binding = ShaderBlock.BufferBinding.UNIFORM;
            this.memoryLayout = ShaderBlock.MemoryLayout.SHARED;
        }

        public Builder<T> binding(ShaderBlock.BufferBinding binding) {
            this.binding = binding;
            return this;
        }

        public Builder<T> memoryLayout(ShaderBlock.MemoryLayout memoryLayout) {
            this.memoryLayout = memoryLayout;
            return this;
        }

        public Builder<T> f32(String name, FloatSerializer<T> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.FLOAT, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putFloat(index, serializer.serialize(value)));
            return this;
        }

        public Builder<T> f64(String name, DoubleSerializer<T> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DOUBLE, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putDouble(index, serializer.serialize(value)));
            return this;
        }

        public Builder<T> integer(String name, IntSerializer<T> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.INT, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putInt(index, serializer.serialize(value)));
            return this;
        }

        public Builder<T> uint(String name, IntSerializer<T> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UINT, name));
            this.fields.put(name, (value, index, buffer) -> buffer.putInt(index, serializer.serialize(value)));
            return this;
        }

        public Builder<T> vec2(String name, Function<T, Vector2fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> vec2(String name, FloatSerializer<T> x, FloatSerializer<T> y) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.VEC2, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putFloat(index, x.serialize(value))
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
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putFloat(index, x.serialize(value))
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
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putFloat(index, x.serialize(value))
                    .putFloat(index + Float.BYTES, y.serialize(value))
                    .putFloat(index + Float.BYTES * 2, z.serialize(value))
                    .putFloat(index + Float.BYTES * 3, w.serialize(value)));
            return this;
        }

        public Builder<T> dvec2(String name, Function<T, Vector2dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DVEC2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dvec2(String name, DoubleSerializer<T> x, DoubleSerializer<T> y) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DVEC2, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putDouble(index, x.serialize(value))
                    .putDouble(index + Double.BYTES, y.serialize(value)));
            return this;
        }

        public Builder<T> dvec3(String name, Function<T, Vector3dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DVEC3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dvec3(String name, DoubleSerializer<T> x, DoubleSerializer<T> y, DoubleSerializer<T> z) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DVEC3, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putDouble(index, x.serialize(value))
                    .putDouble(index + Double.BYTES, y.serialize(value))
                    .putDouble(index + Double.BYTES * 2, z.serialize(value)));
            return this;
        }

        public Builder<T> dvec4(String name, Function<T, Vector4dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DVEC4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dvec4(String name, DoubleSerializer<T> x, DoubleSerializer<T> y, DoubleSerializer<T> z, DoubleSerializer<T> w) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DVEC4, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putDouble(index, x.serialize(value))
                    .putDouble(index + Double.BYTES, y.serialize(value))
                    .putDouble(index + Double.BYTES * 2, z.serialize(value))
                    .putDouble(index + Double.BYTES * 3, w.serialize(value)));
            return this;
        }

        public Builder<T> ivec2(String name, Function<T, Vector2ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> ivec2(String name, IntSerializer<T> x, IntSerializer<T> y) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC2, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value)));
            return this;
        }

        public Builder<T> ivec3(String name, Function<T, Vector3ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> ivec3(String name, IntSerializer<T> x, IntSerializer<T> y, IntSerializer<T> z) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC3, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value))
                    .putInt(index + Integer.BYTES * 2, z.serialize(value)));
            return this;
        }

        public Builder<T> ivec4(String name, Function<T, Vector4ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> ivec4(String name, IntSerializer<T> x, IntSerializer<T> y, IntSerializer<T> z, IntSerializer<T> w) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.IVEC4, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value))
                    .putInt(index + Integer.BYTES * 2, z.serialize(value))
                    .putInt(index + Integer.BYTES * 3, w.serialize(value)));
            return this;
        }

        public Builder<T> uvec2(String name, Function<T, Vector2ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UVEC2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> uvec2(String name, IntSerializer<T> x, IntSerializer<T> y) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UVEC2, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value)));
            return this;
        }

        public Builder<T> uvec3(String name, Function<T, Vector3ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UVEC3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> uvec3(String name, IntSerializer<T> x, IntSerializer<T> y, IntSerializer<T> z) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UVEC3, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value))
                    .putInt(index + Integer.BYTES * 2, z.serialize(value)));
            return this;
        }

        public Builder<T> uvec4(String name, Function<T, Vector4ic> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UVEC4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> uvec4(String name, IntSerializer<T> x, IntSerializer<T> y, IntSerializer<T> z, IntSerializer<T> w) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.UVEC4, name));
            this.fields.put(name, (value, index, buffer) -> buffer
                    .putInt(index, x.serialize(value))
                    .putInt(index + Integer.BYTES, y.serialize(value))
                    .putInt(index + Integer.BYTES * 2, z.serialize(value))
                    .putInt(index + Integer.BYTES * 3, w.serialize(value)));
            return this;
        }

        public Builder<T> mat2(String name, Function<T, Matrix2fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.MAT2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> mat3(String name, Function<T, Matrix3fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.MAT3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> mat4(String name, Function<T, Matrix4fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.MAT4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> mat3x2(String name, Function<T, Matrix3x2fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.MAT3X2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> mat4x3(String name, Function<T, Matrix4x3fc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.MAT4X3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dmat2(String name, Function<T, Matrix2dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DMAT2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dmat3(String name, Function<T, Matrix3dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DMAT3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dmat4(String name, Function<T, Matrix4dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DMAT4, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dmat3x2(String name, Function<T, Matrix3x2dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DMAT3X2, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        public Builder<T> dmat4x3(String name, Function<T, Matrix4x3dc> serializer) {
            this.structFields.add(new GlslStructField(GlslTypeSpecifier.BuiltinType.DMAT4X3, name));
            this.fields.put(name, (value, index, buffer) -> serializer.apply(value).get(index, buffer));
            return this;
        }

        /**
         * @return A new buffer layout from this builder
         */
        public VeilShaderBufferLayout<T> build() {
            if (this.memoryLayout == ShaderBlock.MemoryLayout.STD430 && this.binding != ShaderBlock.BufferBinding.SHADER_STORAGE) {
                throw new IllegalArgumentException("std430 only supports shader storage buffers");
            }
            if (this.fields.isEmpty()) {
                throw new IllegalArgumentException("At least 1 field must be defined in a shader block");
            }
            return new VeilShaderBufferLayout<>(this.name, Collections.unmodifiableMap(this.fields), this.binding, this.memoryLayout, new GlslStructSpecifier(this.name, this.structFields));
        }

        /**
         * Serializes a float field from the specified object.
         *
         * @param <T> The type of object to get the field from
         */
        @FunctionalInterface
        public interface FloatSerializer<T> {

            float serialize(T value);
        }

        /**
         * Serializes a double field from the specified object.
         *
         * @param <T> The type of object to get the field from
         */
        @FunctionalInterface
        public interface DoubleSerializer<T> {

            double serialize(T value);
        }

        /**
         * Serializes an int field from the specified object.
         *
         * @param <T> The type of object to get the field from
         */
        @FunctionalInterface
        public interface IntSerializer<T> {

            int serialize(T value);
        }
    }
}

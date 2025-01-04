package foundry.veil.impl.client.render.ext;

import com.mojang.blaze3d.platform.GlStateManager;
import foundry.veil.api.client.render.shader.ShaderException;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.List;

import static org.lwjgl.opengl.ARBGLSPIRV.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB;
import static org.lwjgl.opengl.ARBGLSPIRV.glSpecializeShaderARB;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL41C.glShaderBinary;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL46C.GL_SHADER_BINARY_FORMAT_SPIR_V;
import static org.lwjgl.opengl.GL46C.glSpecializeShader;
import static org.lwjgl.util.shaderc.Shaderc.*;

@ApiStatus.Internal
public enum VeilShaderSPIRV {
    NONE {
        @Override
        public void compile(int shader, int type, String fileName, String source, boolean hlsl) throws ShaderException {
            if (hlsl) {
                throw new ShaderException("HLSL is unsupported on this platform");
            }
            GlStateManager.glShaderSource(shader, List.of(source));
        }
    },
    ARB {
        @Override
        public void compile(int shader, int type, String fileName, String source, boolean hlsl) throws ShaderException {
            try (SPIRV spirv = compileSPIRV(fileName, source, type, hlsl); MemoryStack stack = MemoryStack.stackPush()) {
                glShaderBinary(stack.ints(shader), GL_SHADER_BINARY_FORMAT_SPIR_V_ARB, spirv.bytecode);
                glSpecializeShaderARB(shader, "main", stack.mallocInt(0), stack.mallocInt(0));
            }
        }
    },
    CORE {
        @Override
        public void compile(int shader, int type, String fileName, String source, boolean hlsl) throws ShaderException {
            try (SPIRV spirv = compileSPIRV(fileName, source, type, hlsl); MemoryStack stack = MemoryStack.stackPush()) {
                glShaderBinary(stack.ints(shader), GL_SHADER_BINARY_FORMAT_SPIR_V, spirv.bytecode);
                glSpecializeShader(shader, "main", stack.mallocInt(0), stack.mallocInt(0));
            }
        }
    };

    private static final Int2IntMap KIND_MAP = new Int2IntArrayMap();
    private static VeilShaderSPIRV uploader;

    static {
        KIND_MAP.put(GL_VERTEX_SHADER, shaderc_glsl_vertex_shader);
        KIND_MAP.put(GL_TESS_CONTROL_SHADER, shaderc_glsl_tess_control_shader);
        KIND_MAP.put(GL_TESS_EVALUATION_SHADER, shaderc_glsl_tess_evaluation_shader);
        KIND_MAP.put(GL_GEOMETRY_SHADER, shaderc_glsl_geometry_shader);
        KIND_MAP.put(GL_FRAGMENT_SHADER, shaderc_glsl_fragment_shader);
        KIND_MAP.put(GL_COMPUTE_SHADER, shaderc_glsl_compute_shader);
    }

    public static SPIRV compileSPIRV(String fileName, String source, int type, boolean hlsl) throws ShaderException {
        long compiler = shaderc_compiler_initialize();

        try {
            if (compiler == MemoryUtil.NULL) {
                throw new ShaderException("Failed to create shader compiler");
            }

            long options = shaderc_compiler_initialize();
            if (options == MemoryUtil.NULL) {
                throw new ShaderException("Failed to create shader compiler");
            }

            shaderc_compile_options_set_source_language(options, hlsl ? shaderc_source_language_hlsl : shaderc_source_language_glsl);
            long result = shaderc_compile_into_spv(compiler, source, KIND_MAP.get(type), fileName, "main", options);
            try {
                if (result == MemoryUtil.NULL) {
                    throw new ShaderException("Failed to compile shader " + fileName + " into SPIR-V");
                }

                if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                    throw new ShaderException("Failed to compile shader " + fileName + " into SPIR-V", shaderc_result_get_error_message(result));
                }
                return new SPIRV(result, shaderc_result_get_bytes(result));
            } finally {
                shaderc_compile_options_release(options);
            }
        } finally {
            shaderc_compiler_release(compiler);
        }
    }

    public abstract void compile(int shader, int type, String fileName, String source, boolean hlsl) throws ShaderException;

    public static VeilShaderSPIRV get() {
        if (uploader == null) {
            GLCapabilities caps = GL.getCapabilities();
            if (caps.OpenGL46) {
                uploader = CORE;
            } else if (caps.GL_ARB_gl_spirv) {
                uploader = ARB;
            } else {
                uploader = NONE;
            }
        }
        return uploader;
    }

    public static final class SPIRV implements NativeResource {

        private final long pointer;
        private ByteBuffer bytecode;

        public SPIRV(long pointer, ByteBuffer bytecode) {
            this.pointer = pointer;
            this.bytecode = bytecode;
        }

        @Override
        public void free() {
            shaderc_result_release(this.pointer);
            this.bytecode = null; // Help the GC
        }
    }
}

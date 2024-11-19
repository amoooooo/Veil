package foundry.veil.api.client.render;

import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL11C.glGetInteger;

/**
 * Shader stage specific resource limits.
 *
 * @param maxUniformComponents   The number of active components of uniform variables that can be defined outside a uniform block.
 *                               The term "component" is meant as the basic component of a vector/matrix, so a vec3 takes up 3 components.
 *                               The minimum value here is 1024, enough room for 256 vec4s.
 * @param maxUniformBlocks       The maximum number of uniform blocks that this shader stage can access.
 *                               The OpenGL-required minimum is 12 in GL 3.3, and 14 in GL 4.3.
 * @param maxInputComponents     The maximum number of components that this stage can take as input.
 *                               The required minimum value differs from shader stage to shader stage.
 * @param maxOutputComponents    The maximum number of components that this stage can output.
 *                               The required minimum value differs from shader stage to shader stage.
 * @param maxTextureImageUnits   The maximum number of texture image units that the sampler in this shader can access.
 *                               The OpenGL-required minimum value is 16 for each stage.
 * @param maxImageUniforms       The maximum number of image variables for this shader stage.
 *                               The OpenGL 4.2 required minimum is eight for fragment and compute shaders, and 0 for the rest.
 *                               This means implementations may not allow you to use image variables in non-fragment or compute stages.
 * @param maxAtomicCounters      The maximum number of Atomic Counter variables that this stage can define.
 *                               The OpenGL 4.2 required minimum is eight for fragment and compute shaders, and 0 for the rest.
 * @param maxAtomicCountBuffers  The maximum number of different buffers that the atomic counter variables can come from.
 *                               The OpenGL 4.2 required minimum is one for fragment shaders, 8 for compute shaders, and 0 for the rest.
 * @param maxShaderStorageBlocks The maximum number of different shader storage blocks that a stage can use.
 *                               For fragment and compute shaders, the OpenGL 4.3 required minimum is 8; for the rest, it is 0.
 * @see <a target="_blank" href="https://www.khronos.org/opengl/wiki/Shader#Resource_limitations">Reference Page</a>
 */
public record VeilShaderLimits(int maxUniformComponents,
                               int maxUniformBlocks,
                               int maxInputComponents,
                               int maxOutputComponents,
                               int maxTextureImageUnits,
                               int maxImageUniforms,
                               int maxAtomicCounters,
                               int maxAtomicCountBuffers,
                               int maxShaderStorageBlocks) {

    public VeilShaderLimits(GLCapabilities caps,
                            int maxUniformComponents,
                            int maxUniformBlocks,
                            int maxInputComponents,
                            int maxOutputComponents,
                            int maxTextureImageUnits,
                            int maxImageUniforms,
                            int maxAtomicCounters,
                            int maxAtomicCountBuffers,
                            int maxShaderStorageBlocks) {
        this(maxUniformComponents,
                maxUniformBlocks,
                maxInputComponents,
                maxOutputComponents,
                maxTextureImageUnits,
                caps.OpenGL42 || caps.GL_ARB_shader_image_load_store ? glGetInteger(maxImageUniforms) : 0,
                caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters ? glGetInteger(maxAtomicCounters) : 0,
                caps.OpenGL42 || caps.GL_ARB_shader_atomic_counters ? glGetInteger(maxAtomicCountBuffers) : 0,
                caps.OpenGL43 || caps.GL_ARB_shader_storage_buffer_object ? glGetInteger(maxShaderStorageBlocks) : 0);
    }
}

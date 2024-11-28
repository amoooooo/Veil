package foundry.veil.fabric.compat.sodium;

import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniform;
import org.joml.Matrix3fc;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20C.glUniformMatrix3fv;

public class VeilNormalUniform extends GlUniform<Matrix3fc> {

    public VeilNormalUniform(int index) {
        super(index);
    }

    @Override
    public void set(Matrix3fc value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(9);
            value.get(buffer);
            glUniformMatrix3fv(this.index, false, buffer);
        }
    }
}


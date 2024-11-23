package foundry.veil.impl.client.render.dynamicbuffer;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.ShaderManager;
import foundry.veil.mixin.accessor.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.ApiStatus;

import java.util.Locale;

import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40C.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL43C.GL_COMPUTE_SHADER;

@ApiStatus.Internal
public class DynamicBufferManger {

    private static final int[] GL_MAPPING = {
            GL_VERTEX_SHADER,
            GL_TESS_CONTROL_SHADER,
            GL_TESS_EVALUATION_SHADER,
            GL_GEOMETRY_SHADER,
            GL_FRAGMENT_SHADER,
            GL_COMPUTE_SHADER
    };

    private int activeBuffers;

    public DynamicBufferManger() {
        this.activeBuffers = 0;
    }

    public int getActiveBuffers() {
        return this.activeBuffers;
    }

    public boolean setActiveBuffers(int activeBuffers) {
        if (this.activeBuffers == activeBuffers) {
            return false;
        }

        this.activeBuffers = activeBuffers;

        try {
            VeilRenderSystem.renderer().getShaderManager().setActiveBuffers(activeBuffers);

            for (ShaderInstance shader : ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).getShaders().values()) {

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public int getAttachmentIndex(DynamicBufferType type) {
        if ((this.activeBuffers & type.getMask()) == 0) {
            return -1;
        }

        int index = 1;
        for (DynamicBufferType value : DynamicBufferType.values()) {
            if (value == type) {
                break;
            }
            index++;
        }
        return index;
    }

    public static int getShaderIndex(int glType, int activeBuffers) {
        for (int i = 0; i < GL_MAPPING.length; i++) {
            if (GL_MAPPING[i] == glType) {
                return i | activeBuffers << 4;
            }
        }
        throw new IllegalArgumentException("Invalid GL Shader Type: 0x" + Integer.toHexString(glType).toUpperCase(Locale.ROOT));
    }

    public static int getShaderType(int key) {
        return GL_MAPPING[key & 15];
    }
}

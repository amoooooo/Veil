package foundry.veil.impl.resource.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.resource.type.VeilShaderDefinitionResource;
import net.minecraft.resources.ResourceLocation;

public class VeilShaderDefinitionResourceRenderer {

    private static ResourceLocation last = null;

    public static void render(ShaderProgram shader, VeilShaderDefinitionResource resource, float width, float height) {
        shader.bind();
        if (!shader.getId().equals(last)) {
            last = shader.getId();

            VertexFormat format = shader.getFormat();
            System.out.println(format);
        }
        ShaderProgram.unbind();
    }
}

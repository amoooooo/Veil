package foundry.veil.impl.resource.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.resource.type.VeilShaderDefinitionResource;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public class VeilShaderDefinitionResourceRenderer {

    private static ResourceLocation last = null;

    public static void render(ShaderProgram shader, VeilShaderDefinitionResource resource, float width, float height) {
        VertexFormat format = Objects.requireNonNull(shader.getFormat());
        shader.bind();
        if (!shader.getId().equals(last)) {
            last = shader.getId();

            if (format.contains(VertexFormatElement.POSITION)) {
//                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
//                builder.addVertex()
            }
            System.out.println(format);
        }

        ShaderProgram.unbind();
    }
}

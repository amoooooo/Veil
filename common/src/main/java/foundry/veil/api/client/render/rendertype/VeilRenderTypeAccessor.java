package foundry.veil.api.client.render.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;

import java.util.List;

/**
 * Provides access to the individual shards of a {@link RenderType}.
 *
 * @author Ocelot
 */
public interface VeilRenderTypeAccessor {

    /**
     * @return The Minecraft state deciding what textures should be bound
     */
    RenderStateShard.EmptyTextureStateShard textureState();

    /**
     * @return The Minecraft state deciding what {@link ShaderInstance} to use
     */
    RenderStateShard.ShaderStateShard shaderState();

    /**
     * @return The state of {@link GL14C#glBlendFuncSeparate(int, int, int, int)}
     */
    RenderStateShard.TransparencyStateShard transparencyState();

    /**
     * @return The state of {@link GL11C#GL_DEPTH_TEST}
     */
    RenderStateShard.DepthTestStateShard depthTestState();

    /**
     * @return The state of {@link GL11C#glCullFace(int)}
     */
    RenderStateShard.CullStateShard cullState();

    /**
     * @return The Minecraft state determining if the {@link LightTexture} should be enabled
     */
    RenderStateShard.LightmapStateShard lightmapState();

    /**
     * @return The Minecraft state determining if the {@link OverlayTexture} should be enabled
     */
    RenderStateShard.OverlayStateShard overlayState();

    /**
     * @return The state of {@link GL11C#glPolygonOffset(float, float)} or {@link RenderSystem#getModelViewStack()}
     */
    RenderStateShard.LayeringStateShard layeringState();

    /**
     * @return The state of what framebuffer to write into
     */
    RenderStateShard.OutputStateShard outputState();

    /**
     * @return The Minecraft state intended to call {@link RenderSystem#setTextureMatrix(Matrix4f)}
     */
    RenderStateShard.TexturingStateShard texturingState();

    /**
     * @return The state of {@link GL11C#glColorMask(boolean, boolean, boolean, boolean)} and {@link GL11C#glDepthMask(boolean)}
     */
    RenderStateShard.WriteMaskStateShard writeMaskState();

    /**
     * @return The state of {@link GL11C#glLineWidth(float)}
     */
    RenderStateShard.LineStateShard lineState();

    /**
     * @return The state of {@link GL11C#glLogicOp(int)}
     */
    RenderStateShard.ColorLogicStateShard colorLogicState();

    /**
     * @return The outline property state
     */
    RenderType.OutlineProperty outlineProperty();

    /**
     * @return An immutable view of all states in the render type
     */
    List<RenderStateShard> states();
}

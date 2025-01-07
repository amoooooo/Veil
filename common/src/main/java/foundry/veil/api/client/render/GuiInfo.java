package foundry.veil.api.client.render;

import foundry.veil.api.client.registry.VeilShaderBufferRegistry;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import net.minecraft.client.Minecraft;

/**
 * Manages the global gui context variables.
 *
 * @author Ocelot
 */
public class GuiInfo {

    private float guiScale;
    private boolean enabled;

    /**
     * Creates a new set of camera matrices.
     */
    public GuiInfo() {
        this.guiScale = 0.0F;
        this.enabled = false;
    }

    public static VeilShaderBufferLayout<GuiInfo> createLayout() {
        return VeilShaderBufferLayout.<GuiInfo>builder()
                .interfaceName("GuiInfo")
                .f32("GuiScale", GuiInfo::getGuiScale)
                .build();
    }

    /**
     * Updates the camera matrices to match the current render system projection.
     */
    public void update() {
        ShaderBlock<GuiInfo> block = VeilRenderSystem.getBlock(VeilShaderBufferRegistry.GUI_INFO.get());
        if (block == null) {
            return;
        }

        this.guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        block.set(this);
        VeilRenderSystem.bind(VeilShaderBufferRegistry.GUI_INFO.get());
        this.enabled = true;
    }

    /**
     * Unbinds this shader block.
     */
    public void unbind() {
        VeilRenderSystem.unbind(VeilShaderBufferRegistry.GUI_INFO.get());
        this.enabled = false;
    }

    /**
     * @return The far clipping plane of the frustum
     */
    public float getGuiScale() {
        return this.guiScale;
    }

    /**
     * @return Whether the gui is currently being drawn
     */
    public boolean isGuiRendering() {
        return this.enabled;
    }
}

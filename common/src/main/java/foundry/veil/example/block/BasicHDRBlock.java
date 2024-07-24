package foundry.veil.example.block;

import foundry.veil.Veil;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.platform.VeilEventPlatform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class BasicHDRBlock extends Block {
    public static final ResourceLocation ID = Veil.veilPath("basic_hdr_block");

    public BasicHDRBlock() {
        super(Properties.of());
        VeilEventPlatform.INSTANCE.onVeilAssignBlockRenderLayer(
                event -> event.assignRenderLayer(VeilRenderType.SOLID_HDR, this));
    }
}

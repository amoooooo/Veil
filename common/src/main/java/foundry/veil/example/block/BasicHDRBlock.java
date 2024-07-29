package foundry.veil.example.block;

import foundry.veil.Veil;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderTypeAssigner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class BasicHDRBlock extends Block {
    public static final ResourceLocation ID = Veil.veilPath("basic_hdr_block");

    public BasicHDRBlock() {
        super(Properties.of());
        VeilRenderTypeAssigner.assign(this, VeilRenderType.SOLID_HDR);
    }
}

package foundry.veil.mixin.debug.client;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.ext.DebugVertexBufferExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(VertexFormat.class)
public class DebugVertexFormatMixin {

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    @Final
    private List<String> names;

    @Shadow
    @Nullable
    private VertexBuffer immediateDrawVertexBuffer;

    @Inject(method = "getImmediateDrawVertexBuffer", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vertex/VertexFormat;immediateDrawVertexBuffer:Lcom/mojang/blaze3d/vertex/VertexBuffer;", opcode = 181, shift = At.Shift.AFTER))
    public void nameImmediateDrawVertexBuffer(CallbackInfoReturnable<VertexBuffer> cir) {
        ((DebugVertexBufferExt) this.immediateDrawVertexBuffer).veil$setName("Vertex Format (" + this.vertexSize + " bytes): " + String.join(" ", this.names));
    }
}

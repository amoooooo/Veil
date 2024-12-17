//package foundry.veil.fabric.mixin.compat.iris;
//
//import foundry.veil.ext.iris.IrisFramebufferExtension;
//import it.unimi.dsi.fastutil.ints.Int2IntMap;
//import net.irisshaders.iris.gl.GlResource;
//import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.OptionalInt;
//
//@Mixin(GlFramebuffer.class)
//public abstract class GLFramebufferMixin extends GlResource implements IrisFramebufferExtension {
//
//    @Shadow(remap = false)
//    private boolean hasDepthAttachment;
//
//    @Shadow(remap = false)
//    @Final
//    private Int2IntMap attachments;
//
//    @Unique
//    private int veil$depthTextureId;
//
//    protected GLFramebufferMixin(int id) {
//        super(id);
//    }
//
//    @Inject(method = "addDepthAttachment", at = @At("TAIL"), remap = false)
//    public void addDepthAttachment(int texture, CallbackInfo ci) {
//        this.veil$depthTextureId = texture;
//    }
//
//    @Override
//    public int veil$getId() {
//        return this.getGlId();
//    }
//
//    @Override
//    public OptionalInt veil$getDepthAttachment() {
//        return this.hasDepthAttachment ? OptionalInt.of(this.veil$depthTextureId) : OptionalInt.empty();
//    }
//
//    @Override
//    public Int2IntMap veil$getColorAttachments() {
//        return this.attachments;
//    }
//}

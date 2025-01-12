package foundry.veil.mixin.debug.client;

import foundry.veil.api.client.render.ext.VeilDebug;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11C.GL_TEXTURE;

@Mixin(TextureManager.class)
public class DebugTextureManagerMixin {

    @Inject(method = "register(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V", at = @At("TAIL"))
    public void applyLabel(ResourceLocation name, AbstractTexture texture, CallbackInfo ci) {
        VeilDebug debug = VeilDebug.get();
        if (debug == VeilDebug.ENABLED) {
            texture.bind(); // Have to bind the texture to make sure it's been initialized
            debug.objectLabel(GL_TEXTURE, texture.getId(), "Texture " + name);
        }
    }
}

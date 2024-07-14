package foundry.veil.fabric.mixin.client;

import foundry.veil.VeilClient;
import net.minecraft.client.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;


/*  IDoTheRant

        For some reason, whenever the player clicks back after the mouse has been shown,
        it will think the mouse is already shown when the next time mouse key is pressed,
        I have spent the past 1 hour, 2 Minutes and 55 seconds trying to figure out
        why this is the case, only to realise that there is no reason to make it toggleable
        how stupid am I?

        For anyone who sees this and dares to make it toggleable, good luck...
        Look at the commented out sections, this just has the toggle and doesn't
        account for when the mouse is clicked

        This was a feature I made for my own client (hehe) and I thought it was a good idea
        to implement this into the official veil project

*/

@Mixin(KeyboardHandler.class)
public abstract class ShowMouse {
    @Unique private boolean mouseShown = false;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At(value = "TAIL"))
    private void onKeyPress(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            mouseShown = !mouseShown;
            Minecraft.getInstance().mouseHandler.grabMouse();
        }

        if (window == this.minecraft.getWindow().getWindow() && action == GLFW_PRESS && VeilClient.MOUSE_KEY.matches(key, scancode)) {
            if (this.minecraft.level != null) {
                //if (!mouseShown) {
                //    mouseShown = true;
                //    Minecraft.getInstance().mouseHandler.grabMouse();
                //    minecraft.gui.getChat().addMessage(Component.translatable("debug.mouse.hidden"));
                //} else {
                //VeilExampleMod.LOGGER.info("debug.mouse.shown");
                Minecraft.getInstance().mouseHandler.releaseMouse();
                //minecraft.gui.getChat().addMessage(Component.translatable("debug.mouse.shown")); Removed this so that it doesn't send an annoying message when F7 is Clicked
                mouseShown = false;
            }
        }
    }
}

package foundry.veil.impl.resource.action;

import foundry.veil.api.client.registry.VeilResourceEditorRegistry;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.type.FramebufferResource;
import net.minecraft.network.chat.Component;

import java.util.OptionalInt;

public class FramebufferEditAction implements VeilResourceAction<FramebufferResource> {

    private static final Component NAME = Component.translatable("editor.veil.resource.action.framebuffer_edit");
    private static final Component DESC = Component.translatable("editor.veil.resource.action.framebuffer_edit.desc");

    @Override
    public Component getName() {
        return NAME;
    }

    @Override
    public Component getDescription() {
        return DESC;
    }

    @Override
    public OptionalInt getIcon() {
        return OptionalInt.empty();
    }

    @Override
    public void perform(VeilEditorEnvironment environment, FramebufferResource resource) {
        environment.open(resource, VeilResourceEditorRegistry.FRAMEBUFFER.get());
    }
}

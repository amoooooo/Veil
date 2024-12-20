package foundry.veil.impl.resource.action;

import foundry.veil.api.client.registry.VeilResourceEditorRegistry;
import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.type.FramebufferResource;
import net.minecraft.network.chat.Component;

import java.util.OptionalInt;

public enum FramebufferEditAction implements VeilResourceAction<FramebufferResource> {

    INSTANCE;

    private static final Component NAME = Component.translatable("resource.veil.action.framebuffer_edit");
    private static final Component DESC = Component.translatable("resource.veil.action.framebuffer_edit.desc");

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
        return OptionalInt.of(0xEBCA);
    }

    @Override
    public void perform(VeilEditorEnvironment environment, FramebufferResource resource) {
        environment.open(resource, VeilResourceEditorRegistry.FRAMEBUFFER.get());
    }
}

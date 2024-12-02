package foundry.veil.impl.resource.action;

import foundry.veil.api.resource.VeilEditorEnvironment;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.type.BlockModelResource;
import net.minecraft.network.chat.Component;

import java.util.OptionalInt;

public record ModelEditAction<T extends BlockModelResource>() implements VeilResourceAction<T> {

    private static final Component NAME = Component.translatable("editor.veil.resource.action.model_edit");
    private static final Component DESC = Component.translatable("editor.veil.resource.action.model_edit.desc");

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
        return OptionalInt.of(0xECDB); // Edit file line icon
    }

    @Override
    public void perform(VeilEditorEnvironment environment, T resource) {
        // TODO add a screen for model viewing
    }
}

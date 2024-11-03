package foundry.veil.api.resource.type;

import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;

import java.util.List;

public record UnknownResource(VeilResourceInfo resourceInfo) implements VeilResource<UnknownResource> {

    @Override
    public List<VeilResourceAction<UnknownResource>> getActions() {
        return List.of();
    }

    @Override
    public boolean canHotReload() {
        return false;
    }

    @Override
    public void hotReload() {
    }

    @Override
    public int getIconCode() {
        return 0xED13; // Unknown file icon
    }
}

package foundry.veil.api.resource.type;

import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import foundry.veil.api.resource.VeilResourceManager;
import foundry.veil.impl.resource.action.TextEditAction;

import java.util.List;

public record VanillaShaderFileResource(
        VeilResourceInfo resourceInfo) implements VeilShaderResource<VanillaShaderFileResource> {

    @Override
    public List<VeilResourceAction<VanillaShaderFileResource>> getActions() {
        return List.of(new TextEditAction<>());
    }

    @Override
    public boolean canHotReload() {
        return false;
    }

    @Override
    public void hotReload(VeilResourceManager resourceManager) {
    }
}

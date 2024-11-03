package foundry.veil.api.resource.type;

import foundry.veil.api.resource.VeilResource;
import foundry.veil.api.resource.VeilResourceAction;
import foundry.veil.api.resource.VeilResourceInfo;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.util.List;

public record McMetaResource(VeilResourceInfo resourceInfo, ResourceMetadata metadata) implements VeilResource<McMetaResource> {

    @Override
    public List<VeilResourceAction<McMetaResource>> getActions() {
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
        return 0xECEA; // Info file icon
    }
}

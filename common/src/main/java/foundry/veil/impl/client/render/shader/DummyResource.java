package foundry.veil.impl.client.render.shader;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Optional;

public class DummyResource extends Resource {

    public DummyResource(IoSupplier<InputStream> streamSupplier) {
        super(null, streamSupplier);
    }

    @Override
    public PackResources source() {
        throw new UnsupportedOperationException("No pack source");
    }

    @Override
    public String sourcePackId() {
        return "dummy";
    }

    @Override
    public Optional<KnownPack> knownPackInfo() {
        return Optional.empty();
    }
}

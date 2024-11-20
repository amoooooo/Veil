//package foundry.veil.forge.mixin.resources;
//
//import foundry.veil.ext.PackResourcesExtension;
//import net.minecraft.server.packs.PackResources;
//import net.minecraft.server.packs.resources.IoSupplier;
//import org.jetbrains.annotations.Nullable;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//
//import java.io.InputStream;
//import java.util.List;
//import java.util.stream.Stream;
//
//@Mixin(DelegatingPackResources.class)
//public class DelegatingPackResourcesMixin implements PackResourcesExtension {
//
//    @Shadow
//    @Final
//    private List<PackResources> delegates;
//
//    @Override
//    public void veil$listResources(PackResourceConsumer consumer) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public @Nullable IoSupplier<InputStream> veil$getIcon() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Stream<PackResources> veil$listPacks() {
//        return this.delegates.stream().flatMap(pack -> {
//            if (pack instanceof PackResourcesExtension extension) {
//                return extension.veil$listPacks();
//            } else {
//                return Stream.of(pack);
//            }
//        });
//    }
//}

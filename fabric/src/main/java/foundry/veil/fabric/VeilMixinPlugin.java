package foundry.veil.fabric;

import foundry.veil.Veil;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VeilMixinPlugin implements IMixinConfigPlugin {

    private static final Set<String> COMPAT = Set.of(
            "foundry.veil.fabric.mixin.client.stage",
            "foundry.veil.fabric.mixin.client.pipeline");
    private static final Set<String> SODIUM_WITHOUT_IRIS_COMPAT = Set.of(
    );
    private final Map<String, Boolean> loadedMods = new HashMap<>();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        for (String compat : COMPAT) {
            if (mixinClassName.startsWith(compat)) {
                return Veil.SODIUM ? !mixinClassName.startsWith(compat + ".vanilla") : !mixinClassName.startsWith(compat + ".sodium");
            }
        }
        if (mixinClassName.startsWith("foundry.veil.fabric.mixin.compat")) {
            if (Veil.IRIS && SODIUM_WITHOUT_IRIS_COMPAT.contains(mixinClassName)) {
                return false;
            }
            String[] parts = mixinClassName.split("\\.");
            return this.loadedMods.computeIfAbsent(parts[5], Veil.platform()::isModLoaded);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    // Hack to make sure mixin doesn't have a panic attack
    public void preApply(String targetClassName, org.spongepowered.asm.lib.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, org.spongepowered.asm.lib.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}

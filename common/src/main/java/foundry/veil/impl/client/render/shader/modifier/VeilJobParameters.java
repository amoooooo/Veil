package foundry.veil.impl.client.render.shader.modifier;

import foundry.veil.api.client.render.shader.ShaderModificationManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public record VeilJobParameters(ShaderModificationManager modificationManager,
                                ResourceLocation shaderId,
                                int flags) {

    /**
     * Whether the version is required and will be applied
     */
    public static final int APPLY_VERSION = 0b01;
    /**
     * Whether [OUT] is a valid command
     */
    public static final int ALLOW_OUT = 0b10;

    public Collection<ShaderModification> modifiers() {
        return this.modificationManager.getModifiers(this.shaderId);
    }

    public boolean applyVersion() {
        return (this.flags & APPLY_VERSION) > 0;
    }

    public boolean allowOut() {
        return (this.flags & ALLOW_OUT) > 0;
    }
}

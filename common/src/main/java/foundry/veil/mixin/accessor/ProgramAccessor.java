package foundry.veil.mixin.accessor;

import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Program.class)
public interface ProgramAccessor {

    @Accessor
    void setId(int id);
}

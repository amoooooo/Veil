package foundry.veil.api.quasar.emitters.module.update;

import foundry.veil.api.quasar.emitters.module.UpdateParticleModule;
import foundry.veil.api.quasar.particle.QuasarParticle;
import foundry.veil.api.quasar.particle.RenderData;
import foundry.veil.api.quasar.registry.RenderStyleRegistry;
import net.minecraft.util.Mth;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class FaceVelocityModule implements UpdateParticleModule {

    private final Vector3d temp;

    public FaceVelocityModule() {
        this.temp = new Vector3d();
    }

    @Override
    public void update(QuasarParticle particle) {
        Vector3d normalizedMotion = particle.getVelocity().normalize(this.temp);
        Vector3f rotation = particle.getRotation();
        rotation.x = (float) Mth.atan2(normalizedMotion.y, Math.sqrt(normalizedMotion.x * normalizedMotion.x + normalizedMotion.z * normalizedMotion.z));
        rotation.y = (float) Mth.atan2(normalizedMotion.x, normalizedMotion.z);
        if (particle.getData().renderStyle() == RenderStyleRegistry.BILLBOARD.get()) {
            rotation.y += (float) (Math.PI / 2.0);
        }
    }
}

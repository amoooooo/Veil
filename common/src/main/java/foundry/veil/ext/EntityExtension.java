package foundry.veil.ext;

import foundry.veil.api.quasar.particle.ParticleEmitter;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface EntityExtension {

    void veil$addEmitter(ParticleEmitter emitter);

    List<ParticleEmitter> veil$getEmitters();
}

package foundry.veil.api.quasar.particle;

import foundry.veil.Veil;
import foundry.veil.api.TickTaskScheduler;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.quasar.data.ParticleEmitterData;
import foundry.veil.api.quasar.data.QuasarParticles;
import foundry.veil.impl.TickTaskSchedulerImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ParticleSystemManager {

    private static final int MAX_PARTICLES = 10000;
    private static final double PERSISTENT_DISTANCE_SQ = 32.0 * 32.0;
    private static final double REMOVAL_DISTANCE_SQ = 128.0 * 128.0;

    private final List<ParticleEmitter> particleEmitters;
    private final Set<ResourceLocation> invalidEmitters;
    private final AtomicInteger particleCount;

    private ClientLevel level;
    private TickTaskSchedulerImpl scheduler;

    public ParticleSystemManager() {
        this.particleEmitters = new ArrayList<>();
        this.invalidEmitters = new HashSet<>();

        this.particleCount = new AtomicInteger();
        this.level = null;
        this.scheduler = null;
    }

    @ApiStatus.Internal
    public void setLevel(@Nullable ClientLevel level) {
        this.clear();
        if (this.scheduler != null) {
            this.scheduler.shutdown();
        }

        this.level = level;
        this.scheduler = new TickTaskSchedulerImpl();
    }

    public @Nullable ParticleEmitter createEmitter(ResourceLocation name) {
        if (this.level == null) {
            return null;
        }
        ParticleEmitterData data = QuasarParticles.registryAccess().registry(QuasarParticles.EMITTER).map(registry -> registry.get(name)).orElse(null);
        if (data == null) {
            if (this.invalidEmitters.add(name)) {
                Veil.LOGGER.error("Unknown Quasar Particle Emitter: {}", name);
            }
            return null;
        }
        return new ParticleEmitter(this, this.level, data);
    }

    public void addParticleSystem(ParticleEmitter particleEmitter) {
        this.scheduler.execute(() -> this.particleEmitters.add(particleEmitter));
    }

    public void clear() {
        for (ParticleEmitter particleEmitter : this.particleEmitters) {
            particleEmitter.onRemoved();
        }
        this.particleEmitters.clear();
    }

    @ApiStatus.Internal
    public void tick() {
        if (this.level == null) {
            return;
        }

        this.scheduler.run();
        this.particleCount.set(0);
        Iterator<ParticleEmitter> iterator = this.particleEmitters.iterator();
        while (iterator.hasNext()) {
            ParticleEmitter emitter = iterator.next();
            emitter.tick();
            if (emitter.isRemoved()) {
                emitter.onRemoved();
                iterator.remove();
                continue;
            }

            this.particleCount.addAndGet(emitter.getParticleCount());
        }
    }

    @ApiStatus.Internal
    public void render(MatrixStack matrixStack, MultiBufferSource bufferSource, Camera camera, CullFrustum frustum, float partialTicks) {
        // TODO store emitters per-chunk and fetch them from the renderer

        this.particleEmitters.sort(Comparator.comparingDouble(a -> -a.getPosition().distanceSquared(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z)));
        for (ParticleEmitter emitter : this.particleEmitters) {
            emitter.render(matrixStack, bufferSource, camera, partialTicks);
        }
    }

    /**
     * Attempts to remove particles from the most dense and farthest particle emitters to make room for closer emitters.
     *
     * @param particles The number of particles being spawned
     */
    public void reserve(int particles) {
        int freeSpace = MAX_PARTICLES - this.particleCount.getAndAdd(-particles); // This isn't correct, but it doesn't really matter
        if (particles <= freeSpace) {
            return;
        }

        particles -= freeSpace;
        Entity cameraEntity = Minecraft.getInstance().cameraEntity;
        for (ParticleEmitter emitter : this.particleEmitters) {
            Vector3d pos = emitter.getPosition();
            double scaleFactor = Math.min(cameraEntity != null ? (cameraEntity.distanceToSqr(pos.x, pos.y, pos.z) - PERSISTENT_DISTANCE_SQ) / REMOVAL_DISTANCE_SQ : 1.0, 1.0);
            if (scaleFactor > 0) {
                particles -= emitter.trim(Math.min(particles, Mth.ceil(emitter.getParticleCount() * scaleFactor)));
                if (particles <= 0) {
                    break;
                }
            }
        }
    }

    public ClientLevel getLevel() {
        return this.level;
    }

    public TickTaskScheduler getScheduler() {
        return this.scheduler;
    }

    public int getEmitterCount() {
        return this.particleEmitters.size();
    }

    public int getParticleCount() {
        return this.particleCount.get();
    }
}

package foundry.veil.api.quasar.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.particle.QuasarParticle;
import foundry.veil.api.quasar.particle.RenderStyle;
import foundry.veil.api.quasar.particle.SpriteData;
import foundry.veil.api.quasar.registry.RenderStyleRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Data passed to each particle when it is created.</p>
 *
 * <p>This class is used to store all the data that is passed to each particle when it is created.
 * This includes the particle settings, whether or not the particle should collide with blocks,
 * whether or not the particle should face its velocity, and the list of sub emitters.</p>
 *
 * <p>This class also stores the list of particle modules that are applied to each particle.
 * These modules are used to modify the particle's behavior. The following are valid particle modules:</p>
 *
 * <ul>
 *   <li>Init Modules - Applied when a particle is created</li>
 *   <li>Update Modules - Applied at the beginning of the particle tick</li>
 *   <li>Collision Modules - Applied when the particle collides with a block or entity</li>
 *   <li>Force Modules - Applied each physics tick to update velocity</li>
 *   <li>Render Modules - Applied when the particle is rendered</li>
 * </ul>
 *
 * @author amo
 * @see QuasarParticle
 */
public record QuasarParticleData(boolean shouldCollide,
                                 boolean faceVelocity,
                                 float velocityStretchFactor,
                                 List<Holder<ParticleModuleData>> initModules,
                                 List<Holder<ParticleModuleData>> updateModules,
                                 List<Holder<ParticleModuleData>> collisionModules,
                                 List<Holder<ParticleModuleData>> forceModules,
                                 List<Holder<ParticleModuleData>> renderModules,
                                 @Nullable SpriteData spriteData,
                                 boolean additive,
                                 RenderStyle renderStyle) {

    public static final Codec<QuasarParticleData> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("should_collide", true).forGetter(QuasarParticleData::shouldCollide),
            Codec.BOOL.optionalFieldOf("face_velocity", false).forGetter(QuasarParticleData::faceVelocity),
            Codec.FLOAT.optionalFieldOf("velocity_stretch_factor", 0.0F).forGetter(QuasarParticleData::velocityStretchFactor),
            ParticleModuleData.INIT_CODEC.listOf().optionalFieldOf("init_modules", Collections.emptyList()).forGetter(QuasarParticleData::initModules),
            ParticleModuleData.UPDATE_CODEC.listOf().optionalFieldOf("update_modules", Collections.emptyList()).forGetter(QuasarParticleData::updateModules),
            ParticleModuleData.UPDATE_CODEC.listOf().optionalFieldOf("collision_modules", Collections.emptyList()).forGetter(QuasarParticleData::collisionModules),
            ParticleModuleData.UPDATE_CODEC.listOf().optionalFieldOf("forces", Collections.emptyList()).forGetter(QuasarParticleData::forceModules),
            ParticleModuleData.RENDER_CODEC.listOf().optionalFieldOf("render_modules", Collections.emptyList()).forGetter(QuasarParticleData::renderModules),
            SpriteData.CODEC.optionalFieldOf("sprite_data").forGetter(data -> Optional.ofNullable(data.spriteData())),
            Codec.BOOL.optionalFieldOf("additive", false).forGetter(QuasarParticleData::additive),
            RenderStyle.CODEC.optionalFieldOf("render_style", RenderStyleRegistry.BILLBOARD.get()).forGetter(QuasarParticleData::renderStyle)
    ).apply(instance, (shouldCollide, faceVelocity, velocityStretchFactor, initModules, updateModules, collisionModules, forceModules, renderModules, spriteData, additive, renderStyle) -> new QuasarParticleData(shouldCollide, faceVelocity, velocityStretchFactor, initModules, updateModules, collisionModules, forceModules, renderModules, spriteData.orElse(null), additive, renderStyle)));
    public static final Codec<Holder<QuasarParticleData>> CODEC = RegistryFileCodec.create(QuasarParticles.PARTICLE_DATA, DIRECT_CODEC);

    public QuasarParticleData(boolean shouldCollide,
                              boolean faceVelocity,
                              float velocityStretchFactor,
                              List<Holder<ParticleModuleData>> initModules,
                              List<Holder<ParticleModuleData>> updateModules,
                              List<Holder<ParticleModuleData>> collisionModules,
                              List<Holder<ParticleModuleData>> forceModules,
                              List<Holder<ParticleModuleData>> renderModules,
                              @Nullable SpriteData spriteData,
                              boolean additive,
                              RenderStyle renderStyle) {
        this.shouldCollide = shouldCollide;
        this.faceVelocity = faceVelocity;
        this.velocityStretchFactor = velocityStretchFactor;
        // Prevent users from modifying core particle data
        this.initModules = Collections.unmodifiableList(initModules);
        this.updateModules = Collections.unmodifiableList(updateModules);
        this.collisionModules = Collections.unmodifiableList(collisionModules);
        this.forceModules = Collections.unmodifiableList(forceModules);
        this.renderModules = Collections.unmodifiableList(renderModules);
        this.spriteData = spriteData;
        this.additive = additive;
        this.renderStyle = renderStyle;
    }

    /**
     * @return A stream containing all modules in the particle.
     */
    public Stream<Holder<ParticleModuleData>> allModules() {
        Stream.Builder<Holder<ParticleModuleData>> builder = Stream.builder();
        for (Holder<ParticleModuleData> initModule : this.initModules) {
            builder.add(initModule);
        }
        for (Holder<ParticleModuleData> initModule : this.updateModules) {
            builder.add(initModule);
        }
        for (Holder<ParticleModuleData> initModule : this.collisionModules) {
            builder.add(initModule);
        }
        for (Holder<ParticleModuleData> initModule : this.forceModules) {
            builder.add(initModule);
        }
        for (Holder<ParticleModuleData> initModule : this.renderModules) {
            builder.add(initModule);
        }
        return builder.build();
    }

    public @Nullable ResourceLocation getRegistryId() {
        return QuasarParticles.registryAccess().registry(QuasarParticles.PARTICLE_DATA).map(registry -> registry.getKey(this)).orElse(null);
    }

//
//    @Deprecated
//    public void addInitModule(InitParticleModule module) {
//        initModules.add(module);
//    }
//
//    @Deprecated
//    public void addInitModules(InitParticleModule... modules) {
//        initModules.addAll(Arrays.asList(modules));
//    }
//
//    @Deprecated
//    public void addRenderModule(RenderParticleModule module) {
//        renderModules.add(module);
//    }
//
//    @Deprecated
//    public void addRenderModules(RenderParticleModule... modules) {
//        renderModules.addAll(Arrays.asList(modules));
//    }
//
//    @Deprecated
//    public void addUpdateModule(UpdateParticleModule module) {
//        updateModules.add(module);
//    }
//
//    @Deprecated
//    public void addUpdateModules(UpdateParticleModule... modules) {
//        updateModules.addAll(Arrays.asList(modules));
//    }
//
//    @Deprecated
//    public void addCollisionModule(CollisionParticleModule module) {
//        collisionModules.add(module);
//    }
//
//    @Deprecated
//    public void addCollisionModules(CollisionParticleModule... modules) {
//        collisionModules.addAll(Arrays.asList(modules));
//    }
//
//    @Deprecated
//    public void addForce(AbstractParticleForce force) {
//        forces.add(force);
//    }
//
//    @Deprecated
//    public void addForces(AbstractParticleForce... forces) {
//        this.forces.addAll(Arrays.asList(forces));
//    }
//
//    @Deprecated
//    public void addSubEmitter(ResourceLocation emitter) {
//        subEmitters.add(emitter);
//    }
//
//    @Deprecated
//    public void addSubEmitters(ResourceLocation... emitters) {
//        subEmitters.addAll(Arrays.asList(emitters));
//    }
}
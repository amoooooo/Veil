package foundry.veil.api.quasar.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import foundry.veil.Veil;
import foundry.veil.api.quasar.data.module.ModuleType;
import foundry.veil.api.quasar.data.module.ParticleModuleData;
import foundry.veil.api.quasar.data.module.collision.CollisionSubEmitterData;
import foundry.veil.api.quasar.data.module.collision.DieOnCollisionModuleData;
import foundry.veil.api.quasar.data.module.force.*;
import foundry.veil.api.quasar.data.module.init.*;
import foundry.veil.api.quasar.data.module.render.ColorParticleModuleData;
import foundry.veil.api.quasar.data.module.render.TrailParticleModuleData;
import foundry.veil.api.quasar.data.module.update.TickSizeParticleModuleData;
import foundry.veil.api.quasar.data.module.update.TickSubEmitterModuleData;
import foundry.veil.api.quasar.emitters.module.init.InitRandomRotationModuleData;
import foundry.veil.api.util.CodecUtil;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;

public class ParticleModuleTypeRegistry {
    public static final ResourceKey<Registry<ModuleType<?>>> INIT_MODULES_KEY = ResourceKey.createRegistryKey(Veil.veilPath("quasar/modules/init"));
    public static final ResourceKey<Registry<ModuleType<?>>> UPDATE_MODULES_KEY = ResourceKey.createRegistryKey(Veil.veilPath("quasar/modules/update"));
    public static final ResourceKey<Registry<ModuleType<?>>> RENDER_MODULES_KEY = ResourceKey.createRegistryKey(Veil.veilPath("quasar/modules/render"));

    private static final RegistrationProvider<ModuleType<?>> INIT_MODULES_PROVIDER = RegistrationProvider.get(INIT_MODULES_KEY, Veil.MODID);
    private static final RegistrationProvider<ModuleType<?>> UPDATE_MODULES_PROVIDER = RegistrationProvider.get(UPDATE_MODULES_KEY, Veil.MODID);
    private static final RegistrationProvider<ModuleType<?>> RENDER_MODULES_PROVIDER = RegistrationProvider.get(RENDER_MODULES_KEY, Veil.MODID);

    public static final Registry<ModuleType<?>> INIT_MODULES_REGISTRY = INIT_MODULES_PROVIDER.asVanillaRegistry();
    public static final Registry<ModuleType<?>> UPDATE_MODULES_REGISTRY = UPDATE_MODULES_PROVIDER.asVanillaRegistry();
    public static final Registry<ModuleType<?>> RENDER_MODULES_REGISTRY = RENDER_MODULES_PROVIDER.asVanillaRegistry();

    public static final Codec<ModuleType<?>> INIT_MODULE_CODEC = CodecUtil.registryOrLegacyCodec(INIT_MODULES_REGISTRY);
    public static final Codec<ModuleType<?>> UPDATE_MODULE_CODEC = CodecUtil.registryOrLegacyCodec(UPDATE_MODULES_REGISTRY);
    public static final Codec<ModuleType<?>> RENDER_MODULE_CODEC = CodecUtil.registryOrLegacyCodec(RENDER_MODULES_REGISTRY);

    // INIT
    public static final ModuleType<InitialVelocityModuleData> INITIAL_VELOCITY = registerInitModule("initial_velocity", InitialVelocityModuleData.CODEC);
    public static final ModuleType<ColorParticleModuleData> INIT_COLOR = registerInitModule("init_color", ColorParticleModuleData.CODEC);
    public static final ModuleType<InitSubEmitterModuleData> INIT_SUB_EMITTER = registerInitModule("init_sub_emitter", InitSubEmitterModuleData.CODEC);
    public static final ModuleType<InitSizeParticleModuleData> INIT_SIZE = registerInitModule("init_size", InitSizeParticleModuleData.CODEC);
    //    ModuleType<InitRandomColorParticleModule> INIT_RANDOM_COLOR = registerInitModule("init_random_color", InitRandomColorParticleModule.CODEC);
    public static final ModuleType<InitRandomRotationModuleData> INIT_RANDOM_ROTATION = registerInitModule("init_random_rotation", InitRandomRotationModuleData.CODEC);
    public static final ModuleType<LightModuleData> LIGHT = registerInitModule("light", LightModuleData.CODEC);
    public static final ModuleType<BlockParticleModuleData> BLOCK_PARTICLE = registerInitModule("block", BlockParticleModuleData.CODEC);

    // RENDER
    public static final ModuleType<TrailParticleModuleData> TRAIL = registerRenderModule("trail", TrailParticleModuleData.CODEC);
    public static final ModuleType<ColorParticleModuleData> COLOR = registerRenderModule("color", ColorParticleModuleData.CODEC);
    //    ModuleType<ColorOverTimeParticleModule> COLOR_OVER_LIFETIME = registerRenderModule("color_over_lifetime", ColorOverTimeParticleModule.CODEC);
    //    ModuleType<ColorOverVelocityParticleModule> COLOR_OVER_VELOCITY = registerRenderModule("color_over_velocity", ColorOverVelocityParticleModule.CODEC);

    // UPDATE
    public static final ModuleType<TickSizeParticleModuleData> TICK_SIZE = registerUpdateModule("tick_size", TickSizeParticleModuleData.CODEC);
    public static final ModuleType<TickSubEmitterModuleData> TICK_SUB_EMITTER = registerUpdateModule("tick_sub_emitter", TickSubEmitterModuleData.CODEC);
    // UPDATE - COLLISION
    public static final ModuleType<DieOnCollisionModuleData> DIE_ON_COLLISION = registerUpdateModule("die_on_collision", DieOnCollisionModuleData.CODEC);
    public static final ModuleType<CollisionSubEmitterData> SUB_EMITTER_COLLISION = registerUpdateModule("sub_emitter_collision", CollisionSubEmitterData.CODEC);
    //    ModuleType<BounceParticleModule> BOUNCE = registerUpdateModule("bounce", BounceParticleModule.CODEC);
    // UPDATE - FORCES
    public static final ModuleType<GravityForceData> GRAVITY = registerUpdateModule("gravity", GravityForceData.CODEC);
    public static final ModuleType<VortexForceData> VORTEX = registerUpdateModule("vortex", VortexForceData.CODEC);
    public static final ModuleType<PointAttractorForceData> POINT_ATTRACTOR = registerUpdateModule("point_attractor", PointAttractorForceData.CODEC);
    public static final ModuleType<VectorFieldForceData> VECTOR_FIELD = registerUpdateModule("vector_field", VectorFieldForceData.CODEC);
    public static final ModuleType<DragForceData> DRAG = registerUpdateModule("drag", DragForceData.CODEC);
    public static final ModuleType<WindForceData> WIND = registerUpdateModule("wind", WindForceData.CODEC);
    public static final ModuleType<PointForceData> POINT = registerUpdateModule("point_force", PointForceData.CODEC);

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    private static <T extends ParticleModuleData> ModuleType<T> registerUpdateModule(String name, Codec<T> codec) {
        ModuleType<T> type = () -> codec;
        UPDATE_MODULES_PROVIDER.register(name, () -> type);
        return type;
    }

    private static <T extends ParticleModuleData> ModuleType<T> registerRenderModule(String name, Codec<T> codec) {
        ModuleType<T> type = () -> codec;
        RENDER_MODULES_PROVIDER.register(name, () -> type);
        return type;
    }

    private static <T extends ParticleModuleData> ModuleType<T> registerInitModule(String name, Codec<T> codec) {
        ModuleType<T> type = () -> codec;
        INIT_MODULES_PROVIDER.register(name, () -> type);
        return type;
    }
}
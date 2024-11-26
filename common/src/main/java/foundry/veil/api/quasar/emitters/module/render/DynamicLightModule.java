package foundry.veil.api.quasar.emitters.module.render;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.quasar.data.module.init.LightModuleData;
import foundry.veil.api.quasar.emitters.module.RenderParticleModule;
import foundry.veil.api.quasar.emitters.module.UpdateParticleModule;
import foundry.veil.api.quasar.particle.QuasarParticle;
import net.minecraft.util.Mth;
import org.joml.Vector4f;

public class DynamicLightModule implements UpdateParticleModule, RenderParticleModule {

    private final LightModuleData data;
    private float brightness;
    private float lastRadius;
    private float radius;

    private final Vector4f lastColor;
    private final Vector4f color;
    private final Vector4f renderColor;
    private float lastBrightness;
    private PointLight light;

    private final boolean constantColor;
    private final boolean constantBrightness;
    private final boolean constantRadius;

    public DynamicLightModule(LightModuleData data) {
        this.data = data;

        this.constantColor = data.color().isConstant();
        this.constantBrightness = this.constantColor && data.brightness().isConstant();
        this.constantRadius = data.radius().isConstant();

        this.lastColor = new Vector4f(1.0F);
        this.color = new Vector4f(1.0F);
        this.renderColor = new Vector4f(1.0F);
        this.light = null;

        if (this.constantColor) {
            data.color().getColor(0.0F, this.color);
            this.lastColor.set(this.color);
            this.renderColor.set(this.color);
        }
    }

    @Override
    public void update(QuasarParticle particle) {
        if (!this.constantColor) {
            this.lastColor.set(this.color);
            this.data.color().getColor((float) particle.getAge() / (float) particle.getLifetime(), this.color);
        }
        if (!this.constantBrightness) {
            this.lastBrightness = this.brightness;
            this.brightness = particle.getEnvironment().safeResolve(this.data.brightness());
        }
        if (!this.constantRadius) {
            this.lastRadius = this.radius;
            this.radius = particle.getEnvironment().safeResolve(this.data.radius());
        }

        float brightness = this.brightness * this.color.w;
        if (this.color.lengthSquared() < 0.1 && brightness < 0.1) {
            this.onRemove();
            return;
        }

        if (this.light == null) {
            this.light = new PointLight();
            if (this.constantColor) {
                this.light.setColor(this.color.x, this.color.y, this.color.z);
            }
            if (this.constantBrightness) {
                this.light.setBrightness(this.brightness * this.renderColor.w);
            }
            if (this.constantRadius) {
                this.light.setBrightness(this.radius);
            }
            VeilRenderSystem.renderer().getLightRenderer().addLight(this.light);
        }
        this.lastBrightness = brightness;
    }

    @Override
    public void render(QuasarParticle particle, float partialTicks) {
        if (this.light == null) {
            return;
        }

        this.light.setPosition(particle.getRenderData().getRenderPosition());

        if (!this.constantColor) {
            this.lastColor.lerp(this.color, partialTicks, this.renderColor);
            this.light.setColor(this.renderColor.x, this.renderColor.y, this.renderColor.z);
        }
        if (!this.constantBrightness) {
            this.light.setBrightness(Mth.lerp(partialTicks, this.lastBrightness, this.brightness) * this.renderColor.w);
        }
        if (!this.constantRadius) {
            this.light.setRadius(Mth.lerp(partialTicks, this.lastRadius, this.radius));
        }
    }

    @Override
    public void onRemove() {
        if (this.light != null) {
            VeilRenderSystem.renderer().getLightRenderer().removeLight(this.light);
            this.light = null;
        }
    }
}

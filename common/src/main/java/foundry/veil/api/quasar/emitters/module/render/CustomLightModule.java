package foundry.veil.api.quasar.emitters.module.render;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.quasar.emitters.module.RenderParticleModule;
import foundry.veil.api.quasar.emitters.module.UpdateParticleModule;
import foundry.veil.api.quasar.particle.QuasarParticle;
import org.joml.Vector4f;

public class CustomLightModule implements UpdateParticleModule, RenderParticleModule {

    private final Vector4f lastColor;
    private final Vector4f color;
    private final Vector4f renderColor;
    private float brightness;
    private float radius;
    private PointLight light;

    public CustomLightModule() {
        this.lastColor = new Vector4f(1.0F);
        this.color = new Vector4f(1.0F);
        this.renderColor = new Vector4f(1.0F);
        this.light = null;
    }

    @Override
    public void update(QuasarParticle particle) {
        this.lastColor.set(this.color);
        float brightness = this.brightness * this.color.w;

        if (this.color.lengthSquared() < 0.1 && brightness < 0.1) {
            this.onRemove();
        } else {
            if (this.light == null) {
                this.light = new PointLight().setRadius(this.radius);
                VeilRenderSystem.renderer().getLightRenderer().addLight(this.light);
            }
            this.light.setColor(this.color.x, this.color.y, this.color.z);
            this.light.setBrightness(this.brightness * this.color.w);
        }
    }

    @Override
    public void render(QuasarParticle particle, float partialTicks) {
        if (this.light == null) {
            return;
        }

        this.light.setPosition(particle.getRenderData().getRenderPosition());
        this.lastColor.lerp(this.color, partialTicks, this.renderColor);
        this.light.setColor(this.renderColor.x, this.renderColor.y, this.renderColor.z);
        this.light.setBrightness(this.brightness * this.renderColor.w);
    }

    @Override
    public void onRemove() {
        if (this.light != null) {
            VeilRenderSystem.renderer().getLightRenderer().removeLight(this.light);
            this.light = null;
        }
    }

    public Vector4f getColor() {
        return this.color;
    }

    public float getBrightness() {
        return this.brightness;
    }

    public float getRadius() {
        return this.radius;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
}

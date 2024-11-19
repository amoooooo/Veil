package foundry.veil.api.client.render.shader.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.resources.ResourceLocation;

/**
 * Source of a shader texture using a framebuffer.
 *
 * @param name    The location of the framebuffer
 * @param sampler The sampler to use. Ignored if {@link #depth} is <code>true</code>
 * @param depth   Whether to sample the depth texture or not
 * @author Ocelot
 */
public record FramebufferSource(ResourceLocation name,
                                int sampler,
                                boolean depth) implements ShaderTextureSource {

    public static final MapCodec<FramebufferSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(source -> source.name.toString()),
            Codec.INT.optionalFieldOf("sampler", 0).forGetter(FramebufferSource::sampler)
    ).apply(instance, (name, sampler) -> {
        boolean depth = name.endsWith(":depth");
        String path = depth ? name.substring(0, name.length() - 6) : name;
        ResourceLocation location = name.contains(":") ? ResourceLocation.parse(path) : ResourceLocation.fromNamespaceAndPath("temp", name);
        return new FramebufferSource(location, depth ? 0 : sampler, depth);
    }));

    @Override
    public int getId(Context context) {
        AdvancedFbo framebuffer = context.getFramebuffer(this.name);
        if (framebuffer == null) {
            return 0;
        }

        if (this.depth) {
            return framebuffer.isDepthTextureAttachment() ? framebuffer.getDepthTextureAttachment().getId() : 0;
        }
        return framebuffer.isColorTextureAttachment(this.sampler) ? framebuffer.getColorTextureAttachment(this.sampler).getId() : 0;
    }

    @Override
    public Type getType() {
        return Type.FRAMEBUFFER;
    }
}

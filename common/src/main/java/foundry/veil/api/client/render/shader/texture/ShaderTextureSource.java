package foundry.veil.api.client.render.shader.texture;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.util.EnumCodec;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Source for shader textures. This allows resource location textures as well as other special types.
 *
 * @author Ocelot
 */
public sealed interface ShaderTextureSource permits LocationSource, FramebufferSource {

    Codec<ShaderTextureSource> CODEC = Codec.either(ResourceLocation.CODEC,
                    Type.CODEC.<ShaderTextureSource>dispatch(ShaderTextureSource::getType, Type::getCodec))
            .xmap(either -> either.map(LocationSource::new, right -> right),
                    source -> source instanceof LocationSource(
                            ResourceLocation location
                    ) ? Either.left(location) : Either.right(source));

    Context GLOBAL_CONTEXT = name -> VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(name);

    /**
     * Retrieves the id of this texture based on context.
     *
     * @param context The context to use
     * @return The id of the texture to bind
     */
    int getId(Context context);

    /**
     * @return The type of shader texture this is
     */
    Type getType();

    /**
     * Types of post textures that can be used.
     *
     * @author Ocelot
     */
    enum Type {
        LOCATION(LocationSource.CODEC),
        FRAMEBUFFER(FramebufferSource.CODEC);

        private final MapCodec<? extends ShaderTextureSource> codec;

        public static final Codec<Type> CODEC = EnumCodec.<Type>builder("texture type").values(Type.class).build();

        Type(MapCodec<? extends ShaderTextureSource> codec) {
            this.codec = codec;
        }

        /**
         * @return The codec for this specific type
         */
        public MapCodec<? extends ShaderTextureSource> getCodec() {
            return this.codec;
        }
    }

//    enum Filter {
//        DEFAULT,
//        LINEAR,
//        NEAREST,
//        LINEAR_MIPMAP_LINEAR,
//        LINEAR_MIPMAP_NEAREST,
//        NEAREST_MIPMAP_LINEAR,
//        NEAREST_MIPMAP_NEAREST;
//
//        public static final Codec<Filter> CODEC = EnumCodec.<Filter>builder("texture filter").values(Filter.class).build();
//    }

    /**
     * Context for applying shader textures.
     *
     * @author Ocelot
     */
    @FunctionalInterface
    interface Context {

        /**
         * Retrieves a framebuffer by id.
         *
         * @param name The name of the framebuffer to retrieve
         * @return The framebuffer with that id or <code>null</code> if it was not found
         */
        @Nullable AdvancedFbo getFramebuffer(ResourceLocation name);

        /**
         * Retrieves a texture by id.
         *
         * @param name The name of the texture to retrieve
         * @return The texture with that id or the missing texture if it was not found
         */
        default int getTexture(ResourceLocation name) {
            if (Veil.MODID.equals(name.getNamespace()) && name.getPath().startsWith("dynamic_buffer")) {
                DynamicBufferManger bufferManger = VeilRenderSystem.renderer().getDynamicBufferManger();
                if (name.equals(VeilRenderer.ALBEDO_BUFFER_TEXTURE)) {
                    return bufferManger.getBufferTexture(DynamicBufferType.ALBEDO);
                }
                if (name.equals(VeilRenderer.NORMAL_BUFFER_TEXTURE)) {
                    return bufferManger.getBufferTexture(DynamicBufferType.NORMAL);
                }
                if (name.equals(VeilRenderer.LIGHT_UV_BUFFER_TEXTURE)) {
                    return bufferManger.getBufferTexture(DynamicBufferType.LIGHT_UV);
                }
                if (name.equals(VeilRenderer.LIGHT_COLOR_BUFFER_TEXTURE)) {
                    return bufferManger.getBufferTexture(DynamicBufferType.LIGHT_COLOR);
                }
                if (name.equals(VeilRenderer.DEBUG_BUFFER_TEXTURE)) {
                    return bufferManger.getBufferTexture(DynamicBufferType.DEBUG);
                }
            }
            return Minecraft.getInstance().getTextureManager().getTexture(name).getId();
        }
    }
}

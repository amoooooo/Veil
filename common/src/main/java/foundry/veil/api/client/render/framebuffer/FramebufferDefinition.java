package foundry.veil.api.client.render.framebuffer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.molang.MolangExpressionCodec;
import foundry.veil.api.molang.VeilMolang;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Represents a framebuffer definition that can be turned into a real framebuffer.
 *
 * @param width        The width of the framebuffer
 * @param height       The height of the framebuffer
 * @param colorBuffers The color attachments to add
 * @param depthBuffer  The depth attachment to use or <code>null</code> to not add a depth buffer
 * @param autoClear    Whether the framebuffer should be cleared automatically at the start of the next frame
 * @author Ocelot
 */
public record FramebufferDefinition(MolangExpression width,
                                    MolangExpression height,
                                    FramebufferAttachmentDefinition[] colorBuffers,
                                    @Nullable FramebufferAttachmentDefinition depthBuffer,
                                    boolean autoClear) {

    public static final MolangExpression DEFAULT_WIDTH;
    public static final MolangExpression DEFAULT_HEIGHT;

    static {
        try {
            DEFAULT_WIDTH = VeilMolang.get().compile("q.screen_width");
            DEFAULT_HEIGHT = VeilMolang.get().compile("q.screen_height");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Codec<Optional<FramebufferAttachmentDefinition>> DEPTH_CODEC =
            Codec.either(FramebufferAttachmentDefinition.DEPTH_CODEC, Codec.BOOL)
                    .xmap(either -> either.map(Optional::of,
                                    compact -> compact ?
                                            Optional.of(new FramebufferAttachmentDefinition(
                                                    FramebufferAttachmentDefinition.Type.TEXTURE,
                                                    FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT,
                                                    FramebufferAttachmentDefinition.DataType.FLOAT,
                                                    true,
                                                    false,
                                                    1,
                                                    null)) :
                                            Optional.empty()),
                            optional -> {
                                if (optional.isEmpty()) {
                                    return Either.right(false);
                                }
                                FramebufferAttachmentDefinition definition = optional.get();
                                if (definition.isCompactDepthAttachment()) {
                                    return Either.right(true);
                                }
                                return Either.left(definition);
                            });

    private static final Codec<FramebufferDefinition> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MolangExpressionCodec.CODEC.optionalFieldOf("width", DEFAULT_WIDTH)
                    .forGetter(FramebufferDefinition::width),
            MolangExpressionCodec.CODEC.optionalFieldOf("height", DEFAULT_HEIGHT)
                    .forGetter(FramebufferDefinition::height),
            FramebufferAttachmentDefinition.COLOR_CODEC.listOf().fieldOf("color_buffers")
                    .flatXmap(FramebufferDefinition::colorSizeCheck, FramebufferDefinition::colorSizeCheck)
                    .forGetter(definition -> Arrays.asList(definition.colorBuffers)),
            FramebufferDefinition.DEPTH_CODEC.fieldOf("depth")
                    .forGetter(definition -> Optional.ofNullable(definition.depthBuffer)),
            Codec.BOOL.optionalFieldOf("autoClear", true)
                    .forGetter(FramebufferDefinition::autoClear)
    ).apply(instance, (width, height, colorBuffers, depthBuffer, autoClear) ->
            new FramebufferDefinition(width,
                    height,
                    colorBuffers.toArray(FramebufferAttachmentDefinition[]::new),
                    depthBuffer.orElse(null),
                    autoClear)));

    private static final Codec<FramebufferDefinition> COMPACT_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    MolangExpressionCodec.CODEC.optionalFieldOf("width", DEFAULT_WIDTH)
                            .forGetter(FramebufferDefinition::width),
                    MolangExpressionCodec.CODEC.optionalFieldOf("height", DEFAULT_HEIGHT)
                            .forGetter(FramebufferDefinition::height),
                    FramebufferAttachmentDefinition.Type.CODEC
                            .optionalFieldOf("type", FramebufferAttachmentDefinition.Type.TEXTURE)
                            .forGetter(definition -> definition.colorBuffers[0].type()),
                    FramebufferAttachmentDefinition.Format.CODEC
                            .optionalFieldOf("format", FramebufferAttachmentDefinition.Format.RGBA8)
                            .forGetter(definition -> definition.colorBuffers[0].format()),
                    FramebufferAttachmentDefinition.DataType.CODEC
                            .optionalFieldOf("dataType", FramebufferAttachmentDefinition.DataType.UNSIGNED_BYTE)
                            .forGetter(definition -> definition.colorBuffers[0].dataType()),
                    Codec.BOOL.optionalFieldOf("linear", false)
                            .forGetter(definition -> definition.colorBuffers[0].linear()),
                    Codec.INT.optionalFieldOf("levels", 1)
                            .forGetter(definition -> definition.colorBuffers[0].levels()),
                    Codec.STRING.optionalFieldOf("name")
                            .forGetter(definition -> Optional.ofNullable(definition.colorBuffers[0].name())),
                    FramebufferDefinition.DEPTH_CODEC.fieldOf("depth")
                            .forGetter(definition -> Optional.ofNullable(definition.depthBuffer)),
                    Codec.BOOL.optionalFieldOf("autoClear", true)
                            .forGetter(FramebufferDefinition::autoClear)
            ).apply(instance, (width, height, type, format, dataType, linear, levels, name, depth, autoClear) ->
                    new FramebufferDefinition(width,
                            height,
                            new FramebufferAttachmentDefinition[]{
                                    new FramebufferAttachmentDefinition(type,
                                            format,
                                            dataType,
                                            false,
                                            linear,
                                            levels,
                                            name.orElse(null))
                            },
                            depth.orElse(null),
                            autoClear)));

    public static final Codec<FramebufferDefinition> CODEC = Codec.either(FramebufferDefinition.FULL_CODEC, FramebufferDefinition.COMPACT_CODEC)
            .xmap(either -> either.map(left -> left, right -> right),
                    definition -> definition.colorBuffers.length == 1 ? Either.right(definition) : Either.left(definition));

    private static DataResult<List<FramebufferAttachmentDefinition>> colorSizeCheck(
            List<FramebufferAttachmentDefinition> definitions) {
        if (definitions.isEmpty()) {
            return DataResult.error(() -> "There must be at least 1 color buffer");
        }
        return DataResult.success(definitions);
    }

    /**
     * Creates a new framebuffer.
     */
    public FramebufferDefinition {
        if (colorBuffers.length < 1) {
            throw new IllegalArgumentException("At least 1 color buffer must be defined");
        }
    }

    /**
     * Creates a new builder from this framebuffer.
     *
     * @param screenWidth  The width of the screen
     * @param screenHeight The height of the screen
     * @return A new {@link AdvancedFbo.Builder} that can be turned into a framebuffer.
     * All defined attachments are created and added to the builder
     */
    public AdvancedFbo.Builder createBuilder(int screenWidth, int screenHeight) {
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("screen_width", screenWidth)
                .setQuery("screen_height", screenHeight)
                .create();
        return this.createBuilder(runtime);
    }

    /**
     * Creates a new builder from this framebuffer.
     *
     * @param environment The environment to evaluate the size in
     * @return A new {@link AdvancedFbo.Builder} that can be turned into a framebuffer.
     * All defined attachments are created and added to the builder
     */
    public AdvancedFbo.Builder createBuilder(MolangEnvironment environment) {
        int width = (int) environment.safeResolve(this.width);
        int height = (int) environment.safeResolve(this.height);
        Validate.inclusiveBetween(1, VeilRenderSystem.maxFramebufferWidth(), width, "width must be between 1 and " + VeilRenderSystem.maxFramebufferWidth());
        Validate.inclusiveBetween(1, VeilRenderSystem.maxFramebufferHeight(), height, "height must be between 1 and " + VeilRenderSystem.maxFramebufferHeight());
        AdvancedFbo.Builder builder = AdvancedFbo.withSize(width, height);

        for (FramebufferAttachmentDefinition definition : this.colorBuffers) {
            if (definition.type() == FramebufferAttachmentDefinition.Type.RENDER_BUFFER) {
                builder.setLevels(definition.levels())
                        .setFormat(definition.format())
                        .addColorRenderBuffer();
            } else {
                builder.setLevels(definition.levels())
                        .setLinear(definition.linear())
                        .setName(definition.name())
                        .setFormat(definition.format())
                        .addColorTextureBuffer(definition.dataType().getId());
            }
        }

        if (this.depthBuffer != null) {
            if (this.depthBuffer.type() == FramebufferAttachmentDefinition.Type.RENDER_BUFFER) {
                builder.setLevels(this.depthBuffer.levels())
                        .setDepthRenderBuffer();
            } else {
                builder.setLevels(this.depthBuffer.levels())
                        .setLinear(this.depthBuffer.linear())
                        .setName(this.depthBuffer.name())
                        .setFormat(this.depthBuffer.format())
                        .setDepthTextureBuffer(this.depthBuffer.dataType().getId());
            }
        }

        return builder;
    }
}

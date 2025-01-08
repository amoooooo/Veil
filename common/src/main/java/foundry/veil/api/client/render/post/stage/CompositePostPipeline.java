package foundry.veil.api.client.render.post.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import foundry.veil.api.client.registry.PostPipelineStageRegistry;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferDefinition;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.shader.texture.ShaderTextureSource;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import net.minecraft.resources.ResourceLocation;
import org.joml.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A pipeline that runs all child pipelines in order.
 */
public class CompositePostPipeline implements PostPipeline {

    private static final Codec<Map<ResourceLocation, FramebufferDefinition>> FRAMEBUFFER_CODEC = Codec.unboundedMap(
            ResourceLocation.CODEC.xmap(name -> ResourceLocation.fromNamespaceAndPath("temp", name.getPath()), loc -> loc),
            FramebufferDefinition.CODEC);
    public static final Codec<CompositePostPipeline> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PostPipeline.CODEC.listOf().fieldOf("stages").forGetter(pipeline -> Arrays.asList(pipeline.getStages())),
            Codec.unboundedMap(Codec.STRING, ShaderTextureSource.CODEC)
                    .optionalFieldOf("textures", Collections.emptyMap())
                    .forGetter(CompositePostPipeline::getTextures),
            CompositePostPipeline.FRAMEBUFFER_CODEC
                    .optionalFieldOf("framebuffers", Collections.emptyMap())
                    .forGetter(CompositePostPipeline::getFramebuffers),
            DynamicBufferType.PACKED_LIST_CODEC.optionalFieldOf("dynamicBuffers", 0).forGetter(CompositePostPipeline::getDynamicBuffersMask),
            Codec.INT.optionalFieldOf("priority", 1000).forGetter(CompositePostPipeline::getPriority),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(CompositePostPipeline::isReplace)
    ).apply(instance, (pipelines, textures, framebuffers, dynamicBuffers, priority, replace) -> new CompositePostPipeline(pipelines.toArray(PostPipeline[]::new), textures, framebuffers, dynamicBuffers, priority, replace)));

    private final PostPipeline[] stages;
    private final Map<String, ShaderTextureSource> textures;
    private final Map<ResourceLocation, FramebufferDefinition> framebufferDefinitions;
    private final Map<ResourceLocation, AdvancedFbo> framebuffers;
    private final DynamicBufferType[] dynamicBuffers;
    private final int dynamicBuffersMask;
    private final int priority;
    private final boolean replace;

    private int screenWidth = -1;
    private int screenHeight = -1;

    private CompositePostPipeline(PostPipeline[] stages, Map<String, ShaderTextureSource> textures, Map<ResourceLocation, FramebufferDefinition> framebufferDefinitions, int dynamicBuffers, int priority, boolean replace) {
        this.stages = stages;
        this.textures = Collections.unmodifiableMap(textures);
        this.framebufferDefinitions = Collections.unmodifiableMap(framebufferDefinitions);
        this.framebuffers = new HashMap<>();
        this.dynamicBuffers = DynamicBufferType.decode(dynamicBuffers);
        this.dynamicBuffersMask = dynamicBuffers;
        this.priority = priority;
        this.replace = replace;
    }

    /**
     * Creates a new composite post pipeline that runs all child pipelines in order.
     *
     * @param stages                 The pipelines to run in order
     * @param textures               The textures to bind globally
     * @param framebufferDefinitions The definitions of framebuffers to create for use in the stages
     * @param dynamicBuffers         A bit field of all enabled dynamic buffers for this pipeline
     */
    public CompositePostPipeline(PostPipeline[] stages, Map<String, ShaderTextureSource> textures, Map<ResourceLocation, FramebufferDefinition> framebufferDefinitions, int dynamicBuffers) {
        this(stages, textures, framebufferDefinitions, dynamicBuffers, 1000, false);
    }

    @Override
    public void apply(Context context) {
        AdvancedFbo main = context.getDrawFramebuffer();
        if (this.screenWidth != main.getWidth() || this.screenHeight != main.getHeight()) {
            this.screenWidth = main.getWidth();
            this.screenHeight = main.getHeight();
            this.framebuffers.values().forEach(AdvancedFbo::free);
            this.framebuffers.clear();

            MolangRuntime runtime = MolangRuntime.runtime()
                    .setQuery("screen_width", this.screenWidth)
                    .setQuery("screen_height", this.screenHeight)
                    .create();
            this.framebufferDefinitions.forEach((name, definition) -> this.framebuffers.put(name, definition.createBuilder(runtime).build(true)));
        }

        this.framebuffers.forEach(context::setFramebuffer);
        this.textures.forEach((name, texture) -> context.setSampler(name, texture.getId(context)));
        for (DynamicBufferType buffer : this.dynamicBuffers) {
            context.setSampler(buffer.getSourceName(), VeilRenderSystem.renderer().getDynamicBufferManger().getBufferTexture(buffer));
        }
        for (PostPipeline pipeline : this.stages) {
            pipeline.apply(context);
        }
    }

    @Override
    public void free() {
        for (PostPipeline pipeline : this.stages) {
            pipeline.free();
        }
        this.framebuffers.values().forEach(AdvancedFbo::free);
        this.framebuffers.clear();
    }

    @Override
    public PostPipelineStageRegistry.PipelineType<? extends PostPipeline> getType() {
        throw new UnsupportedOperationException("Composite pipelines cannot be encoded");
    }

    @Override
    public boolean hasUniform(CharSequence name) {
        for (PostPipeline pipeline : this.stages) {
            if (pipeline.hasUniform(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasUniformBlock(CharSequence name) {
        for (PostPipeline pipeline : this.stages) {
            if (pipeline.hasUniformBlock(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasStorageBlock(CharSequence name) {
        for (PostPipeline pipeline : this.stages) {
            if (pipeline.hasStorageBlock(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setUniformBlock(CharSequence name, int binding) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setUniformBlock(name, binding);
        }
    }

    @Override
    public void setStorageBlock(CharSequence name, int binding) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setStorageBlock(name, binding);
        }
    }

    @Override
    public void setFloat(CharSequence name, float value) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setFloat(name, value);
        }
    }

    @Override
    public void setVector(CharSequence name, float x, float y) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVector(name, x, y);
        }
    }

    @Override
    public void setVector(CharSequence name, float x, float y, float z) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVector(name, x, y, z);
        }
    }

    @Override
    public void setVector(CharSequence name, float x, float y, float z, float w) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVector(name, x, y, z, w);
        }
    }

    @Override
    public void setInt(CharSequence name, int value) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setInt(name, value);
        }
    }

    @Override
    public void setVectorI(CharSequence name, int x, int y) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectorI(name, x, y);
        }
    }

    @Override
    public void setVectorI(CharSequence name, int x, int y, int z) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectorI(name, x, y, z);
        }
    }

    @Override
    public void setVectorI(CharSequence name, int x, int y, int z, int w) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectorI(name, x, y, z, w);
        }
    }

    @Override
    public void setFloats(CharSequence name, float... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setFloats(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector2fc... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector3fc... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector4fc... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectors(name, values);
        }
    }

    @Override
    public void setInts(CharSequence name, int... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setInts(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector2ic... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector3ic... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectors(name, values);
        }
    }

    @Override
    public void setVectors(CharSequence name, Vector4ic... values) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setVectors(name, values);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix2fc value, boolean transpose) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix3fc value, boolean transpose) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix3x2fc value, boolean transpose) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix4fc value, boolean transpose) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setMatrix(name, value, transpose);
        }
    }

    @Override
    public void setMatrix(CharSequence name, Matrix4x3fc value, boolean transpose) {
        for (PostPipeline pipeline : this.stages) {
            pipeline.setMatrix(name, value, transpose);
        }
    }

    /**
     * @return The stages run in this pipeline
     */
    public PostPipeline[] getStages() {
        return this.stages;
    }

    /**
     * @return The globally bound textures for the child stages to access
     */
    public Map<String, ShaderTextureSource> getTextures() {
        return this.textures;
    }

    /**
     * @return The framebuffers created for the child stages to access
     */
    public Map<ResourceLocation, FramebufferDefinition> getFramebuffers() {
        return this.framebufferDefinitions;
    }

    /**
     * @return The dynamic buffers this pipeline uses
     */
    public DynamicBufferType[] getDynamicBuffers() {
        return this.dynamicBuffers;
    }

    /**
     * @return The mask of dynamic buffers this pipeline uses
     */
    public int getDynamicBuffersMask() {
        return this.dynamicBuffersMask;
    }

    /**
     * @return The priority of this pipeline
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * @return Whether this stage will replace all stages with a higher priority
     */
    public boolean isReplace() {
        return this.replace;
    }
}

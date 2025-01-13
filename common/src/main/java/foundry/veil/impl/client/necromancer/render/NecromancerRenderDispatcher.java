package foundry.veil.impl.client.necromancer.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.necromancer.Skeleton;
import foundry.veil.api.client.necromancer.render.NecromancerRenderer;
import foundry.veil.api.client.necromancer.render.Skin;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.mesh.VertexArray;
import foundry.veil.api.client.render.shader.definition.DynamicShaderBlock;
import foundry.veil.api.client.render.shader.definition.ShaderBlock;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static org.lwjgl.opengl.ARBDirectStateAccess.glNamedBufferData;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER;

@ApiStatus.Internal
public class NecromancerRenderDispatcher {

    private static final Batched BATCHED = new Batched();
    private static final Immediate IMMEDIATE = new Immediate();

    private static DynamicShaderBlock<?> boneBlock;
    private static int boneBuffer;
    private static int instancedBuffer;
    private static boolean drawing;

    public static void begin() {
        drawing = true;
        BATCHED.begin();
    }

    public static void end() {
        BATCHED.end();
        drawing = false;
    }

    public static void delete() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glDeleteBuffers(stack.ints(boneBuffer, instancedBuffer));
        }
        boneBlock = null;
        boneBuffer = 0;
        instancedBuffer = 0;
    }

    public static NecromancerRenderer getRenderer() {
        return drawing ? BATCHED : IMMEDIATE;
    }

    private static void updateBlockSize(int skeletonCount, int dataSize) {
        if (boneBlock == null) {
            boneBuffer = GlStateManager._glGenBuffers();
            boneBlock = ShaderBlock.wrapper(ShaderBlock.BufferBinding.UNIFORM, boneBuffer);
        }

        long minSize = (long) skeletonCount * dataSize;
        if (boneBlock.getSize() < minSize) {
            boneBlock.setSize(minSize);
            VeilRenderSystem.renderer().getShaderDefinitions().set("NecromancerBoneCount", Integer.toString(skeletonCount));
            if (VeilRenderSystem.directStateAccessSupported()) {
                glNamedBufferData(boneBuffer, minSize, GL_DYNAMIC_DRAW);
            } else {
                glBindBuffer(GL_UNIFORM_BUFFER, boneBuffer);
                glBufferData(GL_UNIFORM_BUFFER, minSize, GL_DYNAMIC_DRAW);
                glBindBuffer(GL_UNIFORM_BUFFER, 0);
            }
        }
    }

    private static abstract class RendererImpl implements NecromancerRenderer {

        protected int overlay;
        protected int light;
        protected int r;
        protected int g;
        protected int b;
        protected int a;

        @Override
        public void setUv1(int u, int v) {
            this.overlay = (u & 15) << 4 | v & 15;
        }

        @Override
        public void setUv2(int u, int v) {
            this.light = (u & 15) << 4 | v & 15;
        }

        @Override
        public void setColor(float r, float g, float b, float a) {
            this.r = (int) (r * 255.0) & 0xFF;
            this.g = (int) (g * 255.0) & 0xFF;
            this.b = (int) (b * 255.0) & 0xFF;
            this.a = (int) (a * 255.0) & 0xFF;
        }

        @Override
        public void setColor(int color) {
            this.r = (color >> 16) & 0xFF;
            this.g = (color >> 8) & 0xFF;
            this.b = color & 0xFF;
            this.a = (color >> 24) & 0xFF;
        }

        @Override
        public void reset() {
            this.overlay = 10;
            this.light = 255;
            this.r = 255;
            this.g = 255;
            this.b = 255;
            this.a = 255;
        }
    }

    public static class Immediate extends RendererImpl {

        @Override
        public void draw(RenderType renderType, Skeleton skeleton, Skin skin, float partialTicks) {

        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(renderType);
        }
    }

    public static class Batched extends RendererImpl {

        private final List<ByteBufferBuilder> bufferBuilderList;
        private final Int2ObjectMap<SkeletonBatch> skeletonBatches;
        private final Map<RenderType, BufferBuilder> buffers;

        private int bufferIndex;

        public Batched() {
            this.bufferBuilderList = new ObjectArrayList<>();
            this.skeletonBatches = new Int2ObjectArrayMap<>();
            this.buffers = new Object2ObjectArrayMap<>();
        }

        @Override
        public void draw(RenderType renderType, Skeleton skeleton, Skin skin, float partialTicks) {
            SkeletonBatch batch = this.skeletonBatches.computeIfAbsent(skin.hashCode() * 31 + renderType.hashCode(), unused -> new SkeletonBatch(renderType, skin));
            batch.add(skeleton, this.overlay, this.light, this.r, this.g, this.b, this.a, partialTicks);
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            BufferBuilder buffer = this.buffers.get(renderType);
            if (buffer != null) {
                return buffer;
            }

            if (this.bufferIndex >= this.bufferBuilderList.size()) {
                ByteBufferBuilder builder = new ByteBufferBuilder(renderType.bufferSize());
                this.bufferBuilderList.add(builder);
                buffer = new BufferBuilder(builder, renderType.mode(), renderType.format());
            } else {
                buffer = new BufferBuilder(this.bufferBuilderList.get(this.bufferIndex), renderType.mode(), renderType.format());
            }

            this.bufferIndex++;
            this.buffers.put(renderType, buffer);
            return buffer;
        }

        public void begin() {
            this.bufferIndex = 0;
            this.reset();
        }

        public void end() {
            for (SkeletonBatch batch : this.skeletonBatches.values()) {
                batch.render();
            }
            this.skeletonBatches.clear();

            for (Map.Entry<RenderType, BufferBuilder> entry : this.buffers.entrySet()) {
                try (MeshData data = entry.getValue().build()) {
                    if (data != null) {
                        entry.getKey().draw(data);
                    }
                }
            }
            this.buffers.clear();

            ListIterator<ByteBufferBuilder> iterator = this.bufferBuilderList.listIterator(this.bufferIndex);
            while (iterator.hasNext()) {
                iterator.next().close();
                iterator.remove();
            }
        }
    }

    private static class SkeletonBatch {

        private final RenderType renderType;
        private final Skin skin;
        private final List<Skeleton> skeletons;
        private final FloatList partialTicks;

        private ByteBuffer instancedData;

        private SkeletonBatch(RenderType renderType, Skin skin) {
            this.renderType = renderType;
            this.skin = skin;
            this.skeletons = new ObjectArrayList<>();
            this.partialTicks = new FloatArrayList();

            this.instancedData = MemoryUtil.memAlloc(600); // Save space for 100 instances
        }

        public void add(Skeleton skeleton, int overlay, int light, int r, int g, int b, int a, float partialTicks) {
            if (this.instancedData.capacity() - this.instancedData.position() < 6) {
                this.instancedData = MemoryUtil.memRealloc(this.instancedData, (int) (this.instancedData.capacity() * 1.5));
            }
            this.instancedData.put((byte) overlay);
            this.instancedData.put((byte) light);
            this.instancedData.put((byte) r);
            this.instancedData.put((byte) g);
            this.instancedData.put((byte) b);
            this.instancedData.put((byte) a);
            this.skeletons.add(skeleton);
            this.partialTicks.add(partialTicks);
        }

        public void render() {
            try {
                if (instancedBuffer == 0) {
                    instancedBuffer = GlStateManager._glGenBuffers();
                }
                this.instancedData.flip();
                VertexArray.upload(instancedBuffer, this.instancedData, VertexArray.DrawUsage.DYNAMIC);
                updateBlockSize(this.skeletons.size(), this.skin.getSkeletonDataSize());
                this.skin.render(this.renderType, this.skeletons, instancedBuffer, boneBuffer, boneBlock, this.partialTicks);
            } finally {
                MemoryUtil.memFree(this.instancedData);
            }
        }
    }
}

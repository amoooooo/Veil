package foundry.veil.api.client.necromancer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilVertexFormat;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

// W.I.P. replacement for graveyard
// dooooon't use this. i'm still working on it...
//
//      ~ your best friend,
//          cappin  >_o

public class Skin {
    final Map<Integer, SkinnedMesh> boneToMesh = new HashMap<>();
    final ResourceLocation shader;
    final ResourceLocation texture;

    VertexBuffer mesh = null;

    private Skin(ResourceLocation shader, ResourceLocation texture) {
        this.shader = shader;
        this.texture = texture;
    }

    private void build() {
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, VeilVertexFormat.SKINNED_MESH);

        this.mesh = new VertexBuffer(VertexBuffer.Usage.STATIC);

        for (Map.Entry<Integer, SkinnedMesh> meshEntry : this.boneToMesh.entrySet()) {
            meshEntry.getValue().build(bufferbuilder, meshEntry.getKey());
        }
        MeshData renderedBuffer = bufferbuilder.buildOrThrow();
        this.mesh.bind();
        this.mesh.upload(renderedBuffer);
        VertexBuffer.unbind();
    }

    // todo:
    //  this
    public void render(PoseStack stack, Skeleton skeleton) {
        this.mesh.drawWithShader(stack.last().pose(), RenderSystem.getProjectionMatrix(), VeilRenderSystem.setShader(shader).toShaderInstance());
    }

    public static class Builder {
        private final Skeleton skeleton;
        private final Skin skin;

        public Builder(Skeleton skeleton, ResourceLocation shader, ResourceLocation texture) {
            this.skeleton = skeleton;
            this.skin = new Skin(shader, texture);
        }

        public Builder assignMesh(String boneName, SkinnedMesh mesh) {
            if (!this.skeleton.nameToId.containsKey(boneName)) {
                Veil.LOGGER.error("Cannot find bone of name {} in skeleton! Mesh not assigned.", boneName);
            } else {
                this.skin.boneToMesh.put(this.skeleton.nameToId.get(boneName), mesh);
            }
            return this;
        }

        public Skin build() {
            return this.skin;
        }
    }
}

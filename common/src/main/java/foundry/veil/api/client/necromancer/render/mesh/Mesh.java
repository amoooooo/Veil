package foundry.veil.api.client.necromancer.render.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

// TEMPORARY STUFF
// HOLDOVER FROM OLD GRAVEYARD
public abstract class Mesh {
    public static final Mesh EMPTY = new Mesh() {
        public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {}
    };

    public abstract void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha);

    protected record Vertex(float x, float y, float z) {}
    protected record UV(float u, float v) {}
    protected static class Face {
        protected final Vertex[] vertices;
        protected final UV[] uvs;
        protected final Vector3f normal;

        protected Face(Vertex a, Vertex b, Vertex c, Vertex d, float u0, float v0, float u1, float v1, float textureWidth, float textureHeight, boolean mirrored, Direction pDirection) {
            this.vertices = new Vertex[]{a, b, c, d};
            this.uvs = new UV[] {new UV(u1 / textureWidth, v0 / textureHeight), new UV(u0 / textureWidth, v0 / textureHeight), new UV(u0 / textureWidth, v1 / textureHeight), new UV(u1 / textureWidth, v1 / textureHeight)};
            if (mirrored) {
                int i = this.vertices.length;
                for(int j = 0; j < i / 2; ++j) {
                    Vertex vertex = this.vertices[j];
                    UV uv = this.uvs[j];
                    this.vertices[j] = this.vertices[i - 1 - j];
                    this.uvs[j] = this.uvs[i - 1 - j];
                    this.vertices[i - 1 - j] = vertex;
                    this.uvs[i - 1 - j] = uv;
                }
            }

            this.normal = pDirection.step();
            if (mirrored) {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }
            this.normal.mul(-1.0F, -1.0F, -1.0F);
        }

        protected Face(Vertex[] vertices, UV[] uvs, Vector3f normal) {
            this.vertices = vertices;
            this.uvs = uvs;
            this.normal = normal;
        }
    }
}

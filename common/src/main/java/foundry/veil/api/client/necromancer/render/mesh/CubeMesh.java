package foundry.veil.api.client.necromancer.render.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TEMPORARY STUFF
// HOLDOVER FROM OLD GRAVEYARD
public class CubeMesh extends Mesh {
    private final List<Face> faces;
    final int textureWidth, textureHeight;

    public CubeMesh(int textureWidth, int textureHeight) {
        super();
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        faces = new ArrayList<>();
    }

    public Face[] addCube(float xSize, float ySize, float zSize, float xOffset, float yOffset, float zOffset, float xInflate, float yInflate, float zInflate, float uOffset, float vOffset) {
        return this.addCube(xSize, ySize, zSize, xOffset, yOffset, zOffset, xInflate, yInflate, zInflate, uOffset, vOffset, false);
    }

    public Face[] addCube(float xSize, float ySize, float zSize, float xOffset, float yOffset, float zOffset, float xInflate, float yInflate, float zInflate, float uOffset, float vOffset, boolean mirrored) {
        float minX = xOffset;
        float minY = yOffset;
        float minZ = zOffset;
        float maxX = xOffset + xSize;
        float maxY = yOffset + ySize;
        float maxZ = zOffset + zSize;
        minX -= xInflate;
        minY -= yInflate;
        minZ -= zInflate;
        maxX += xInflate;
        maxY += yInflate;
        maxZ += zInflate;
        if (mirrored) {
            float swap = maxX;
            maxX = minX;
            minX = swap;
        }

        Vertex ooo = new Vertex(minX, minY, minZ);
        Vertex xoo = new Vertex(maxX, minY, minZ);
        Vertex oyo = new Vertex(minX, maxY, minZ);
        Vertex ooz = new Vertex(minX, minY, maxZ);
        Vertex xyo = new Vertex(maxX, maxY, minZ);
        Vertex oyz = new Vertex(minX, maxY, maxZ);
        Vertex xoz = new Vertex(maxX, minY, maxZ);
        Vertex xyz = new Vertex(maxX, maxY, maxZ);

        float u0 = uOffset;
        float u1 = uOffset + Mth.floor(zSize);
        float u2 = uOffset + Mth.floor(zSize) + Mth.floor(xSize);
        float u3 = uOffset + Mth.floor(zSize) + Mth.floor(xSize) + Mth.floor(xSize);
        float u4 = uOffset + Mth.floor(zSize) + Mth.floor(xSize) + Mth.floor(zSize);
        float u5 = uOffset + Mth.floor(zSize) + Mth.floor(xSize) + Mth.floor(zSize) + Mth.floor(xSize);
        float v0 = vOffset;
        float v1 = vOffset + Mth.floor(zSize);
        float v2 = vOffset + Mth.floor(zSize) + Mth.floor(ySize);

        Face[] cubeFaces = {new Face(xoz, ooz, ooo, xoo, u3, v0, u2, v1, textureWidth, textureHeight, mirrored, Direction.UP),
                new Face(xyo, oyo, oyz, xyz, u2, v1, u1, v0, textureWidth, textureHeight, mirrored, Direction.DOWN),
                new Face(ooo, ooz, oyz, oyo, u4, v2, u2, v1, textureWidth, textureHeight, mirrored, Direction.EAST),
                new Face(xoz, xoo, xyo, xyz, u1, v2, u0, v1, textureWidth, textureHeight, mirrored, Direction.WEST),
                new Face(ooz, xoz, xyz, oyz, u5, v2, u4, v1, textureWidth, textureHeight, mirrored, Direction.NORTH),
                new Face(xoo, ooo, oyo, xyo, u2, v2, u1, v1, textureWidth, textureHeight, mirrored, Direction.SOUTH)};

        Collections.addAll(faces, cubeFaces);

        return cubeFaces;
    }

    public Face addTri(float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3, float normalX, float normalY, float normalZ) {
        Face face = new Face(new Vertex[]{new Vertex(x1, y1, z1), new Vertex(x2, y2, z2), new Vertex(x2, y2, z2)}, new UV[]{new UV(u1, v1), new UV(u2, v2), new UV(u3, v3)}, new Vector3f(normalX, normalY, normalZ));
        faces.add(face);
        return face;
    }

    public Face addFace(float normalX, float normalY, float normalZ, FaceVertex... vertices) {
        Vertex[] vertArray = new Vertex[vertices.length];
        UV[] uvArray = new UV[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            FaceVertex vertex = vertices[i];
            vertArray[i] = new Vertex(vertex.x(), vertex.y(), vertex.z());
            uvArray[i] = new UV(vertex.u() / (float)this.textureWidth, vertex.v() / (float)this.textureHeight);
        }
        Face face = new Face(vertArray, uvArray, new Vector3f(normalX, normalY, normalZ));
        faces.add(face);
        return face;
    }

    public record FaceVertex(float x, float y, float z, float u, float v) {}

    @Override
    public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
        Matrix4f matrix4f = pPoseStack.last().pose();
        Matrix3f matrix3f = pPoseStack.last().normal();
        Vector4f position = new Vector4f();
        Vector3f normal = new Vector3f();

        for (Face face : this.faces) {
            for (int i = 0; i < face.vertices.length; i++) {
                Vertex vertex = face.vertices[i];
                UV uv = face.uvs[i];
                position.set(vertex.x(), vertex.y(), vertex.z(), 1.0F);
                matrix4f.transform(position);
                normal.set(face.normal.x(), face.normal.y(), face.normal.z());
                matrix3f.transform(normal);
                pVertexConsumer
                        .addVertex(position.x(), position.y(), position.z())
                        .setColor(pRed, pGreen, pBlue, pAlpha)
                        .setUv(uv.u(), uv.v())
                        .setOverlay(pPackedOverlay)
                        .setLight(pPackedLight)
                        .setNormal(normal.x(), normal.y(), normal.z());
            }
        }
    }
}

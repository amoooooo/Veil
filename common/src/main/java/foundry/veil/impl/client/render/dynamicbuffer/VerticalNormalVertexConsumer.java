package foundry.veil.impl.client.render.dynamicbuffer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class VerticalNormalVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private boolean verticalNormal;

    public VerticalNormalVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer addVertex(float pX, float pY, float pZ) {
        this.delegate.addVertex(pX, pY, pZ);
        return this;
    }

    @Override
    public VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
        this.delegate.setColor(pRed, pGreen, pBlue, pAlpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float pU, float pV) {
        this.delegate.setUv(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int pU, int pV) {
        this.delegate.setUv1(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int pU, int pV) {
        this.delegate.setUv2(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ) {
        if (this.verticalNormal) {
            this.delegate.setNormal(0, 1, 0);
        } else {
            this.delegate.setNormal(pNormalX, pNormalY, pNormalZ);
        }
        return this;
    }

    @Override
    public void putBulkData(PoseStack.Pose pPose, BakedQuad pQuad, float[] pBrightness, float pRed, float pGreen, float pBlue, float pAlpha, int[] pLightmap, int pPackedOverlay, boolean p_331268_) {
        this.verticalNormal = !pQuad.isShade();
        VertexConsumer.super.putBulkData(pPose, pQuad, pBrightness, pRed, pGreen, pBlue, pAlpha, pLightmap, pPackedOverlay, p_331268_);
        this.verticalNormal = false;
    }
}
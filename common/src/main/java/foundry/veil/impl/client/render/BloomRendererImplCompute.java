package foundry.veil.impl.client.render;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.bloom.BloomRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import foundry.veil.api.client.render.shader.VeilShaders;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.util.Mth;

import java.io.Closeable;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

public class BloomRendererImplCompute extends BloomRenderer {
    private static final int MIN_DOWN_SAMPLE_SIZE = 10;

    private final List<HDRTexture> downSampleTextures = new ArrayList<>();

    public void initialize(int width, int height) {
        downSampleTextures.forEach(HDRTexture::close);
        downSampleTextures.clear();

        // generate down sample sizes
        int w = width, h = height;
        while (w >= MIN_DOWN_SAMPLE_SIZE && h >= MIN_DOWN_SAMPLE_SIZE) {
            w = Mth.positiveCeilDiv(w, 2);
            h = Mth.positiveCeilDiv(h, 2);
            downSampleTextures.add(new HDRTexture(w, h));
        }
    }

    public void apply(AdvancedFbo framebuffer) {
        var attachment = framebuffer.getColorTextureAttachment(0);
        attachment.bindAttachment();
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Down sample
        {
            VeilRenderSystem.setShader(VeilShaders.BLOOM_DOWNSAMPLE);
            ShaderProgram shader = VeilRenderSystem.getShader();
            shader.bind();
            for (int i = 0; i < downSampleTextures.size(); i++) {
                HDRTexture inputTex = i == 0 ? new HDRTexture(attachment) : downSampleTextures.get(i - 1);
                HDRTexture outputTex = downSampleTextures.get(i);

                RenderSystem.bindTexture(outputTex.id);

                RenderSystem.activeTexture(GL_TEXTURE0);
                RenderSystem.bindTexture(inputTex.id);
                glBindImageTexture(0, outputTex.id, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);

                glDispatchCompute(
                    Mth.positiveCeilDiv(outputTex.width, 8),
                    Mth.positiveCeilDiv(outputTex.height, 4),
                    1
                );
                glMemoryBarrier(GL_ALL_BARRIER_BITS);
            }
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        framebuffer.bind(false);
        framebuffer.clear();
        AdvancedFbo.unbind();

        // Up sample
        {
            VeilRenderSystem.setShader(VeilShaders.BLOOM_UPSAMPLE);
            ShaderProgram shader = VeilRenderSystem.getShader();
            shader.bind();

            for (int i = downSampleTextures.size() - 1; i >= 0; i--) {
                HDRTexture inputTex = downSampleTextures.get(i);
                HDRTexture outputTex = i == 0 ? new HDRTexture(attachment) : downSampleTextures.get(i - 1);

                RenderSystem.bindTexture(outputTex.id);

                RenderSystem.activeTexture(GL_TEXTURE0);
                RenderSystem.bindTexture(inputTex.id);
                glBindImageTexture(0, outputTex.id, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);

                shader.setFloat("multiplier", 1f); // TODO: custom bloom drop off curve?

                glDispatchCompute(
                    Mth.positiveCeilDiv(outputTex.width, 8),
                    Mth.positiveCeilDiv(outputTex.height, 4),
                    1
                );
                glMemoryBarrier(GL_TEXTURE_FETCH_BARRIER_BIT);
            }
        }

        VeilRenderSystem.setShader((ShaderProgram) null);
        RenderSystem.bindTexture(0);
    }

    @Override
    public void close() {
        downSampleTextures.forEach(HDRTexture::close);
    }

    private record HDRTexture(int width, int height, int id) implements Closeable {
        public HDRTexture(int width, int height) {
            this(width, height, TextureUtil.generateTextureId());
            RenderSystem.bindTextureForSetup(id);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (FloatBuffer) null);
            RenderSystem.bindTexture(0);
        }

        public HDRTexture(AdvancedFboTextureAttachment attachment) {
            this(attachment.getWidth(), attachment.getHeight(), attachment.getId());
        }

        @Override
        public void close() {
            TextureUtil.releaseTextureId(id);
        }
    }
}

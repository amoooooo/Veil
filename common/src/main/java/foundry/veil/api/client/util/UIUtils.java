package foundry.veil.api.client.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Either;
import foundry.veil.Veil;
import foundry.veil.api.client.tooltip.Tooltippable;
import foundry.veil.api.client.tooltip.VeilUIItemTooltipDataHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UIUtils {
    public static void drawHoverText(Tooltippable tooltippable, float pticks, final ItemStack stack, PoseStack pStack, List<? extends FormattedText> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight,
                                     int maxTextWidth, int backgroundColor, int borderColorStart, int borderColorEnd, Font font,
                                     int tooltipTextWidthBonus, int tooltipTextHeightBonus, List<VeilUIItemTooltipDataHolder> items,
                                     int desiredX, int desiredY) {
        if (textLines.isEmpty()) {
            return;
        }

        List<ClientTooltipComponent> list = gatherTooltipComponents(stack, textLines,
                stack.getTooltipImage(), mouseX, screenWidth, screenHeight, font, font);
        // RenderSystem.disableRescaleNormal();
        RenderSystem.disableDepthTest();
        int tooltipTextWidth = 0;

        for (FormattedText textLine : textLines) {
            int textLineWidth = font.width(textLine);
            if (textLineWidth > tooltipTextWidth) {
                tooltipTextWidth = textLineWidth;
            }
        }

        boolean needsWrap = false;

        int titleLinesCount = 1;
        int tooltipX = mouseX + 12;
        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = mouseX - 16 - tooltipTextWidth;
            if (tooltipX < 4) // if the tooltip doesn't fit on the screen
            {
                if (mouseX > screenWidth / 2) {
                    tooltipTextWidth = mouseX - 12 - 8;
                } else {
                    tooltipTextWidth = screenWidth - 16 - mouseX;
                }
                needsWrap = true;
            }
        }

        if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
            tooltipTextWidth = maxTextWidth;
            needsWrap = true;
        }

        if (needsWrap) {
            int wrappedTooltipWidth = 0;
            List<FormattedText> wrappedTextLines = new ArrayList<>();
            for (int i = 0; i < textLines.size(); i++) {
                FormattedText textLine = textLines.get(i);
                List<FormattedText> wrappedLine = font.getSplitter()
                        .splitLines(textLine, tooltipTextWidth, Style.EMPTY);
                if (i == 0) {
                    titleLinesCount = wrappedLine.size();
                }

                for (FormattedText line : wrappedLine) {
                    int lineWidth = font.width(line);
                    if (lineWidth > wrappedTooltipWidth) {
                        wrappedTooltipWidth = lineWidth;
                    }
                    wrappedTextLines.add(line);
                }
            }
            tooltipTextWidth = wrappedTooltipWidth;
            textLines = wrappedTextLines;

            if (mouseX > screenWidth / 2) {
                tooltipX = mouseX - 16 - tooltipTextWidth;
            } else {
                tooltipX = mouseX + 12;
            }
        }

        int tooltipY = mouseY - 12;
        int tooltipHeight = 8;

        if (textLines.size() > 1) {
            tooltipHeight += (textLines.size() - 1) * 10;
            if (textLines.size() > titleLinesCount) {
                tooltipHeight += 2; // gap between title lines and next lines
            }
        }

        if (tooltipY < 4) {
            tooltipY = 4;
        } else if (tooltipY + tooltipHeight + 4 > screenHeight) {
            tooltipY = screenHeight - tooltipHeight - 4;
        }

        final int zLevel = 400;
        tooltipTextWidth += tooltipTextWidthBonus;
        tooltipHeight += tooltipTextHeightBonus;


//        VeilUITooltipRenderer.drawConnectionLine(pStack, tooltippable, tooltipX, tooltipY, desiredX, desiredY);
        drawTooltipRects(pticks, pStack, zLevel, backgroundColor, borderColorStart, borderColorEnd, font, list, tooltipTextWidth, titleLinesCount, tooltipX, tooltipY, tooltipHeight, items);
    }

    private static void drawTooltipRects(float pticks, PoseStack pStack, int z, int backgroundColor, int borderColorStart, int borderColorEnd, Font font, List<ClientTooltipComponent> list, int tooltipTextWidth, int titleLinesCount, int tooltipX, int tooltipY, int tooltipHeight, List<VeilUIItemTooltipDataHolder> items) {
        pStack.pushPose();
        Matrix4f mat = pStack.last()
                .pose();
        drawGradientRect(mat, z, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, z, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
        drawGradientRect(mat, z, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, z, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, z, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, z, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
        drawGradientRect(mat, z, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
        drawGradientRect(mat, z, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
        drawGradientRect(mat, z, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

        int itemY = tooltipY;
        for (int lineNumber = 0; lineNumber < list.size(); ++lineNumber) {
            if (lineNumber + 1 == titleLinesCount) {
                itemY += 2;
            }

            itemY += 10;
        }
        pStack.pushPose();
        pStack.translate(0, 0, 300);
        if (items != null && !items.isEmpty()) {
            for (VeilUIItemTooltipDataHolder item : items) {
                renderAndDecorateItem(item.getItemStack(), tooltipX + item.getX().apply(pticks), itemY + item.getY().apply(pticks));
                drawTexturedRect(pStack.last().pose(), z + 100, tooltipX + item.getX().apply(pticks), itemY + item.getY().apply(pticks), 16, 16, 0, 0, 0, 0, 16, 16, Veil.veilPath("textures/gui/item_shadow.png"));
            }
        }
        pStack.popPose();

        MultiBufferSource.BufferSource renderType = Minecraft.getInstance().renderBuffers().bufferSource();
        pStack.translate(0.0D, 0.0D, z);

        for (int lineNumber = 0; lineNumber < list.size(); ++lineNumber) {
            ClientTooltipComponent line = list.get(lineNumber);

            if (line != null) {
                line.renderText(font, tooltipX, tooltipY, mat, renderType);
            }

            if (lineNumber + 1 == titleLinesCount) {
                tooltipY += 2;
            }

            tooltipY += 10;
        }


        renderType.endBatch();
        pStack.popPose();

        RenderSystem.enableDepthTest();
    }

    public static void renderAndDecorateItem(ItemStack $$0, float $$1, float $$2) {
        tryRenderGuiItem(Minecraft.getInstance().player, $$0, $$1, $$2, 0);
    }

    public static void tryRenderGuiItem(@Nullable LivingEntity $$0, ItemStack $$1, float $$2, float $$3, int $$4) {
        tryRenderGuiItem($$0, $$1, $$2, $$3, $$4, 0);
    }

    public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent, int mouseX, int screenWidth, int screenHeight, @Nullable Font forcedFont, Font fallbackFont) {
        Font font = forcedFont != null ? forcedFont : fallbackFont;
        List<Either<FormattedText, TooltipComponent>> elements = textElements.stream()
                .map((Function<FormattedText, Either<FormattedText, TooltipComponent>>) Either::left)
                .collect(Collectors.toCollection(ArrayList::new));
        itemComponent.ifPresent(c -> elements.add(1, Either.right(c)));


        // text wrapping
        int tooltipTextWidth = elements.stream()
                .mapToInt(either -> either.map(font::width, component -> 0))
                .max()
                .orElse(0);

        boolean needsWrap = false;

        int tooltipX = mouseX + 12;
        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = mouseX - 16 - tooltipTextWidth;
            if (tooltipX < 4) // if the tooltip doesn't fit on the screen
            {
                if (mouseX > screenWidth / 2) {
                    tooltipTextWidth = mouseX - 12 - 8;
                } else {
                    tooltipTextWidth = screenWidth - 16 - mouseX;
                }
                needsWrap = true;
            }
        }

//        if (tooltipTextWidth > -1)
//        {
//            tooltipTextWidth = -1;
//            needsWrap = true;
//        }

        int tooltipTextWidthF = tooltipTextWidth;
        if (needsWrap) {
            return elements.stream()
                    .flatMap(either -> either.map(
                            text -> font.split(text, tooltipTextWidthF).stream().map(ClientTooltipComponent::create),
                            component -> Stream.of(ClientTooltipComponent.create(component))
                    ))
                    .toList();
        }
        return elements.stream()
                .map(either -> either.map(
                        text -> ClientTooltipComponent.create(text instanceof Component ? ((Component) text).getVisualOrderText() : Language.getInstance().getVisualOrder(text)),
                        ClientTooltipComponent::create
                ))
                .toList();
    }

    public static void drawGradientRect(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (float) (startColor >> 24 & 255) / 255.0F;
        float startRed = (float) (startColor >> 16 & 255) / 255.0F;
        float startGreen = (float) (startColor >> 8 & 255) / 255.0F;
        float startBlue = (float) (startColor & 255) / 255.0F;
        float endAlpha = (float) (endColor >> 24 & 255) / 255.0F;
        float endRed = (float) (endColor >> 16 & 255) / 255.0F;
        float endGreen = (float) (endColor >> 8 & 255) / 255.0F;
        float endBlue = (float) (endColor & 255) / 255.0F;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(mat, right, top, zLevel).setColor(startRed, startGreen, startBlue, startAlpha);
        buffer.addVertex(mat, left, top, zLevel).setColor(startRed, startGreen, startBlue, startAlpha);
        buffer.addVertex(mat, left, bottom, zLevel).setColor(endRed, endGreen, endBlue, endAlpha);
        buffer.addVertex(mat, right, bottom, zLevel).setColor(endRed, endGreen, endBlue, endAlpha);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }

    public static void drawTexturedRect(Matrix4f mat, int zLevel, float x, float y, float width, float height, float u, float v, float uWidth, float vHeight, int textureWidth, int textureHeight, ResourceLocation texture) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(mat, x, y + height, zLevel).setUv(u * f, (v + vHeight) * f1);
        buffer.addVertex(mat, x + width, y + height, zLevel).setUv((u + uWidth) * f, (v + vHeight) * f1);
        buffer.addVertex(mat, x + width, y, zLevel).setUv((u + uWidth) * f, v * f1);
        buffer.addVertex(mat, x, y, zLevel).setUv(u * f, v * f1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void tryRenderGuiItem(LivingEntity $$0, ItemStack $$1, float $$2, float $$3, int $$4, float $$5) {
//        if (!$$1.isEmpty()) {
//            BakedModel $$6 = Minecraft.getInstance().getItemRenderer().getModel($$1, (Level) null, $$0, $$4);
//            Minecraft.getInstance().getItemRenderer().blitOffset = $$6.isGui3d() ? Minecraft.getInstance().getItemRenderer().blitOffset + 50.0F + (float) $$5 : Minecraft.getInstance().getItemRenderer().blitOffset + 50.0F;
//
//            try {
//                renderGuiItem($$1, $$2, $$3, $$6);
//            } catch (Throwable var11) {
//                CrashReport $$8 = CrashReport.forThrowable(var11, "Rendering item");
//                CrashReportCategory $$9 = $$8.addCategory("Item being rendered");
//                $$9.setDetail("Item Type", () -> {
//                    return String.valueOf($$1.getItem());
//                });
//                $$9.setDetail("Item Damage", () -> {
//                    return String.valueOf($$1.getDamageValue());
//                });
//                $$9.setDetail("Item NBT", () -> {
//                    return String.valueOf($$1.getTag());
//                });
//                $$9.setDetail("Item Foil", () -> {
//                    return String.valueOf($$1.hasFoil());
//                });
//                throw new ReportedException($$8);
//            }
//
//            Minecraft.getInstance().getItemRenderer().blitOffset = $$6.isGui3d() ? Minecraft.getInstance().getItemRenderer().blitOffset - 50.0F - (float) $$5 : Minecraft.getInstance().getItemRenderer().blitOffset - 50.0F;
//        }
    }

    public static void renderGuiItem(ItemStack stack, float x, float y, BakedModel bakedModel) {
        Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Matrix4fStack $$4 = RenderSystem.getModelViewStack();
        $$4.pushMatrix();
//        $$4.translate((double) x, (double) y, (double) (100.0F + Minecraft.getInstance().getItemRenderer().blitOffset));
        $$4.translate(8, 8, 0);
        $$4.scale(1.0F, -1.0F, 1.0F);
        $$4.scale(16.0F, 16.0F, 16.0F);
        RenderSystem.applyModelViewMatrix();
        PoseStack $$5 = new PoseStack();
        MultiBufferSource.BufferSource $$6 = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean $$7 = !bakedModel.usesBlockLight();
        if ($$7) {
            Lighting.setupForFlatItems();
        } else {
            Lighting.setupFor3DItems();
        }

        Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.GUI, false, $$5, $$6, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
        $$6.endBatch();
        RenderSystem.enableDepthTest();
        if ($$7) {
            Lighting.setupFor3DItems();
        }

        $$4.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}

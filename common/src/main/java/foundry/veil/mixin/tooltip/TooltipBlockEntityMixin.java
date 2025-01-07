package foundry.veil.mixin.tooltip;

import foundry.veil.api.client.color.Color;
import foundry.veil.api.client.color.ColorTheme;
import foundry.veil.api.client.tooltip.Tooltippable;
import foundry.veil.api.client.tooltip.VeilUIItemTooltipDataHolder;
import foundry.veil.api.client.tooltip.anim.TooltipTimeline;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(BlockEntity.class)
public class TooltipBlockEntityMixin implements Tooltippable {

    @Unique
    private List<Component> veil$tooltip = new ArrayList<>();

    @Unique
    private ColorTheme veil$theme;

    @Unique
    private List<VeilUIItemTooltipDataHolder> veil$tooltipDataHolder = new ArrayList<>();

    @Unique
    private TooltipTimeline veil$timeline = null;

    @Unique
    private boolean veil$worldspace = true;

    @Unique
    private boolean veil$tooltipEnabled = false;

    @Unique
    private int veil$tooltipX = 0;

    @Unique
    private int veil$tooltipY = 0;

    @Unique
    private int veil$tooltipWidth = 0;

    @Unique
    private int veil$tooltipHeight = 0;

    @Override
    public List<Component> getTooltip() {
        return this.veil$tooltip;
    }

    @Override
    public boolean isTooltipEnabled() {
        return this.veil$tooltipEnabled;
    }

    @Override
    public CompoundTag saveTooltipData() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("tooltipEnabled", this.veil$tooltipEnabled);
        tag.putInt("tooltipX", this.veil$tooltipX);
        tag.putInt("tooltipY", this.veil$tooltipY);
        tag.putInt("tooltipWidth", this.veil$tooltipWidth);
        tag.putInt("tooltipHeight", this.veil$tooltipHeight);
        tag.putBoolean("worldspace", this.veil$worldspace);

        if (this.veil$theme != null) {
            CompoundTag themeTag = new CompoundTag();
            for (Map.Entry<String, Color> entry : this.veil$theme.getColorsMap().entrySet()) {
                String key = entry.getKey() != null ? entry.getKey() : "";
                themeTag.putInt(key, entry.getValue().getRGB());
            }
            tag.put("theme", themeTag);
        }
        return tag;
    }

    @Override
    public void loadTooltipData(CompoundTag tag) {
        this.veil$tooltipEnabled = tag.getBoolean("tooltipEnabled");
        this.veil$tooltipX = tag.getInt("tooltipX");
        this.veil$tooltipY = tag.getInt("tooltipY");
        this.veil$tooltipWidth = tag.getInt("tooltipWidth");
        this.veil$tooltipHeight = tag.getInt("tooltipHeight");
        this.veil$worldspace = tag.getBoolean("worldspace");

        if (this.veil$theme != null) {
            this.veil$theme.clear();
        }
        if (tag.contains("theme", CompoundTag.TAG_COMPOUND)) {
            if (this.veil$theme == null) {
                this.veil$theme = new ColorTheme();
            }
            CompoundTag themeTag = tag.getCompound("theme");
            for (String key : themeTag.getAllKeys()) {
                this.veil$theme.addColor(key, Color.of(themeTag.getInt(key)));
            }
        }
    }

    @Override
    public void setTooltip(List<Component> tooltip) {
        this.veil$tooltip = tooltip;
    }

    @Override
    public void addTooltip(Component tooltip) {
        this.veil$tooltip.add(tooltip);
    }

    @Override
    public void addTooltip(List<Component> tooltip) {
        this.veil$tooltip.addAll(tooltip);
    }

    @Override
    public void addTooltip(String tooltip) {
        this.veil$tooltip.add(Component.nullToEmpty(tooltip));
    }

    @Override
    public ColorTheme getTheme() {
        return this.veil$theme;
    }

    @Override
    public void setTheme(ColorTheme theme) {
        this.veil$theme = theme;
    }

    @Override
    public void setBackgroundColor(int color) {
        this.veil$theme.addColor("background", Color.of(color));
    }

    @Override
    public void setTopBorderColor(int color) {
        this.veil$theme.addColor("topBorder", Color.of(color));
    }

    @Override
    public void setBottomBorderColor(int color) {
        this.veil$theme.addColor("bottomBorder", Color.of(color));
    }

    @Override
    public boolean getWorldspace() {
        return this.veil$worldspace;
    }

    @Override
    public TooltipTimeline getTimeline() {
        return this.veil$timeline;
    }

    @Override
    public ItemStack getStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public int getTooltipWidth() {
        return this.veil$tooltipWidth;
    }

    @Override
    public int getTooltipHeight() {
        return this.veil$tooltipHeight;
    }

    @Override
    public int getTooltipXOffset() {
        return this.veil$tooltipX;
    }

    @Override
    public int getTooltipYOffset() {
        return this.veil$tooltipHeight;
    }

    @Override
    public List<VeilUIItemTooltipDataHolder> getItems() {
        return this.veil$tooltipDataHolder;
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    public void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries, CallbackInfo ci) {
        pTag.put("tooltipData", this.saveTooltipData());
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries, CallbackInfo ci) {
        this.loadTooltipData(pTag.getCompound("tooltipData"));
    }

}

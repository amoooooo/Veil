package foundry.veil.example;

import foundry.veil.Veil;
import foundry.veil.example.block.BasicHDRBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VeilExampleContent {
    private static final List<RegInfo<?, ?>> toRegister = new ArrayList<>();

    public static final BasicHDRBlock BASIC_HDR_BLOCK = block(new BasicHDRBlock());
    public static final BlockItem BASIC_HDR_BLOCK_ITEM = item(new BlockItem(BASIC_HDR_BLOCK, new Item.Properties()));

    public static void init() {
        toRegister.forEach(RegInfo::commit);
        toRegister.clear();

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                Veil.veilPath("example_items"),
                CreativeModeTab
                        .builder(CreativeModeTab.Row.TOP, 100)
                        .title(Component.translatable("veil.item_group.example_items_tab"))
                        .icon(() -> new ItemStack(BASIC_HDR_BLOCK_ITEM))
                        .displayItems((itemDisplayParameters, output) -> {
                            output.accept(BASIC_HDR_BLOCK_ITEM);
                        })
                        .build()
        );
    }

    private static <T extends Item> T item(T item, ResourceLocation id) {
        return reg(BuiltInRegistries.ITEM, item, id);
    }

    private static <T extends Item> T item(T item) {
        return reg(BuiltInRegistries.ITEM, item);
    }

    private static <T extends Block> T block(T block, ResourceLocation id) {
        return reg(BuiltInRegistries.BLOCK, block, id);
    }

    private static <T extends Block> T block(T block) {
        return reg(BuiltInRegistries.BLOCK, block);
    }

    private record RegInfo<V, T extends V>(Registry<V> registry, T obj, ResourceLocation id) {
        private void commit() {
            Registry.register(this.registry, this.id, this.obj);
        }
    }

    private static <V, T extends V> T reg(Registry<V> registry, T obj, ResourceLocation id) {
        toRegister.add(new RegInfo<>(registry, obj, id));
        return obj;
    }

    private static <V, T extends V> T reg(Registry<V> registry, T obj) {
        return reg(registry, obj, getObjRegId(obj));
    }

    private static ResourceLocation getObjRegId(Object obj) {
        Class<?> clz = obj.getClass();
        if (obj instanceof BlockItem blockItem)
            clz = blockItem.getBlock().getClass();
        try {
            return Objects.requireNonNull((ResourceLocation) clz.getField("ID").get(null));
        } catch (IllegalAccessException | NoSuchFieldException | ClassCastException e) {
            throw new RuntimeException("Failed to get register id from object <" + obj + ">.");
        }
    }
}

package net.placemats.common.data;

import static net.placemats.common.data.PlaceMatRegistries.REGISTRATE;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public class PlaceMatCreativeTab {

    public static void init() {
    }

    public static final RegistryEntry<CreativeModeTab> PLACE_MATS = REGISTRATE.defaultCreativeTab("place_mats",
            builder -> builder.title(Component.translatable("place_mats.creative_tab.place_mats"))
                    .icon(() -> new ItemStack(PlaceMatBlocks.STORAGE_RACK.get()))
                    .displayItems(new RegistrateDisplayItemsGenerator("place_mats", REGISTRATE)))
            .register();

    public record RegistrateDisplayItemsGenerator(String name,
            PlaceMatRegistrate registrate) implements CreativeModeTab.DisplayItemsGenerator {

        @Override
        public void accept(CreativeModeTab.ItemDisplayParameters itemDisplayParameters,
                CreativeModeTab.Output output) {
            var tab = registrate.get(name, Registries.CREATIVE_MODE_TAB);
            for (var entry : registrate.getAll(Registries.BLOCK)) {
                Block block = entry.get();
                var stack = new ItemStack(block, 1);

                if (registrate.isInCreativeTab(entry, tab))
                    continue;
                if (entry.getId().getNamespace().equals("place_mats") && !stack.isEmpty())
                    output.accept(block);
            }
            for (var entry : registrate.getAll(Registries.ITEM)) {
                if (registrate.isInCreativeTab(entry, tab))
                    continue;
                Item item = entry.get();
                var stack = new ItemStack(item, 1);
                if (item instanceof BlockItem)
                    continue;
                if (entry.getId().getNamespace().equals("place_mats"))
                    output.accept(stack);
            }
        }
    }
}

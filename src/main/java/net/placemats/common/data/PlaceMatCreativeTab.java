package net.placemats.common.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlaceMatMain.MOD_ID);

    public static void init() {
    }

    public static final RegistryObject<CreativeModeTab> PLACE_MATS = CREATIVE_MODE_TABS.register("place_mats",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("place_mats.creative_tab.place_mats"))
                    .icon(() -> new ItemStack(PlaceMatBlocks.STORAGE_RACK_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(PlaceMatBlocks.STORAGE_RACK_ITEM.get());
                        PlaceMatBlocks.WOOD_STORAGE_RACKS.forEach(blockReg -> output.accept(blockReg.get()));
                    })
                    .build());
}

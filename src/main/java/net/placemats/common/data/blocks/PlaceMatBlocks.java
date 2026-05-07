package net.placemats.common.data.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.compat.tfc.TFCCompat;

@SuppressWarnings({ "unused" })
public final class PlaceMatBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PlaceMatMain.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PlaceMatMain.MOD_ID);

    public static void init() {
    }

    public static final RegistryObject<Block> STORAGE_RACK = BLOCKS.register("storage_rack", () -> {
        PlaceMatBlock block = (PlaceMatBlock) TFCCompat.INSTANCE.createPlaceMatBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2.0f).noOcclusion(), true);
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
                new AABB(1 / 16D, 0 / 16D, 1 / 16D, 15 / 16D, 7 / 16D, 15 / 16D),
                7 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.addRange(new PlaceMatBlock.PlacementRange(
                new AABB(1 / 16D, 8 / 16D, 1 / 16D, 7 / 16D, 15 / 16D, 7 / 16D),
                15 / 16F, false, false, false, false, true, false, null, true, true, 16, null, true, false, false, false, 1.0f, 0, 0, 0, 0));
        return block;
    });

    public static final RegistryObject<Item> STORAGE_RACK_ITEM = ITEMS.register("storage_rack", () -> new BlockItem(STORAGE_RACK.get(), new Item.Properties()));

}

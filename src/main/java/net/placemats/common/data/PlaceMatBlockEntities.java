package net.placemats.common.data;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.blocks.PlaceMatBlocks;
import net.placemats.compat.firmalife.FirmaLifeCompat;

public class PlaceMatBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PlaceMatMain.MOD_ID);

    public static final RegistryObject<BlockEntityType<PlaceMatBlockEntity>> PLACE_MAT = BLOCK_ENTITIES.register("place_mat",
            () -> BlockEntityType.Builder.of(FirmaLifeCompat.INSTANCE::createPlaceMatBE,
                    PlaceMatBlocks.STORAGE_RACK.get()).build(null));

    public static void init() {
    }

    public static void addValidBEBlock(Supplier<?> type, Block block) {
        if (type.get() instanceof BlockEntityType<?> beType) {
            try {
                java.lang.reflect.Field f = BlockEntityType.class.getDeclaredField("validBlocks");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Set<Block> set = (java.util.Set<Block>) f.get(beType);
                try {
                    set.add(block);
                } catch (UnsupportedOperationException e) {
                    java.util.Set<Block> newSet = new java.util.HashSet<>(set);
                    newSet.add(block);
                    f.set(beType, newSet);
                }
            } catch (Exception e) {
                PlaceMatMain.LOGGER.error("Failed to add valid block to BE type: {}", e.getMessage());
            }
        }
    }

}

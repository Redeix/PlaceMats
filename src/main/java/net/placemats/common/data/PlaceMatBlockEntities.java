package net.placemats.common.data;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.placemats.PlaceMatMain;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.compat.firmalife.FirmaLifeCompat;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class PlaceMatBlockEntities {
    public static final BlockEntityEntry<PlaceMatBlockEntity> PLACE_MAT = PlaceMatRegistries.REGISTRATE.<PlaceMatBlockEntity>blockEntity("place_mat", (type, pos, state) -> FirmaLifeCompat.INSTANCE.createPlaceMatBE(pos, state))
        .validBlock(PlaceMatBlocks.STORAGE_RACK)
        .validBlocks(PlaceMatBlocks.WOOD_STORAGE_RACKS.toArray(BlockEntry[]::new))
        .validBlocks(PlaceMatBlocks.WOOD_ORNATE_SHELVES.toArray(BlockEntry[]::new))
        .validBlocks(PlaceMatBlocks.WOOD_ORNATE_DOUBLE_SHELVES.toArray(BlockEntry[]::new))
        .validBlocks(PlaceMatBlocks.WOOD_FLOATING_SHELVES.toArray(BlockEntry[]::new))
        .register();

    public static void init() {
    }

    public static void addValidBEBlock(Supplier<? extends BlockEntityType<?>> type, Block block) {
        try {
            BlockEntityType<?> beType = type.get();
            if (beType != null) {
                Field field = BlockEntityType.class.getDeclaredField("validBlocks");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Set<Block> set = (Set<Block>) field.get(beType);
                try {
                    set.add(block);
                } catch (UnsupportedOperationException e) {
                    Set<Block> newSet = new HashSet<>(set);
                    newSet.add(block);
                    field.set(beType, newSet);
                }
            }
        } catch (Exception e) {
            PlaceMatMain.LOGGER.error("Failed to add valid block to BE type: {}", e.getMessage());
        }
    }
}

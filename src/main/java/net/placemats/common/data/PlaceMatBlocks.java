package net.placemats.common.data;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.compat.tfc.TFCCompat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "unused" })
public final class PlaceMatBlocks {

    public static final BlockEntry<PlaceMatBlock> STORAGE_RACK = PlaceMatRegistries.REGISTRATE.storageRack("storage_rack", true)
        .properties(p -> p.sound(SoundType.METAL).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
        .onRegister(block -> {
            block.containerSize(10);
            block.addRange(new PlaceMatBlock.PlacementRange(
                    new AABB(1 / 16D, 0 / 16D, 1 / 16D, 15 / 16D, 7 / 16D, 15 / 16D),
                    7 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
            block.addRange(new PlaceMatBlock.PlacementRange(
                    new AABB(1 / 16D, 8 / 16D, 1 / 16D, 7 / 16D, 15 / 16D, 7 / 16D),
                    15 / 16F, false, false, false, false, true, false, null, true, true, 16, null, true, false, false, false, 1.0f, 0, 0, 0, 0));
        })
        .blockstate((ctx, prov) -> {
            var model = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/storage_rack_parent"))
                    .texture("1", prov.modLoc("block/storage_rack_base"));
            prov.horizontalBlock(ctx.getEntry(), model);
        })
        .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_PICKAXE, PlaceMatTags.Blocks.STORAGE_RACKS)
        .loot(RegistrateBlockLootTables::dropSelf)
        .item()
        .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.STORAGE_RACKS, PlaceMatTags.Items.PLACE_MAT_BLACKLIST)
        .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/storage_rack")))
        .build()
        .register();

    public static final List<BlockEntry<PlaceMatBlock>> WOOD_STORAGE_RACKS = registerWoodVariants();

    public static void init() {
    }

    private static List<BlockEntry<PlaceMatBlock>> registerWoodVariants() {
        List<BlockEntry<PlaceMatBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String name = woodType.name() + "_" + "storage_rack";

            BlockEntry<PlaceMatBlock> blockReg = PlaceMatRegistries.REGISTRATE.storageRack(name, true)
                .properties(p -> p.sound(SoundType.WOOD).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
                .onRegister(block -> {
                    block.containerSize(10);
                    block.addRange(new PlaceMatBlock.PlacementRange(
                            new AABB(1 / 16D, 0 / 16D, 1 / 16D, 15 / 16D, 7 / 16D, 15 / 16D),
                            7 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.addRange(new PlaceMatBlock.PlacementRange(
                            new AABB(1 / 16D, 8 / 16D, 1 / 16D, 7 / 16D, 15 / 16D, 7 / 16D),
                            15 / 16F, false, false, false, false, true, false, null, true, true, 16, null, true, false, false, false, 1.0f, 0, 0, 0, 0));
                })
                .blockstate((ctx, prov) -> {
                    var model = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/storage_rack_parent"))
                            .texture("1", prov.modLoc("block/" + ctx.getName()));
                    prov.horizontalBlock(ctx.getEntry(), model);
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.STORAGE_RACKS)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.STORAGE_RACKS, PlaceMatTags.Items.PLACE_MAT_BLACKLIST)
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
                .build()
                .register();

            list.add(blockReg);
        });

        return list;
    }

    public static PlaceMatBlock createStorageRack(BlockBehaviour.Properties properties) {
        PlaceMatBlock block = (PlaceMatBlock) TFCCompat.INSTANCE.createPlaceMatBlock(properties, true);
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
                new AABB(1 / 16D, 0 / 16D, 1 / 16D, 15 / 16D, 7 / 16D, 15 / 16D),
                7 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.addRange(new PlaceMatBlock.PlacementRange(
                new AABB(1 / 16D, 8 / 16D, 1 / 16D, 7 / 16D, 15 / 16D, 7 / 16D),
                15 / 16F, false, false, false, false, true, false, null, true, true, 16, null, true, false, false, false, 1.0f, 0, 0, 0, 0));
        return block;
    }
}

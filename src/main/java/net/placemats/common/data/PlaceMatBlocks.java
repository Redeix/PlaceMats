package net.placemats.common.data;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.generators.ConfiguredModel;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlaceMatStairsBlock;
import net.placemats.compat.tfc.TFCCompat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "unused" })
public final class PlaceMatBlocks {

    public static final BlockEntry<PlaceMatCardinalBlock> STORAGE_RACK = PlaceMatRegistries.REGISTRATE.storageRack("storage_rack", true)
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

    public static final List<BlockEntry<PlaceMatCardinalBlock>> WOOD_STORAGE_RACKS = registerWoodVariants();
    public static final List<BlockEntry<PlaceMatStairsBlock>> WOOD_ORNATE_SHELVES = registerWoodOrnateShelves();
    public static final List<BlockEntry<PlaceMatStairsBlock>> ORNATE_SHELVES = WOOD_ORNATE_SHELVES;

    public static void init() {
    }

    private static List<BlockEntry<PlaceMatCardinalBlock>> registerWoodVariants() {
        List<BlockEntry<PlaceMatCardinalBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String name = woodType.name() + "_" + "storage_rack";

            BlockEntry<PlaceMatCardinalBlock> blockReg = PlaceMatRegistries.REGISTRATE.storageRack(name, true)
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

    private static List<BlockEntry<PlaceMatStairsBlock>> registerWoodOrnateShelves() {
        List<BlockEntry<PlaceMatStairsBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String name = woodType.name() + "_" + "ornate_shelf";

            BlockEntry<PlaceMatStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.ornateShelf(name)
                .properties(p -> p.sound(SoundType.WOOD).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
                .blockstate((ctx, prov) -> {
                    var straightModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/ornate_shelf_parent"))
                            .texture("0", prov.modLoc("block/" + woodType.name() + "_shelf"));
                    var innerModel = prov.models().withExistingParent(ctx.getName() + "_inner", prov.modLoc("block/ornate_shelf_inner_parent"))
                            .texture("0", prov.modLoc("block/" + woodType.name() + "_shelf"));
                    var outerModel = prov.models().withExistingParent(ctx.getName() + "_outer", prov.modLoc("block/ornate_shelf_outer_parent"))
                            .texture("0", prov.modLoc("block/" + woodType.name() + "_shelf"));

                    prov.getVariantBuilder(ctx.getEntry()).forAllStatesExcept(state -> {
                        Direction facing = state.getValue(PlaceMatStairsBlock.FACING);
                        StairsShape shape = state.getValue(PlaceMatStairsBlock.SHAPE);
                        int yRot = (int) facing.getClockWise().toYRot();
                        if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                            yRot += 270;
                        }
                        yRot %= 360;
                        var model = shape == StairsShape.STRAIGHT ? straightModel : (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT ? innerModel : outerModel);
                        return ConfiguredModel.builder()
                                .modelFile(model)
                                .rotationY(yRot)
                                .uvLock(true)
                                .build();
                    }, PlaceMatBlock.LOCKED);
                })
                .onRegister(block -> {
                    block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
                    block.containerSize(10);
                    block.addRange(new PlaceMatBlock.PlacementRange(
                        new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 16 / 16D, 16 / 16D),
                        16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.ORNATE_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.ORNATE_SHELVES, PlaceMatTags.Items.PLACE_MAT_BLACKLIST)
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
                .build()
                .register();

            list.add(blockReg);
        });

        return list;
    }

    // Generic methods for use with EveryCompat v

    public static PlaceMatCardinalBlock createStorageRack(BlockBehaviour.Properties properties) {
        PlaceMatCardinalBlock block = (PlaceMatCardinalBlock) TFCCompat.INSTANCE.createPlaceMatBlock(properties, true);
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
                new AABB(1 / 16D, 0 / 16D, 1 / 16D, 15 / 16D, 7 / 16D, 15 / 16D),
                7 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.addRange(new PlaceMatBlock.PlacementRange(
                new AABB(1 / 16D, 8 / 16D, 1 / 16D, 7 / 16D, 15 / 16D, 7 / 16D),
                15 / 16F, false, false, false, false, true, false, null, true, true, 16, null, true, false, false, false, 1.0f, 0, 0, 0, 0));
        return block;
    }

    public static PlaceMatStairsBlock createOrnateShelf(BlockBehaviour.Properties properties) {
        PlaceMatStairsBlock block = (PlaceMatStairsBlock) TFCCompat.INSTANCE.createPlaceMatStairsBlock(properties);
        block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
            new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 16 / 16D, 16 / 16D),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        return block;
    }
}

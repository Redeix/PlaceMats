package net.placemats.common.data;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.generators.ConfiguredModel;

import net.placemats.common.block.PlaceMatAttachedStairsBlock;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlaceMatStairsBlock;
import net.placemats.compat.tfc.TFCCompat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "unused" })
public final class PlaceMatBlocks {

    private static ResourceLocation getStrippedLogTexture(WoodType woodType) {
        String name = woodType.name();
        if ("bamboo".equals(name)) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/stripped_bamboo_block");
        } else if ("crimson".equals(name) || "warped".equals(name)) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/stripped_" + name + "_stem");
        } else {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/stripped_" + name + "_log");
        }
    }

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
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> WOOD_ORNATE_SHELVES = registerWoodOrnateShelves();
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> ORNATE_SHELVES = WOOD_ORNATE_SHELVES;
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> WOOD_FLOATING_SHELVES = registerWoodFloatingShelves();
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> FLOATING_SHELVES = WOOD_FLOATING_SHELVES;

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

    private static List<BlockEntry<PlaceMatAttachedStairsBlock>> registerWoodOrnateShelves() {
        List<BlockEntry<PlaceMatAttachedStairsBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String name = woodType.name() + "_" + "ornate_shelf";

            BlockEntry<PlaceMatAttachedStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.floatingShelf(name)
                .properties(p -> p.sound(SoundType.WOOD).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
                .blockstate((ctx, prov) -> {
                    String shelf = String.valueOf(prov.modLoc("block/" + woodType.name() + "_shelf"));
                    var straightModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/ornate_shelf_parent")).texture("0", shelf);
                    var straightAttachedModel = prov.models().withExistingParent(ctx.getName() + "_attached", prov.modLoc("block/ornate_shelf_attached_parent")).texture("0", shelf);
                    var innerModel = prov.models().withExistingParent(ctx.getName() + "_inner", prov.modLoc("block/ornate_shelf_inner_parent")).texture("0", shelf);
                    var innerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_inner_attached", prov.modLoc("block/ornate_shelf_inner_attached_parent")).texture("0", shelf);
                    var outerModel = prov.models().withExistingParent(ctx.getName() + "_outer", prov.modLoc("block/ornate_shelf_outer_parent")).texture("0", shelf);
                    var outerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_outer_attached", prov.modLoc("block/ornate_shelf_outer_attached_parent")).texture("0", shelf);

                    prov.getVariantBuilder(ctx.getEntry()).forAllStatesExcept(state -> {
                        Direction facing = state.getValue(PlaceMatAttachedStairsBlock.FACING);
                        StairsShape shape = state.getValue(PlaceMatAttachedStairsBlock.SHAPE);
                        boolean attached = state.getValue(PlaceMatAttachedStairsBlock.ATTACHED);
                        int yRot = (int) facing.getClockWise().toYRot();
                        if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                            yRot += 270;
                        }
                        yRot %= 360;
                        var model = shape == StairsShape.STRAIGHT
                            ? (attached ? straightAttachedModel : straightModel)
                            : (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT
                            ? (attached ? innerAttachedModel : innerModel)
                            : (attached ? outerAttachedModel : outerModel));
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
                    block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
                        new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 9 / 16D, 16 / 16D));
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.ORNATE_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.ORNATE_SHELVES, PlaceMatTags.Items.PLACE_MAT_BLACKLIST)
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "_attached")))
                .build()
                .register();

            list.add(blockReg);
        });

        return list;
    }

    private static List<BlockEntry<PlaceMatAttachedStairsBlock>> registerWoodFloatingShelves() {
        List<BlockEntry<PlaceMatAttachedStairsBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String name = woodType.name() + "_" + "floating_shelf";

            BlockEntry<PlaceMatAttachedStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.floatingShelf(name)
                .properties(p -> p.sound(SoundType.WOOD).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
                .blockstate((ctx, prov) -> {
                    ResourceLocation strippedLog = getStrippedLogTexture(woodType);
                    String shelf = String.valueOf(prov.modLoc("block/" + woodType.name() + "_shelf"));
                    var straightModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/floating_shelf_parent")).texture("0", shelf).texture("1", strippedLog);
                    var straightAttachedModel = prov.models().withExistingParent(ctx.getName() + "_attached", prov.modLoc("block/floating_shelf_attached_parent")).texture("0", shelf).texture("1", strippedLog);
                    var innerModel = prov.models().withExistingParent(ctx.getName() + "_inner", prov.modLoc("block/floating_shelf_inner_parent")).texture("0", shelf).texture("1", strippedLog);
                    var innerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_inner_attached", prov.modLoc("block/floating_shelf_inner_attached_parent")).texture("0", shelf).texture("1", strippedLog);
                    var outerModel = prov.models().withExistingParent(ctx.getName() + "_outer", prov.modLoc("block/floating_shelf_outer_parent")).texture("0", shelf).texture("1", strippedLog);
                    var outerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_outer_attached", prov.modLoc("block/floating_shelf_outer_attached_parent")).texture("0", shelf).texture("1", strippedLog);

                    prov.getVariantBuilder(ctx.getEntry()).forAllStatesExcept(state -> {
                        Direction facing = state.getValue(PlaceMatAttachedStairsBlock.FACING);
                        StairsShape shape = state.getValue(PlaceMatAttachedStairsBlock.SHAPE);
                        boolean attached = state.getValue(PlaceMatAttachedStairsBlock.ATTACHED);
                        int yRot = (int) facing.getClockWise().toYRot();
                        if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                            yRot += 270;
                        }
                        yRot %= 360;
                        var model = shape == StairsShape.STRAIGHT
                            ? (attached ? straightAttachedModel : straightModel)
                            : (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT
                            ? (attached ? innerAttachedModel : innerModel)
                            : (attached ? outerAttachedModel : outerModel));
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
                    block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
                        new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 9 / 16D, 16 / 16D));
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.FLOATING_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.FLOATING_SHELVES, PlaceMatTags.Items.PLACE_MAT_BLACKLIST)
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "_attached")))
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

    public static PlaceMatAttachedStairsBlock createOrnateShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
            new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 16 / 16D, 16 / 16D),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
            new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 9 / 16D, 16 / 16D));
        return block;
    }

    public static PlaceMatAttachedStairsBlock createFloatingShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
            new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 16 / 16D, 16 / 16D),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
            new AABB(0 / 16D, 2 / 16D, 0 / 16D, 16 / 16D, 9 / 16D, 16 / 16D));
        return block;
    }
}

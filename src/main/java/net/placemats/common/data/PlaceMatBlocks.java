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
import net.minecraftforge.client.model.generators.ConfiguredModel;

import net.placemats.common.block.PlaceMatAttachedStairsBlock;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlaceMatStairsBlock;
import net.placemats.compat.tfc.TFCCompat;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.Block.box;
import static net.placemats.common.data.resource.RegistryHelpers.*;

@SuppressWarnings({ "unused" })
public final class PlaceMatBlocks {

    public static final BlockEntry<PlaceMatCardinalBlock> STORAGE_RACK = PlaceMatRegistries.REGISTRATE.storageRack("storage_rack", true)
        .properties(p -> p.sound(SoundType.METAL).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
        .onRegister(block -> {
            block.containerSize(10);
            block.disableZoneRotation();
            block.addRange(basicPlacementRange(box(0, 0, 0, 16, 7, 16), 7 / 16F));
            block.addRange(basicPlacementRange(box(0, 8, 0, 16, 15, 16), 15 / 16F));
        })
        .blockstate((ctx, prov) -> {
            var model = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/storage_rack_parent"))
                    .texture("0", prov.modLoc("block/storage_rack_base"));
            prov.horizontalBlock(ctx.getEntry(), model);
        })
        .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_PICKAXE, PlaceMatTags.Blocks.STORAGE_RACKS, PlaceMatTags.Blocks.DOUBLE_SHELVES)
        .loot(RegistrateBlockLootTables::dropSelf)
        .item()
        .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.STORAGE_RACKS, PlaceMatTags.Items.PLACE_MAT_BLACKLIST, PlaceMatTags.Items.DOUBLE_SHELVES)
        .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/storage_rack")))
        .build()
        .register();

    public static final List<BlockEntry<PlaceMatCardinalBlock>> WOOD_STORAGE_RACKS = registerWoodVariants();
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> WOOD_ORNATE_SHELVES = registerWoodOrnateShelves();
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> ORNATE_SHELVES = WOOD_ORNATE_SHELVES;
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> WOOD_FLOATING_SHELVES = registerWoodFloatingShelves();
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> FLOATING_SHELVES = WOOD_FLOATING_SHELVES;
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> WOOD_ORNATE_DOUBLE_SHELVES = registerWoodOrnateDoubleShelves();
    public static final List<BlockEntry<PlaceMatAttachedStairsBlock>> ORNATE_DOUBLE_SHELVES = WOOD_ORNATE_DOUBLE_SHELVES;

    public static void init() {
    }

    private static List<BlockEntry<PlaceMatCardinalBlock>> registerWoodVariants() {
        List<BlockEntry<PlaceMatCardinalBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String type = "storage_rack";
            String name = woodType.name() + "_" + type;

            BlockEntry<PlaceMatCardinalBlock> blockReg = PlaceMatRegistries.REGISTRATE.storageRack(name, true)
                .properties(p -> p.sound(SoundType.WOOD).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
                .onRegister(block -> {
                    block.containerSize(10);
                    block.disableZoneRotation();
                    block.addRange(basicPlacementRange(box(0, 0, 0, 16, 7, 16), 7 / 16F));
                    block.addRange(basicPlacementRange(box(0, 8, 0, 16, 15, 16), 15 / 16F));
                })
                .blockstate(basicWoodBlockState(woodType, "smooth_shelf", type))
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.STORAGE_RACKS, PlaceMatTags.Blocks.DOUBLE_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.STORAGE_RACKS, PlaceMatTags.Items.PLACE_MAT_BLACKLIST, PlaceMatTags.Items.DOUBLE_SHELVES)
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
                        box(0, 2, 0, 16, 16, 16),
                        16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
                        box(0, 2, 0, 16, 9, 16));
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.ORNATE_SHELVES, PlaceMatTags.Blocks.SINGLE_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.ORNATE_SHELVES, PlaceMatTags.Items.PLACE_MAT_BLACKLIST, PlaceMatTags.Items.SINGLE_SHELVES)
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
                        box(0, 2, 0, 16, 16, 16),
                        16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
                        box(0, 2, 0, 16, 9, 16));
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.FLOATING_SHELVES, PlaceMatTags.Blocks.SINGLE_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.FLOATING_SHELVES, PlaceMatTags.Items.PLACE_MAT_BLACKLIST, PlaceMatTags.Items.SINGLE_SHELVES)
                .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "_attached")))
                .build()
                .register();

            list.add(blockReg);
        });

        return list;
    }

    private static List<BlockEntry<PlaceMatAttachedStairsBlock>> registerWoodOrnateDoubleShelves() {
        List<BlockEntry<PlaceMatAttachedStairsBlock>> list = new ArrayList<>();

        WoodType.values().forEach(woodType -> {
            String name = woodType.name() + "_" + "ornate_double_shelf";

            BlockEntry<PlaceMatAttachedStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.floatingShelf(name)
                .properties(p -> p.sound(SoundType.WOOD).strength(2.0f).noOcclusion().isViewBlocking((state, level, pos) -> false))
                .blockstate((ctx, prov) -> {
                    String shelf = String.valueOf(prov.modLoc("block/" + woodType.name() + "_shelf"));
                    var straightModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/ornate_double_shelf_parent")).texture("0", shelf);
                    var straightAttachedModel = prov.models().withExistingParent(ctx.getName() + "_attached", prov.modLoc("block/ornate_double_shelf_attached_parent")).texture("0", shelf);
                    var innerModel = prov.models().withExistingParent(ctx.getName() + "_inner", prov.modLoc("block/ornate_double_shelf_inner_parent")).texture("0", shelf);
                    var innerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_inner_attached", prov.modLoc("block/ornate_double_shelf_inner_attached_parent")).texture("0", shelf);
                    var outerModel = prov.models().withExistingParent(ctx.getName() + "_outer", prov.modLoc("block/ornate_double_shelf_outer_parent")).texture("0", shelf);
                    var outerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_outer_attached", prov.modLoc("block/ornate_double_shelf_outer_attached_parent")).texture("0", shelf);

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
                    block.cardinalBoundingBox(Block.box(0, 0, 0, 16, 16, 8));
                    block.cardinalCollisionShape(Block.box(0, 0, 0, 16, 10, 8));
                    block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_RIGHT),
                        Block.box(0, 0, 0, 16, 16, 8), Block.box(8, 0, 8, 16, 16, 16));
                    block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_RIGHT),
                        Block.box(0, 0, 0, 16, 10, 8), Block.box(8, 0, 8, 16, 10, 16));
                    block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
                        Block.box(0, 0, 0, 16, 16, 8), Block.box(0, 0, 8, 8, 16, 16));
                    block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
                        Block.box(0, 0, 0, 16, 10, 8), Block.box(0, 0, 8, 8, 10, 16));
                    block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
                        Block.box(0, 0, 0, 8, 16, 8));
                    block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
                        Block.box(0, 0, 0, 8, 10, 8));
                    block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
                        Block.box(8, 0, 0, 16, 16, 8));
                    block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
                        Block.box(8, 0, 0, 16, 10, 8));
                    block.containerSize(10);
                    block.addRange(0, new PlaceMatBlock.PlacementRange(
                        box(0, 2, 0, 16, 8, 8),
                        8 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.addRange(1, new PlaceMatBlock.PlacementRange(
                        box(0, 10, 0, 16, 16, 8),
                        16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.adjustRange(0, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
                        box(0, 2, 0, 8, 8, 8), 8 / 16F);
                    block.adjustRange(1, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
                        box(0, 10, 0, 8, 16, 8), 16 / 16F);
                    block.adjustRange(0, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
                        box(8, 2, 0, 16, 8, 8), 8 / 16F);
                    block.adjustRange(1, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
                        box(8, 10, 0, 16, 16, 8), 16 / 16F);
                    block.addRange(2, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT), new PlaceMatBlock.PlacementRange(
                        box(8, 2, 8, 16, 8, 16),
                        8 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.addRange(3, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT), new PlaceMatBlock.PlacementRange(
                        box(8, 10, 8, 16, 16, 16),
                        16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
                    block.adjustRange(2, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
                        box(0, 2, 8, 8, 8, 16), 8 / 16F);
                    block.adjustRange(3, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
                        box(0, 10, 8, 8, 16, 16), 16 / 16F);
                })
                .tag(PlaceMatTags.Blocks.PLACE_MATS, BlockTags.MINEABLE_WITH_AXE, PlaceMatTags.Blocks.ORNATE_DOUBLE_SHELVES, PlaceMatTags.Blocks.DOUBLE_SHELVES)
                .loot(RegistrateBlockLootTables::dropSelf)
                .item()
                .tag(PlaceMatTags.Items.PLACE_MATS, PlaceMatTags.Items.ORNATE_DOUBLE_SHELVES, PlaceMatTags.Items.PLACE_MAT_BLACKLIST, PlaceMatTags.Items.DOUBLE_SHELVES)
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
        block.disableZoneRotation();
        block.addRange(basicPlacementRange(box(0, 0, 0, 16, 7, 16), 7 / 16F));
        block.addRange(basicPlacementRange(box(0, 8, 0, 16, 15, 16), 15 / 16F));
        return block;
    }

    public static PlaceMatAttachedStairsBlock createOrnateShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
            box(0, 2, 0, 16, 16, 16),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
            box(0, 2, 0, 16, 9, 16));
        return block;
    }

    public static PlaceMatAttachedStairsBlock createFloatingShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
        block.containerSize(10);
        block.addRange(new PlaceMatBlock.PlacementRange(
            box(0, 2, 0, 16, 16, 16),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.adjustRange(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT, StairsShape.OUTER_LEFT, StairsShape.OUTER_RIGHT),
            box(0, 2, 0, 16, 9, 16));
        return block;
    }

    public static PlaceMatAttachedStairsBlock createOrnateDoubleShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.cardinalBoundingBox(Block.box(0, 0, 0, 16, 16, 8));
        block.cardinalCollisionShape(Block.box(0, 0, 0, 16, 10, 8));
        block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_RIGHT),
            Block.box(0, 0, 0, 16, 16, 8), Block.box(8, 0, 8, 16, 16, 16));
        block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_RIGHT),
            Block.box(0, 0, 0, 16, 10, 8), Block.box(8, 0, 8, 16, 10, 16));
        block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
            Block.box(0, 0, 0, 16, 16, 8), Block.box(0, 0, 8, 8, 16, 16));
        block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
            Block.box(0, 0, 0, 16, 10, 8), Block.box(0, 0, 8, 8, 10, 16));
        block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
            Block.box(0, 0, 0, 8, 16, 8));
        block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
            Block.box(0, 0, 0, 8, 10, 8));
        block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
            Block.box(8, 0, 0, 16, 16, 8));
        block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
            Block.box(8, 0, 0, 16, 10, 8));
        block.containerSize(10);
        block.addRange(0, new PlaceMatBlock.PlacementRange(
            box(0, 2, 0, 16, 8, 8),
            8 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.addRange(1, new PlaceMatBlock.PlacementRange(
            box(0, 10, 0, 16, 16, 8),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.adjustRange(0, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
            box(0, 2, 0, 8, 8, 8), 8 / 16F);
        block.adjustRange(1, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT),
            box(0, 10, 0, 8, 16, 8), 16 / 16F);
        block.adjustRange(0, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
            box(8, 2, 0, 16, 8, 8), 8 / 16F);
        block.adjustRange(1, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT),
            box(8, 10, 0, 16, 16, 8), 16 / 16F);
        block.addRange(2, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT), new PlaceMatBlock.PlacementRange(
            box(8, 2, 8, 16, 8, 16),
            8 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.addRange(3, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT), new PlaceMatBlock.PlacementRange(
            box(8, 10, 8, 16, 16, 16),
            16 / 16F, false, false, false, false, true, false, null, false, false, 16, null, false, false, false, false, 1.0f, 0, 0, 0, 0));
        block.adjustRange(2, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
            box(0, 2, 8, 8, 8, 16), 8 / 16F);
        block.adjustRange(3, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT),
            box(0, 10, 8, 8, 16, 16), 16 / 16F);
        return block;
    }
}

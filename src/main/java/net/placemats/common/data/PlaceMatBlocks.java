package net.placemats.common.data;

import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;

import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlaceMatAttachedStairsBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
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
                .properties(WOOD_PROPERTIES)
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
            String type = "ornate_shelf";
            String name = woodType.name() + "_" + type;

            BlockEntry<PlaceMatAttachedStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.floatingShelf(name)
                .properties(WOOD_PROPERTIES)
                .blockstate(attachedStairsBlockState(
                    List.of(
                        new TextureConfig("0", ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,"block/" + woodType.name() + "_shelf"))),
                    type
                ))
                .onRegister(block -> {
                    block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
                    block.containerSize(10);
                    block.disableZoneRotation();
                    block.addRange(basicPlacementRange(box(0, 2, 0, 16, 16, 16), 16 / 16F));
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
            String type = "floating_shelf";
            String name = woodType.name() + "_" + type;
            ResourceLocation strippedLog = getStrippedLogTexture(woodType);

            BlockEntry<PlaceMatAttachedStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.floatingShelf(name)
                .properties(WOOD_PROPERTIES)
                .blockstate(attachedStairsBlockState(
                    List.of(
                        new TextureConfig("0", ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,"block/" + woodType.name() + "_shelf")),
                        new TextureConfig("1", strippedLog)),
                    type
                ))
                .onRegister(block -> {
                    block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
                    block.containerSize(10);
                    block.disableZoneRotation();
                    block.addRange(basicPlacementRange(box(0, 2, 0, 16, 16, 16), 16 / 16F));
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
            String type = "ornate_double_shelf";
            String name = woodType.name() + "_" + type;

            BlockEntry<PlaceMatAttachedStairsBlock> blockReg = PlaceMatRegistries.REGISTRATE.floatingShelf(name)
                .properties(WOOD_PROPERTIES)
                .blockstate(attachedStairsBlockState(
                    List.of(
                        new TextureConfig("0", ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,"block/" + woodType.name() + "_shelf"))),
                    type
                ))
                .onRegister(block -> {
                    block.containerSize(10);
                    stairsShelvesShapes().accept(block);
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
        block.disableZoneRotation();
        block.addRange(basicPlacementRange(box(0, 2, 0, 16, 16, 16), 16 / 16F));
        return block;
    }

    public static PlaceMatAttachedStairsBlock createFloatingShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.collisionShape(Block.box(0, 0, 0, 16, 2, 16));
        block.containerSize(10);
        block.disableZoneRotation();
        block.addRange(basicPlacementRange(box(0, 2, 0, 16, 16, 16), 16 / 16F));
        return block;
    }

    public static PlaceMatAttachedStairsBlock createOrnateDoubleShelf(BlockBehaviour.Properties properties) {
        PlaceMatAttachedStairsBlock block = (PlaceMatAttachedStairsBlock) TFCCompat.INSTANCE.createPlaceMatAttachedStairsBlock(properties);
        block.containerSize(10);
        stairsShelvesShapes().accept(block);
        return block;
    }
}

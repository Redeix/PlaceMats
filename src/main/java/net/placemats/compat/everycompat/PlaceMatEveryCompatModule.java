package net.placemats.compat.everycompat;

import net.mehvahdjukaar.every_compat.api.SimpleEntrySet;
import net.mehvahdjukaar.every_compat.api.SimpleModule;
import net.mehvahdjukaar.moonlight.api.set.wood.VanillaWoodTypes;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatCreativeTab;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.PlaceMatBlocks;

public class PlaceMatEveryCompatModule extends SimpleModule {

    public PlaceMatEveryCompatModule(String modId) {
        super(modId, "pm");
        this.addEntry(SimpleEntrySet.builder(WoodType.class, "storage_rack",
            () -> PlaceMatBlocks.WOOD_STORAGE_RACKS.stream().filter(r -> r.getId().getPath().contains("oak")).findFirst().get().get(), () -> VanillaWoodTypes.OAK,
            w -> PlaceMatBlocks.createStorageRack(Utils.copyPropertySafe(w.planks).noOcclusion().isViewBlocking((state, level, pos) -> false))
            )
            .addTag(BlockTags.MINEABLE_WITH_AXE, Registries.BLOCK)
            .addTag(PlaceMatTags.Blocks.PLACE_MATS, Registries.BLOCK)
            .addTag(PlaceMatTags.Blocks.STORAGE_RACKS, Registries.BLOCK)
            .addTag(PlaceMatTags.Items.PLACE_MATS, Registries.ITEM)
            .addTag(PlaceMatTags.Items.STORAGE_RACKS, Registries.ITEM)
            .addTag(PlaceMatTags.Items.PLACE_MAT_BLACKLIST, Registries.ITEM)
            .addTile(PlaceMatBlockEntities.PLACE_MAT)
            .requiresChildren("slab")
            .setTabKey(PlaceMatCreativeTab.PLACE_MATS.getId())
            .addTexture(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_storage_rack"))
            .includeModelsBlock(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_storage_rack"))
            .includeModelsItem(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "item/oak_storage_rack"))
            .defaultRecipe()
            .build());
        this.addEntry(SimpleEntrySet.builder(WoodType.class, "ornate_shelf",
            () -> PlaceMatBlocks.WOOD_ORNATE_SHELVES.stream().filter(r -> r.getId().getPath().contains("oak")).findFirst().get().get(), () -> VanillaWoodTypes.OAK,
            w -> PlaceMatBlocks.createOrnateShelf(Utils.copyPropertySafe(w.planks).noOcclusion().isViewBlocking((state, level, pos) -> false))
            )
            .addTag(BlockTags.MINEABLE_WITH_AXE, Registries.BLOCK)
            .addTag(PlaceMatTags.Blocks.PLACE_MATS, Registries.BLOCK)
            .addTag(PlaceMatTags.Blocks.ORNATE_SHELVES, Registries.BLOCK)
            .addTag(PlaceMatTags.Items.PLACE_MATS, Registries.ITEM)
            .addTag(PlaceMatTags.Items.ORNATE_SHELVES, Registries.ITEM)
            .addTag(PlaceMatTags.Items.PLACE_MAT_BLACKLIST, Registries.ITEM)
            .addTile(PlaceMatBlockEntities.PLACE_MAT)
            .requiresChildren("slab")
            .setTabKey(PlaceMatCreativeTab.PLACE_MATS.getId())
            .addTexture(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_shelf"))
            .includeModelsBlock(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_ornate_shelf"))
            .includeModelsBlock(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_ornate_shelf_inner"))
            .includeModelsBlock(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_ornate_shelf_outer"))
            .includeModelsItem(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "item/oak_ornate_shelf"))
            .defaultRecipe()
            .build());
    }
}

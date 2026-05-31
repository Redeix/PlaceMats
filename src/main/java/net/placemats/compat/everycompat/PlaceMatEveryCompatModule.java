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
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatEveryCompatModule extends SimpleModule {

    public PlaceMatEveryCompatModule(String modId) {
        super(modId, "pm");
        this.addEntry(SimpleEntrySet.builder(WoodType.class, "storage_rack",
            PlaceMatBlocks.OAK_STORAGE_RACK, () -> VanillaWoodTypes.OAK,
            w -> PlaceMatBlocks.createStorageRack(Utils.copyPropertySafe(w.planks).noOcclusion().isViewBlocking((state, level, pos) -> false))
            )
            .addTag(BlockTags.MINEABLE_WITH_AXE, Registries.BLOCK)
            .addTag(PlaceMatTags.Blocks.PLACE_MATS, Registries.BLOCK)
            .addTile(PlaceMatBlockEntities.PLACE_MAT)
            .requiresChildren("slab")
            .setTabKey(PlaceMatCreativeTab.PLACE_MATS.getId())
            .addTexture(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_storage_rack"))
            .includeModelsBlock(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/oak_storage_rack"))
            .includeModelsItem(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "item/oak_storage_rack"))
            .defaultRecipe()
            .build());
    }
}

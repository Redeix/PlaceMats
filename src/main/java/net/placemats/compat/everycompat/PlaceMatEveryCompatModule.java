package net.placemats.compat.everycompat;

import net.mehvahdjukaar.every_compat.api.SimpleEntrySet;
import net.mehvahdjukaar.every_compat.api.SimpleModule;
import net.mehvahdjukaar.moonlight.api.set.wood.VanillaWoodTypes;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatCreativeTab;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatEveryCompatModule extends SimpleModule {

    public PlaceMatEveryCompatModule(String modId) {
        super(modId, "pm");
        this.addEntry(SimpleEntrySet.builder(WoodType.class, "storage_rack",
                PlaceMatBlocks.OAK_STORAGE_RACK, () -> VanillaWoodTypes.OAK,
                w -> new PlaceMatBlock(Utils.copyPropertySafe(w.planks).noOcclusion())
                )
                .addTag(BlockTags.MINEABLE_WITH_AXE, Registries.BLOCK)
                .addTile(PlaceMatBlockEntities.PLACE_MAT)
                .setTabKey(PlaceMatCreativeTab.PLACE_MATS.getId())
                .addTexture(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/storage_racks/storage_rack_oak"))
                .includeModelsBlock(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/storage_rack"))
                .includeModelsItem(ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "item/storage_rack"))
                .defaultRecipe()
                .build());
    }
}

package net.placemats.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class ItemModelProvider extends net.minecraftforge.client.model.generators.ItemModelProvider {
    public ItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(PlaceMatBlocks.STORAGE_RACK_ITEM.getId().getPath(), modLoc("block/storage_rack"));
        withExistingParent(PlaceMatBlocks.OAK_STORAGE_RACK_ITEM.getId().getPath(), modLoc("block/oak_storage_rack"));
    }
}

package net.placemats.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class ItemModelProvider extends net.minecraftforge.client.model.generators.ItemModelProvider {
    public ItemModelProvider(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        assert PlaceMatBlocks.STORAGE_RACK_ITEM.getId() != null;
        getBuilder(PlaceMatBlocks.STORAGE_RACK_ITEM.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/storage_rack")));

        PlaceMatBlocks.WOOD_STORAGE_RACKS.forEach(blockReg -> {
            assert blockReg.getId() != null;
            String path = blockReg.getId().getPath();
            getBuilder(path).parent(new ModelFile.UncheckedModelFile(modLoc("block/" + path)));
        });
    }
}

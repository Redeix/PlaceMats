package net.placemats.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class BlockModelProvider extends net.minecraftforge.client.model.generators.BlockModelProvider {
    public BlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        assert PlaceMatBlocks.STORAGE_RACK.getId() != null;
        withExistingParent(PlaceMatBlocks.STORAGE_RACK.getId().getPath(), modLoc("block/storage_rack_parent"))
                .texture("1", modLoc("block/storage_rack_base"));

        PlaceMatBlocks.WOOD_STORAGE_RACKS.forEach(blockReg -> {
            assert blockReg.getId() != null;
            String path = blockReg.getId().getPath();
            withExistingParent(path, modLoc("block/storage_rack_parent"))
                    .texture("1", modLoc("block/" + path));
        });
    }
}

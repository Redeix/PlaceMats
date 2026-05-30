package net.placemats.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class BlockStateProvider extends net.minecraftforge.client.model.generators.BlockStateProvider {
    public BlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, PlaceMatMain.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(PlaceMatBlocks.STORAGE_RACK.get(), models().getExistingFile(modLoc("block/storage_rack")));
        horizontalBlock(PlaceMatBlocks.OAK_STORAGE_RACK.get(), models().getExistingFile(modLoc("block/oak_storage_rack")));
    }
}

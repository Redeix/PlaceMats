package net.placemats.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

import net.placemats.PlaceMatMain;

public class BlockModelProvider extends net.minecraftforge.client.model.generators.BlockModelProvider {
    public BlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

    }
}

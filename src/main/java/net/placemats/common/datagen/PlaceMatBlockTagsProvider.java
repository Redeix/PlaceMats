package net.placemats.common.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatBlockTagsProvider extends BlockTagsProvider {
    public PlaceMatBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(PlaceMatTags.Blocks.PLACE_MATS)
                .add(PlaceMatBlocks.STORAGE_RACK.get());

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(PlaceMatBlocks.STORAGE_RACK.get());
    }
}

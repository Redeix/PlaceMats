package net.placemats.common.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatItemTagsProvider extends ItemTagsProvider {
    public PlaceMatItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(PlaceMatTags.Blocks.PLACE_MATS, PlaceMatTags.Items.PLACE_MATS);
        tag(PlaceMatTags.Items.PLACE_MATS).add(PlaceMatBlocks.STORAGE_RACK_ITEM.get());
        tag(PlaceMatTags.Items.PLACE_MATS).add(PlaceMatBlocks.OAK_STORAGE_RACK_ITEM.get());
    }
}

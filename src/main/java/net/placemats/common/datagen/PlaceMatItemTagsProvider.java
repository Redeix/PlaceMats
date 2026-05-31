package net.placemats.common.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatItemTagsProvider extends ItemTagsProvider {
    public PlaceMatItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, PlaceMatMain.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        copy(PlaceMatTags.Blocks.PLACE_MATS, PlaceMatTags.Items.PLACE_MATS);
        copy(PlaceMatTags.Blocks.STORAGE_RACKS, PlaceMatTags.Items.STORAGE_RACKS);

        tag(PlaceMatTags.Items.PLACE_MAT_BLACKLIST).addTag(PlaceMatTags.Items.PLACE_MATS);
    }
}

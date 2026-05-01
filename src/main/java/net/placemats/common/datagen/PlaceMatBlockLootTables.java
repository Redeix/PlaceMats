package net.placemats.common.datagen;

import java.util.Set;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatBlockLootTables extends BlockLootSubProvider {
    protected PlaceMatBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(PlaceMatBlocks.STORAGE_RACK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return PlaceMatBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}

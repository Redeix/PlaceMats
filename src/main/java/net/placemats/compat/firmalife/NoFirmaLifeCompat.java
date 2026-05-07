package net.placemats.compat.firmalife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.placemats.common.blockentity.PlaceMatBlockEntity;

import net.placemats.common.data.PlaceMatBlockEntities;

public class NoFirmaLifeCompat implements FirmaLifeCompat {
    @Override public float getCellarLevel3Temperature() { return 0; }
    @Override public float getCellarLevel2Temperature() { return 0; }
    @Override public Object getShelvedTrait() { return null; }
    @Override public Object getShelved2Trait() { return null; }
    @Override public Object getShelved3Trait() { return null; }
    @Override public Object[] getPossibleShelvedTraits() { return new Object[0]; }
    @Override public PlaceMatBlockEntity createPlaceMatBE(BlockPos pos, BlockState state) {
        return new PlaceMatBlockEntity(PlaceMatBlockEntities.PLACE_MAT.get(), pos, state);
    }
}

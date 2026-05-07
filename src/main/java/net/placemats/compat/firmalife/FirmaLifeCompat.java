package net.placemats.compat.firmalife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.compat.ModCompat;

public interface FirmaLifeCompat {
    FirmaLifeCompat INSTANCE = ModCompat.FIRMALIFE_LOADED ? new FirmaLifeCompatImpl() : new NoFirmaLifeCompat();

    float getCellarLevel3Temperature();
    float getCellarLevel2Temperature();
    Object getShelvedTrait();
    Object getShelved2Trait();
    Object getShelved3Trait();
    Object[] getPossibleShelvedTraits();
    PlaceMatBlockEntity createPlaceMatBE(BlockPos pos, BlockState state);
}

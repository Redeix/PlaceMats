package net.placemats.compat.firmalife;

import com.eerussianguy.firmalife.config.FLConfig;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.placemats.common.blockentity.PlaceMatBlockEntity;

import net.placemats.common.data.PlaceMatBlockEntities;

public class FirmaLifeCompatImpl implements FirmaLifeCompat {
    @Override public float getCellarLevel3Temperature() { return FLConfig.SERVER.cellarLevel3Temperature.get().floatValue(); }
    @Override public float getCellarLevel2Temperature() { return FLConfig.SERVER.cellarLevel2Temperature.get().floatValue(); }
    @Override public Object getShelvedTrait() { return FLFoodTraits.SHELVED; }
    @Override public Object getShelved2Trait() { return FLFoodTraits.SHELVED_2; }
    @Override public Object getShelved3Trait() { return FLFoodTraits.SHELVED_3; }
    @Override public Object[] getPossibleShelvedTraits() {
        return new Object[] { FLFoodTraits.SHELVED, FLFoodTraits.SHELVED_2, FLFoodTraits.SHELVED_3, FLFoodTraits.HUNG, FLFoodTraits.HUNG_2, FLFoodTraits.HUNG_3 };
    }
    @Override public PlaceMatBlockEntity createPlaceMatBE(BlockPos pos, BlockState state) {
        return new FirmaLifePlaceMatBlockEntity(PlaceMatBlockEntities.PLACE_MAT.get(), pos, state);
    }
}

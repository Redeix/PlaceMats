package net.placemats.compat.firmalife;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import org.jetbrains.annotations.NotNull;

public class FirmaLifePlaceMatBlockEntity extends PlaceMatBlockEntity implements ClimateReceiver {
    public FirmaLifePlaceMatBlockEntity(BlockEntityType<PlaceMatBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setValid(@NotNull Level level, @NotNull BlockPos pos, boolean valid, int tier, @NotNull ClimateType climate) {
        this.setClimateValid(valid, climate == ClimateType.CELLAR);
    }
}

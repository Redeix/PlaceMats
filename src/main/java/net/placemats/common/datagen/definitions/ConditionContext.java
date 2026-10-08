package net.placemats.common.datagen.definitions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.placemats.common.block.IPlacementRangeProvider;
import net.placemats.common.block.PlacementRange;

public record ConditionContext(
        ItemStack stack,
        @Nullable BlockState state,
        @Nullable PlacementRange range,
        int rangeIndex) {

    public static ConditionContext of(ItemStack stack) {
        return new ConditionContext(stack, null, null, -1);
    }

    public static ConditionContext of(ItemStack stack, @Nullable BlockState state, @Nullable PlacementRange range) {
        int index = -1;
        if (state != null && state.getBlock() instanceof IPlacementRangeProvider provider && range != null) {
            index = provider.getPlacementRanges(state).indexOf(range);
        }
        return new ConditionContext(stack, state, range, index);
    }

    public static ConditionContext of(ItemStack stack, @Nullable BlockState state, @Nullable PlacementRange range, int rangeIndex) {
        return new ConditionContext(stack, state, range, rangeIndex);
    }
}

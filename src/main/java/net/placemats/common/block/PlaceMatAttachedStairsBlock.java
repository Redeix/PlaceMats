package net.placemats.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused" })
public class PlaceMatAttachedStairsBlock extends PlaceMatStairsBlock {

    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;

    public PlaceMatAttachedStairsBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, StairsShape.STRAIGHT)
                .setValue(ATTACHED, false)
                .setValue(LOCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ATTACHED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Direction clickedFace = context.getClickedFace();
        boolean attached = false;
        if (clickedFace.getAxis().isHorizontal()) {
            Level level = context.getLevel();
            BlockPos clickedPos = context.getClickedPos().relative(clickedFace.getOpposite());
            BlockState clickedState = level.getBlockState(clickedPos);
            attached = clickedState.isFaceSturdy(level, clickedPos, clickedFace);
        }

        return state.setValue(ATTACHED, attached);
    }
}

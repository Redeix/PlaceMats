package net.placemats.common.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Two-block tall apron table block that forms multiblock tables with corner and perimeter models.
 */
@SuppressWarnings("unused")
public class PlaceMatApronTallTableBlock extends PlaceMatTallTableBlock {

    public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
    public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
    public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");
    public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");

    public PlaceMatApronTallTableBlock(Properties properties, Supplier<? extends Block> topBlockSupplier) {
        super(properties, topBlockSupplier);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH_EAST, false)
                .setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false)
                .setValue(NORTH_WEST, false));
    }

    public PlaceMatApronTallTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH_EAST, false)
                .setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false)
                .setValue(NORTH_WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH_EAST, SOUTH_EAST, SOUTH_WEST, NORTH_WEST);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        BlockState updatedState = super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        if (facing.getAxis().isHorizontal()) {
            return makeDiagonalConnections(updatedState, level, currentPos);
        }
        return updatedState;
    }

    @Override
    public BlockState makeConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return makeDiagonalConnections(super.makeConnections(state, level, pos), level, pos);
    }

    public BlockState makeDiagonalConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH_EAST, connectsTo(level.getBlockState(pos.north().east()), state))
                .setValue(SOUTH_EAST, connectsTo(level.getBlockState(pos.south().east()), state))
                .setValue(SOUTH_WEST, connectsTo(level.getBlockState(pos.south().west()), state))
                .setValue(NORTH_WEST, connectsTo(level.getBlockState(pos.north().west()), state));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        return switch (rotation) {
            case CLOCKWISE_90 -> rotated
                    .setValue(NORTH_EAST, state.getValue(NORTH_WEST))
                    .setValue(SOUTH_EAST, state.getValue(NORTH_EAST))
                    .setValue(SOUTH_WEST, state.getValue(SOUTH_EAST))
                    .setValue(NORTH_WEST, state.getValue(SOUTH_WEST));
            case CLOCKWISE_180 -> rotated
                    .setValue(NORTH_EAST, state.getValue(SOUTH_WEST))
                    .setValue(SOUTH_EAST, state.getValue(NORTH_WEST))
                    .setValue(SOUTH_WEST, state.getValue(NORTH_EAST))
                    .setValue(NORTH_WEST, state.getValue(SOUTH_EAST));
            case COUNTERCLOCKWISE_90 -> rotated
                    .setValue(NORTH_EAST, state.getValue(SOUTH_EAST))
                    .setValue(SOUTH_EAST, state.getValue(SOUTH_WEST))
                    .setValue(SOUTH_WEST, state.getValue(NORTH_WEST))
                    .setValue(NORTH_WEST, state.getValue(NORTH_EAST));
            default -> rotated;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return switch (mirror) {
            case LEFT_RIGHT -> mirrored
                    .setValue(NORTH_EAST, state.getValue(SOUTH_EAST))
                    .setValue(SOUTH_EAST, state.getValue(NORTH_EAST))
                    .setValue(NORTH_WEST, state.getValue(SOUTH_WEST))
                    .setValue(SOUTH_WEST, state.getValue(NORTH_WEST));
            case FRONT_BACK -> mirrored
                    .setValue(NORTH_EAST, state.getValue(NORTH_WEST))
                    .setValue(NORTH_WEST, state.getValue(NORTH_EAST))
                    .setValue(SOUTH_EAST, state.getValue(SOUTH_WEST))
                    .setValue(SOUTH_WEST, state.getValue(SOUTH_EAST));
            default -> mirrored;
        };
    }
}

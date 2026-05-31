package net.placemats.common.block;

import java.util.ArrayList;
import java.util.List;

import net.placemats.common.event.PlaceMatInteractions;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatTags;

@SuppressWarnings({ "deprecation", "UnusedReturnValue", "unused" })
public class PlaceMatBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // Defines the bounding box where items can be placed.

    @Getter
    private int containerSize = 12;
    @Getter
    private final List<PlacementRange> placementRanges = new ArrayList<>();

    @Getter
    private boolean extractionDisabled = false;
    @Getter
    private boolean insertionDisabled = false;
    @Getter
    private int maxStackSize = 64;
    @Getter
    private boolean disableLayFlat = false;
    @Getter
    private boolean disableCustomModels = false;
    @Getter
    private float scaleMultiplier = 1.0f;
    @Getter
    private float defaultYaw = 0;
    @Getter
    private float defaultPitch = 0;
    @Getter
    private float defaultRoll = 0;
    @Getter
    private float defaultElevation = 0;
    @Getter
    @Nullable
    private ResourceLocation foodTrait = null;

    public PlaceMatBlock(Properties properties) {
        super(properties);
        BlockState defaultState = getStateDefinition().any();
        if (defaultState.hasProperty(FACING)) {
            defaultState = defaultState.setValue(FACING, Direction.NORTH);
        }
        registerDefaultState(defaultState);
    }

    public PlaceMatBlock containerSize(int size) {
        this.containerSize = size;
        return this;
    }

    public PlaceMatBlock disableExtraction() {
        this.extractionDisabled = true;
        return this;
    }

    public PlaceMatBlock disableInsertion() {
        this.insertionDisabled = true;
        return this;
    }

    public PlaceMatBlock maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    public PlaceMatBlock applyFoodTrait(ResourceLocation trait) {
        this.foodTrait = trait;
        return this;
    }

    public PlaceMatBlock disableLayFlat() {
        this.disableLayFlat = true;
        return this;
    }

    public PlaceMatBlock disableCustomModels() {
        this.disableCustomModels = true;
        return this;
    }

    public PlaceMatBlock scaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier;
        return this;
    }

    public PlaceMatBlock defaultRotation(float yaw, float pitch, float roll) {
        this.defaultYaw = yaw;
        this.defaultPitch = pitch;
        this.defaultRoll = roll;
        return this;
    }

    public PlaceMatBlock defaultElevation(float elevation) {
        this.defaultElevation = elevation;
        return this;
    }

    public PlaceMatBlock addRange(PlacementRange range) {
        this.placementRanges.add(range);
        return this;
    }

    public record PlacementRange(
            AABB box,
            float maxHeight,
            boolean rollDisabled,
            boolean yawDisabled,
            boolean pitchDisabled,
            boolean elevationDisabled,
            boolean stackingEnabled,
            boolean collisionDisabled,
            @Nullable TagKey<Item> whitelistTag,
            boolean extractionDisabled,
            boolean insertionDisabled,
            int maxStackSize,
            @Nullable ResourceLocation foodTrait,
            boolean restricted,
            boolean disableLayFlat,
            boolean disableCustomModels,
            boolean snapToCenter,
            float scaleMultiplier,
            float defaultYaw,
            float defaultPitch,
            float defaultRoll,
            float defaultElevation) {
    }

    public void addPlacementRanges(BlockState state, java.util.function.Consumer<PlacementRange> consumer) {
        placementRanges.forEach(consumer);
    }

    @Nullable
    public PlacementRange getTargetedPlacementRange(BlockState state, Vec3 relativeHitVec) {
        PlacementRange best = null;
        double minDistance = Double.MAX_VALUE;

        Vec3 localHitVec = getLocalHitVec(state, relativeHitVec);

        for (PlacementRange range : placementRanges) {
            if (localHitVec.x >= range.box.minX && localHitVec.x <= range.box.maxX &&
                    localHitVec.z >= range.box.minZ && localHitVec.z <= range.box.maxZ) {
                if (localHitVec.y >= range.box.minY && localHitVec.y <= range.box.maxY) {
                    return range;
                }
                double centerBoxY = (range.box.minY + range.box.maxY) / 2.0;
                double distY = Math.abs(localHitVec.y - centerBoxY);
                if (distY < minDistance) {
                    minDistance = distY;
                    best = range;
                }
            }
        }

        if (best != null) {
            return best;
        }

        for (PlacementRange range : placementRanges) {
            double dist = getDistanceToBoxSqr(localHitVec, range.box);
            if (dist < minDistance) {
                minDistance = dist;
                best = range;
            }
        }

        return best;
    }

    // Okay, this sucks. But otherwise snapping to restricted zones feels very clunky
    private static double getDistanceToBoxSqr(Vec3 point, AABB box) {
        double dx = point.x < box.minX ? box.minX - point.x : (point.x > box.maxX ? point.x - box.maxX : 0);
        double dy = point.y < box.minY ? box.minY - point.y : (point.y > box.maxY ? point.y - box.maxY : 0);
        double dz = point.z < box.minZ ? box.minZ - point.z : (point.z > box.maxZ ? point.z - box.maxZ : 0);
        return dx * dx + dy * dy + dz * dz;
    }

    public static Vec3 getLocalHitVec(BlockState state, Vec3 relativeHitVec) {
        if (!state.hasProperty(FACING)) {
            return relativeHitVec;
        }
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case SOUTH -> new Vec3(1 - relativeHitVec.x, relativeHitVec.y, 1 - relativeHitVec.z);
            case EAST -> new Vec3(relativeHitVec.z, relativeHitVec.y, 1 - relativeHitVec.x);
            case WEST -> new Vec3(1 - relativeHitVec.z, relativeHitVec.y, relativeHitVec.x);
            default -> relativeHitVec;
        };
    }

    public static Vec3 getWorldHitVec(BlockState state, Vec3 localHitVec) {
        if (!state.hasProperty(FACING)) {
            return localHitVec;
        }
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case SOUTH -> new Vec3(1 - localHitVec.x, localHitVec.y, 1 - localHitVec.z);
            case WEST -> new Vec3(localHitVec.z, localHitVec.y, 1 - localHitVec.x);
            case EAST -> new Vec3(1 - localHitVec.z, localHitVec.y, localHitVec.x);
            default -> localHitVec;
        };
    }

    public static Vec3 rotateDirection(Direction facing, Vec3 dir) {
        return switch (facing) {
            case SOUTH -> new Vec3(-dir.x, dir.y, -dir.z);
            case WEST -> new Vec3(dir.z, dir.y, -dir.x);
            case EAST -> new Vec3(-dir.z, dir.y, dir.x);
            default -> dir;
        };
    }

    public static Vec3 rotateDirectionInverse(Direction facing, Vec3 dir) {
        return switch (facing) {
            case SOUTH -> new Vec3(-dir.x, dir.y, -dir.z);
            case WEST -> new Vec3(-dir.z, dir.y, dir.x);
            case EAST -> new Vec3(dir.z, dir.y, -dir.x);
            default -> dir;
        };
    }

    public static void addPlacementRangesStatic(BlockState state, java.util.function.Consumer<PlacementRange> consumer) {
        if (state.getBlock() instanceof PlaceMatBlock fpb) {
            fpb.addPlacementRanges(state, consumer);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlaceMatBlockEntity foodPlacer) {
            InteractionResult result = PlaceMatInteractions.handleInteraction(foodPlacer, player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
            Vec3 location = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            PlacementRange targetedRange = getTargetedPlacementRange(state, location);
            if (targetedRange != null) {
                if (targetedRange.whitelistTag() == null || held.is(targetedRange.whitelistTag())) {
                    return InteractionResult.SUCCESS;
                }
            } else if (placementRanges.isEmpty()) {
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return PlaceMatBlockEntities.PLACE_MAT.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    public static class Cardinal extends PlaceMatBlock {

        public Cardinal(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }


    // Method for pick-block item cloning when looking at placed items.
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult blockHit && level.getBlockEntity(pos) instanceof PlaceMatBlockEntity foodPlacer) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            PlaceMatBlockEntity.PlacedItem targeted = foodPlacer.getTargetedItem(eyePos, lookVec, pos);
            if (targeted != null) {
                return targeted.stack.copy();
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlaceMatBlockEntity foodPlacer) {
                foodPlacer.dropItems();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PlaceMatBlockEntity foodPlacer) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            if (foodPlacer.getTargetedItem(eyePos, lookVec, pos) != null) {
                return 0.0f;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PlaceMatBlockEntities.PLACE_MAT.get() ? (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof PlaceMatBlockEntity be) {
                be.tick(level1, pos, state1);
            }
        } : null;
    }
}

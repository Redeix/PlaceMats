package net.placemats.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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

/**
 * Base class for place mats, which are blocks that can hold items.
 * Provides consumers for individual placement ranges and for the block as a whole.
 * Settings for the block (such as max stack size) take priority over individual placement range settings.
 */
@SuppressWarnings({ "deprecation", "UnusedReturnValue", "unused" })
public class PlaceMatBlock extends Block implements EntityBlock {

    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;

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
    private boolean rotateZones = true;
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
    @Getter
    private VoxelShape shape = Shapes.block();
    @Getter
    private VoxelShape collisionShape = Shapes.block();

    public PlaceMatBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(LOCKED, false));
    }

    /**
     * Sets the container size which limits the amount of item stacks within the block's inventory.
     */
    public PlaceMatBlock containerSize(int size) {
        this.containerSize = size;
        return this;
    }

    /**
     * Disables the ability to extract item stacks from the block's inventory.
     */
    public PlaceMatBlock disableExtraction() {
        this.extractionDisabled = true;
        return this;
    }

    /**
     * Disables the ability to insert item stacks into the block's inventory.
     */
    public PlaceMatBlock disableInsertion() {
        this.insertionDisabled = true;
        return this;
    }

    /**
     * Sets the maximum stack size for individual item stacks within the block's inventory.
     */
    public PlaceMatBlock maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    /**
     * If TFC compat is enabled, applies a specified food trait to all food items placed on the block.
     */
    public PlaceMatBlock applyFoodTrait(ResourceLocation trait) {
        this.foodTrait = trait;
        return this;
    }

    /**
     * Default items will render laying flat on the block. This setting will make them upright instead.
     * Useful when you want to display things like item frames.
     */
    public PlaceMatBlock disableLayFlat() {
        this.disableLayFlat = true;
        return this;
    }

    /**
     * Disables rendering for custom models defined with a custom model JSON file.
     * Useful when you want items to display normally.
     */
    public PlaceMatBlock disableCustomModels() {
        this.disableCustomModels = true;
        return this;
    }

    /**
     * Sets whether placement zones should rotate with the block facing or stay fixed.
     * Default is true (rotation enabled).
     */
    public PlaceMatBlock rotateZones(boolean rotateZones) {
        this.rotateZones = rotateZones;
        return this;
    }

    /**
     * Disables zone rotation with block facing so placement zones stay fixed.
     */
    public PlaceMatBlock disableZoneRotation() {
        this.rotateZones = false;
        return this;
    }

    /**
     * Sets a scale multiplier for all rendered models.
     */
    public PlaceMatBlock scaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier;
        return this;
    }

    /**
     * Sets a default rotation for all rendered models.
     */
    public PlaceMatBlock defaultRotation(float yaw, float pitch, float roll) {
        this.defaultYaw = yaw;
        this.defaultPitch = pitch;
        this.defaultRoll = roll;
        return this;
    }

    /**
     * Sets a default elevation (y-offset) for all rendered models.
     */
    public PlaceMatBlock defaultElevation(float elevation) {
        this.defaultElevation = elevation;
        return this;
    }

    public PlaceMatBlock shape(VoxelShape shape) {
        this.shape = shape;
        return this;
    }

    public PlaceMatBlock shape(AABB box) {
        this.shape = Shapes.create(box);
        return this;
    }

    public PlaceMatBlock collisionShape(VoxelShape collisionShape) {
        this.collisionShape = collisionShape;
        return this;
    }

    public PlaceMatBlock collisionShape(AABB box) {
        this.collisionShape = Shapes.create(box);
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

    public void addPlacementRanges(BlockState state, Consumer<PlacementRange> consumer) {
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

    // Okay, this sucks. But otherwise snapping to restricted zones feels very clunky.
    private static double getDistanceToBoxSqr(Vec3 point, AABB box) {
        double dx = point.x < box.minX ? box.minX - point.x : (point.x > box.maxX ? point.x - box.maxX : 0);
        double dy = point.y < box.minY ? box.minY - point.y : (point.y > box.maxY ? point.y - box.maxY : 0);
        double dz = point.z < box.minZ ? box.minZ - point.z : (point.z > box.maxZ ? point.z - box.maxZ : 0);
        return dx * dx + dy * dy + dz * dz;
    }

    public static Vec3 getLocalHitVec(BlockState state, Vec3 relativeHitVec) {
        if (!state.hasProperty(PlaceMatCardinalBlock.FACING)) {
            return relativeHitVec;
        }
        if (state.getBlock() instanceof PlaceMatBlock pmb && !pmb.isRotateZones()) {
            return relativeHitVec;
        }
        Direction facing = state.getValue(PlaceMatCardinalBlock.FACING);
        return switch (facing) {
            case SOUTH -> new Vec3(1 - relativeHitVec.x, relativeHitVec.y, 1 - relativeHitVec.z);
            case EAST -> new Vec3(relativeHitVec.z, relativeHitVec.y, 1 - relativeHitVec.x);
            case WEST -> new Vec3(1 - relativeHitVec.z, relativeHitVec.y, relativeHitVec.x);
            default -> relativeHitVec;
        };
    }

    public static Vec3 getWorldHitVec(BlockState state, Vec3 localHitVec) {
        if (!state.hasProperty(PlaceMatCardinalBlock.FACING)) {
            return localHitVec;
        }
        if (state.getBlock() instanceof PlaceMatBlock pmb && !pmb.isRotateZones()) {
            return localHitVec;
        }
        Direction facing = state.getValue(PlaceMatCardinalBlock.FACING);
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

    public static void addPlacementRangesStatic(BlockState state, Consumer<PlacementRange> consumer) {
        if (state.getBlock() instanceof PlaceMatBlock fpb) {
            fpb.addPlacementRanges(state, consumer);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        boolean currentState = state.getValue(LOCKED);

        if (be instanceof PlaceMatBlockEntity pmbe && !pmbe.getBlockState().getValue(LOCKED).equals(true)) {
            InteractionResult result = PlaceMatInteractions.handleInteraction(pmbe, player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST) && !state.getValue(LOCKED).equals(true)) {
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

        if (held.is(PlaceMatTags.Items.KEY) && level instanceof ServerLevel serverLevel) {
            boolean newState = !currentState;
            state = state.setValue(LOCKED, newState);

            if (be instanceof PlaceMatBlockEntity pmbe) {
                if (newState) {
                    pmbe.setLockedBy(player.getUUID());
                } else if (player.getUUID() == pmbe.getLockedBy()) {
                    pmbe.setLockedBy(null);
                }
                pmbe.setChanged();
            }

            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 2f, 0.5f);
            serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
            level.setBlockAndUpdate(pos, state);

            return InteractionResult.SUCCESS;
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
        builder.add(LOCKED);
    }

    // Method for pick-block item cloning when looking at placed items.
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult blockHit && level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            PlaceMatBlockEntity.PlacedItem targeted = pmbe.getTargetedItem(eyePos, lookVec, pos);
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
            if (be instanceof PlaceMatBlockEntity pmbe) {
                pmbe.dropItems();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            if (pmbe.getTargetedItem(eyePos, lookVec, pos) != null || pmbe.getBlockState().getValue(LOCKED).equals(true)) {
                return 0.0f;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.collisionShape;
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

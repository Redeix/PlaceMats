package net.placemats.common.block;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatTags;

/**
 * Top block for two-block tall place mats.
 * Invisible, placement range whose survival and locked state are linked to the bottom table block.
 */
@SuppressWarnings("deprecation")
public class InvisibleMatBlock extends PlaceMatBlock {

    public InvisibleMatBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof PlaceMatTallTableBlock;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.DOWN && !(facingState.getBlock() instanceof PlaceMatTallTableBlock)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    /**
     * In order to prevent the shape from always appearing and getting in the way,
     * we only show the shape when a player is holding a valid placeable item or key.
     * Otherwise, we only add shapes for currently placed items.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof LivingEntity living && living instanceof Player player) {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();

                boolean hasPlaceableMain = !main.isEmpty() && !main.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST);
                boolean hasPlaceableOff = !off.isEmpty() && !off.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST);
                boolean isHoldingKey = main.is(PlaceMatTags.Items.KEY) || off.is(PlaceMatTags.Items.KEY);

                if (isHoldingKey || hasPlaceableMain || hasPlaceableOff) {
                    return Shapes.block();
                }
            }

            var placedItems = pmbe.getPlacedItems();
            if (placedItems.isEmpty()) {
                return Shapes.empty();
            }

            VoxelShape combined = Shapes.empty();
            Direction facing = state.hasProperty(PlaceMatCardinalBlock.FACING) ? state.getValue(PlaceMatCardinalBlock.FACING) : Direction.NORTH;
            boolean rotate = !(state.getBlock() instanceof PlaceMatBlock pmb && !pmb.isRotateZones());

            for (PlaceMatBlockEntity.PlacedItem item : placedItems) {
                AABB box = item.getVisualBox();
                if (rotate && facing != Direction.NORTH) {
                    box = PlaceMatShapeHelper.rotateAABB(facing, box);
                }
                combined = Shapes.or(combined, Shapes.create(box));
            }
            return combined.isEmpty() ? Shapes.empty() : combined;
        }
        return Shapes.empty();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (fromPos.equals(pos.below())) {
            BlockState belowState = level.getBlockState(fromPos);
            if (belowState.getBlock() instanceof PlaceMatTallTableBlock) {
                LockType belowLock = PlaceMatBlock.getLockType(belowState);
                LockType currentLock = PlaceMatBlock.getLockType(state);
                if (belowLock.isLocked()) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof PlaceMatBlockEntity pmbe && pmbe.getPlacedItems().isEmpty()) {
                        level.removeBlock(pos, false);
                        return;
                    }
                }
                if (currentLock != belowLock) {
                    BlockEntity be = level.getBlockEntity(pos);
                    BlockEntity belowBE = level.getBlockEntity(fromPos);
                    UUID owner = belowBE instanceof PlaceMatBlockEntity belowPMBE ? belowPMBE.getLockedBy() : null;
                    if (be instanceof PlaceMatBlockEntity pmbe) {
                        pmbe.setLockedBy(owner);
                        pmbe.setChanged();
                    }
                    level.setBlock(pos, state.setValue(LOCKED, belowLock), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.FAIL;
        }

        ItemStack held = player.getItemInHand(hand);
        BlockEntity be = level.getBlockEntity(pos);

        if (held.is(PlaceMatTags.Items.KEY)) {
            return handleKeyInteraction(level, pos, state, player, be);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    public static InteractionResult handleKeyInteraction(Level level, BlockPos pos, BlockState state, Player player, @Nullable BlockEntity be) {
        if (!state.hasProperty(LOCKED)) {
            return InteractionResult.PASS;
        }

        LockType currentLock = state.getValue(LOCKED);
        boolean isStrict = currentLock == LockType.STRICT;
        LockType newLock = isStrict ? LockType.NONE : LockType.STRICT;

        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        BlockEntity belowBE = level.getBlockEntity(belowPos);

        if (newLock.isLocked()) {
            if (be instanceof PlaceMatBlockEntity pmbe && pmbe.getPlacedItems().isEmpty()) {
                if (level instanceof ServerLevel serverLevel) {
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 2f, 0.5f);
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
                    level.removeBlock(pos, false);
                    if (belowState.getBlock() instanceof PlaceMatTallTableBlock && belowState.hasProperty(LOCKED)) {
                        if (belowBE instanceof PlaceMatBlockEntity belowPMBE) {
                            belowPMBE.setLockedBy(player.getUUID());
                            belowPMBE.setChanged();
                        }
                        level.setBlockAndUpdate(belowPos, belowState.setValue(LOCKED, newLock));
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        if (be instanceof PlaceMatBlockEntity pmbe) {
            if (!isStrict) {
                pmbe.setLockedBy(player.getUUID());
            } else if (player.getUUID().equals(pmbe.getLockedBy()) || pmbe.getLockedBy() == null) {
                pmbe.setLockedBy(null);
            }
            pmbe.setChanged();
        }

        if (belowState.getBlock() instanceof PlaceMatTallTableBlock && belowState.hasProperty(LOCKED)) {
            if (belowBE instanceof PlaceMatBlockEntity belowPMBE) {
                if (!isStrict) {
                    belowPMBE.setLockedBy(player.getUUID());
                } else if (player.getUUID().equals(belowPMBE.getLockedBy()) || belowPMBE.getLockedBy() == null) {
                    belowPMBE.setLockedBy(null);
                }
                belowPMBE.setChanged();
            }
            if (level instanceof ServerLevel) {
                level.setBlockAndUpdate(belowPos, belowState.setValue(LOCKED, newLock));
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 2f, 0.5f);
            serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
            level.setBlockAndUpdate(pos, state.setValue(LOCKED, newLock));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public boolean syncSetLockType(Level level, BlockPos pos, BlockState state, LockType lockType, @Nullable UUID owner) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        BlockEntity belowBE = level.getBlockEntity(belowPos);

        if (belowState.getBlock() instanceof PlaceMatTallTableBlock && belowState.hasProperty(LOCKED)) {
            if (belowBE instanceof PlaceMatBlockEntity belowPMBE) {
                belowPMBE.setLockedBy(lockType.isLocked() ? owner : null);
                belowPMBE.setChanged();
            }
            level.setBlockAndUpdate(belowPos, belowState.setValue(LOCKED, lockType));
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (lockType.isLocked()) {
            if (be instanceof PlaceMatBlockEntity pmbe && pmbe.getPlacedItems().isEmpty()) {
                level.removeBlock(pos, false);
                return true;
            }
        }

        if (be instanceof PlaceMatBlockEntity pmbe) {
            pmbe.setLockedBy(lockType.isLocked() ? owner : null);
            pmbe.setChanged();
        }
        return level.setBlockAndUpdate(pos, state.setValue(LOCKED, lockType));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult && level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            PlaceMatBlockEntity.PlacedItem targeted = pmbe.getTargetedItem(eyePos, lookVec, pos);
            if (targeted != null) {
                return targeted.stack.copy();
            }
        }
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof PlaceMatTallTableBlock) {
            return below.getBlock().getCloneItemStack(below, target, level, pos.below(), player);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }
}

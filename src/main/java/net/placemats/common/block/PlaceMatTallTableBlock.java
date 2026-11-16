package net.placemats.common.block;

import java.util.UUID;
import java.util.function.Supplier;

import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatBlocks;
import net.placemats.common.data.PlaceMatTags;

/**
 * Base class for two-block tall table bottom blocks.
 */
@Setter
@SuppressWarnings("deprecation")
public class PlaceMatTallTableBlock extends PlaceMatTableBlock {

    private Supplier<? extends Block> topBlockSupplier;

    public PlaceMatTallTableBlock(Properties properties, Supplier<? extends Block> topBlockSupplier) {
        super(properties);
        this.topBlockSupplier = topBlockSupplier;
    }

    public PlaceMatTallTableBlock(Properties properties) {
        this(properties, PlaceMatBlocks.TALL_TABLE_TOP);
    }

    public Block getTopBlock() {
        return topBlockSupplier.get();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return super.getStateForPlacement(context);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockPos abovePos = pos.above();
            if (abovePos.getY() < level.getMaxBuildHeight()) {
                BlockState aboveState = level.getBlockState(abovePos);
                if (aboveState.isAir() || aboveState.canBeReplaced()) {
                    level.setBlock(abovePos, getTopBlock().defaultBlockState().setValue(LOCKED, state.getValue(LOCKED)), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos abovePos = pos.above();
        if (level.getBlockState(abovePos).getBlock() instanceof InvisibleMatBlock) {
            level.destroyBlock(abovePos, !player.isCreative());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockPos abovePos = pos.above();
            if (level.getBlockState(abovePos).getBlock() instanceof InvisibleMatBlock) {
                level.destroyBlock(abovePos, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && !isLocked(state)) {
            BlockPos abovePos = pos.above();
            BlockState aboveState = level.getBlockState(abovePos);
            if (aboveState.isAir() || aboveState.canBeReplaced()) {
                level.setBlockAndUpdate(abovePos, getTopBlock().defaultBlockState().setValue(LOCKED, LockType.NONE));
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
        Supplier<? extends Block> supplier = state.getBlock() instanceof PlaceMatTallTableBlock tallTable ? tallTable::getTopBlock : PlaceMatBlocks.TALL_TABLE_TOP;
        return handleKeyInteraction(level, pos, state, player, be, supplier);
    }

    public static InteractionResult handleKeyInteraction(Level level, BlockPos pos, BlockState state, Player player, @Nullable BlockEntity be, Supplier<? extends Block> topBlockSupplier) {
        if (!state.hasProperty(LOCKED)) {
            return InteractionResult.PASS;
        }

        LockType currentLock = state.getValue(LOCKED);
        boolean isStrict = currentLock == LockType.STRICT;
        LockType newLock = isStrict ? LockType.NONE : LockType.STRICT;

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        BlockEntity aboveBE = level.getBlockEntity(abovePos);

        if (newLock.isLocked()) {
            if (aboveState.getBlock() instanceof InvisibleMatBlock) {
                if (aboveBE instanceof PlaceMatBlockEntity topPMBE && topPMBE.getPlacedItems().isEmpty()) {
                    if (level instanceof ServerLevel) {
                        level.removeBlock(abovePos, false);
                    }
                } else {
                    if (aboveBE instanceof PlaceMatBlockEntity topPMBE) {
                        topPMBE.setLockedBy(player.getUUID());
                        topPMBE.setChanged();
                    }
                    if (level instanceof ServerLevel) {
                        level.setBlockAndUpdate(abovePos, aboveState.setValue(LOCKED, newLock));
                    }
                }
            }
        } else {
            if (aboveState.getBlock() instanceof InvisibleMatBlock) {
                if (aboveBE instanceof PlaceMatBlockEntity topPMBE) {
                    if (player.getUUID().equals(topPMBE.getLockedBy()) || topPMBE.getLockedBy() == null) {
                        topPMBE.setLockedBy(null);
                    }
                    topPMBE.setChanged();
                }
                if (level instanceof ServerLevel) {
                    level.setBlockAndUpdate(abovePos, aboveState.setValue(LOCKED, LockType.NONE));
                }
            } else if (level.getBlockState(abovePos).isAir() || level.getBlockState(abovePos).canBeReplaced()) {
                if (level instanceof ServerLevel) {
                    Block topBlock = topBlockSupplier.get();
                    level.setBlockAndUpdate(abovePos, topBlock.defaultBlockState().setValue(LOCKED, LockType.NONE));
                }
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

        if (level instanceof ServerLevel serverLevel) {
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 2f, 0.5f);
            serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
            level.setBlockAndUpdate(pos, state.setValue(LOCKED, newLock));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public boolean syncSetLockType(Level level, BlockPos pos, BlockState state, LockType lockType, @Nullable UUID owner) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        BlockEntity aboveBE = level.getBlockEntity(abovePos);

        if (lockType.isLocked()) {
            if (aboveState.getBlock() instanceof InvisibleMatBlock) {
                if (aboveBE instanceof PlaceMatBlockEntity topPMBE && topPMBE.getPlacedItems().isEmpty()) {
                    level.removeBlock(abovePos, false);
                } else {
                    if (aboveBE instanceof PlaceMatBlockEntity topPMBE) {
                        topPMBE.setLockedBy(owner);
                        topPMBE.setChanged();
                    }
                    level.setBlockAndUpdate(abovePos, aboveState.setValue(LOCKED, lockType));
                }
            }
        } else {
            if (aboveState.getBlock() instanceof InvisibleMatBlock) {
                if (aboveBE instanceof PlaceMatBlockEntity topPMBE) {
                    topPMBE.setLockedBy(null);
                    topPMBE.setChanged();
                }
                level.setBlockAndUpdate(abovePos, aboveState.setValue(LOCKED, LockType.NONE));
            } else if (level.getBlockState(abovePos).isAir() || level.getBlockState(abovePos).canBeReplaced()) {
                level.setBlockAndUpdate(abovePos, getTopBlock().defaultBlockState().setValue(LOCKED, LockType.NONE));
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlaceMatBlockEntity pmbe) {
            pmbe.setLockedBy(owner);
            pmbe.setChanged();
        }
        return level.setBlockAndUpdate(pos, state.setValue(LOCKED, lockType));
    }
}

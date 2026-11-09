package net.placemats.common.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.event.PlaceMatInteractions;

/**
 * Base class for place mats, which are blocks that can hold items.
 * Handles game-related interactions while placement range and collision/shape handling is done through interfaces.
 */
@SuppressWarnings({"deprecation", "UnusedReturnValue", "unused"})
public class PlaceMatBlock extends Block implements EntityBlock, IPlacementRangeProvider, IPlaceMatShapeProvider, IDynamicTexturedBlock {

    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;

    @Getter
    private final PlaceMatPlacementDefinition placementDefinition = new PlaceMatPlacementDefinition();
    @Getter
    private final PlaceMatShapeDefinition shapeDefinition = new PlaceMatShapeDefinition();

    @Getter
    private boolean dynamic = false;
    private int textureKeyCount = 0;
    private List<String> textureKeys = new ArrayList<>();
    private Map<String, ResourceLocation> defaultTextures = new HashMap<>();
    private Map<Integer, Component> textureKeyLabels = new HashMap<>();

    public PlaceMatBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(LOCKED, false));
    }

    public PlaceMatBlock dynamicShelf(int keyCount, Map<String, ResourceLocation> defaults, Map<Integer, Component> labels) {
        this.dynamic = true;
        this.textureKeyCount = keyCount;
        this.defaultTextures = new HashMap<>(defaults);
        this.textureKeys = new ArrayList<>(defaults.keySet());
        this.textureKeyLabels = new HashMap<>(labels);
        return this;
    }

    @Override
    public int getTextureKeyCount() {
        return textureKeyCount;
    }

    @Override
    public List<String> getTextureKeys() {
        return Collections.unmodifiableList(textureKeys);
    }

    @Override
    public ResourceLocation getDefaultTexture(String key) {
        return defaultTextures.get(key);
    }

    @Override
    public Component getTextureKeyName(int keyIndex) {
        if (textureKeyLabels.containsKey(keyIndex)) {
            return textureKeyLabels.get(keyIndex);
        }
        return Component.translatable("gui.place_mats.cabinet_maker.key.generic", keyIndex + 1);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Map<String, ResourceLocation> textures = IDynamicTexturedBlock.getDynamicTextures(stack);
            if (!textures.isEmpty()) {
                pmbe.setDynamicTextures(textures);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    /* ======================== Game Interactions ========================== */

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
            } else if (getPlacementRanges(state).isEmpty()) {
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
        if (target instanceof BlockHitResult && level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            PlaceMatBlockEntity.PlacedItem targeted = pmbe.getTargetedItem(eyePos, lookVec, pos);
            if (targeted != null) {
                return targeted.stack.copy();
            }
            ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
            if (pmbe.hasDynamicTextures()) {
                IDynamicTexturedBlock.setDynamicTextures(stack, pmbe.getDynamicTextures());
            }
            return stack;
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PlaceMatBlockEntity pmbe && pmbe.hasDynamicTextures()) {
            for (ItemStack drop : drops) {
                if (drop.getItem() == this.asItem()) {
                    IDynamicTexturedBlock.setDynamicTextures(drop, pmbe.getDynamicTextures());
                }
            }
        }
        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlaceMatBlockEntity pmbe) {
                pmbe.dropItems();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        } else if (!state.equals(newState)) {
            boolean onlyLockChanged = state.hasProperty(LOCKED) && newState.hasProperty(LOCKED)
                    && state.setValue(LOCKED, false).equals(newState.setValue(LOCKED, false));
            if (!onlyLockChanged) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PlaceMatBlockEntity pmbe) {
                    pmbe.dropItems();
                }
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
        return getShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state);
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

    /* ======================== Static Utility Helpers ========================== */

    public static VoxelShape box(float x1, float y1, float z1, float x2, float y2, float z2) {
        return PlaceMatShapeHelper.box(x1, y1, z1, x2, y2, z2);
    }

    public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return PlaceMatShapeHelper.box(x1, y1, z1, x2, y2, z2);
    }

    public static VoxelShape combineShapes(VoxelShape... shapes) {
        return PlaceMatShapeHelper.combineShapes(shapes);
    }

    public static VoxelShape combineBoxes(AABB... boxes) {
        return PlaceMatShapeHelper.combineBoxes(boxes);
    }

    public static AABB rotateAABB(Direction direction, AABB box) {
        return PlaceMatShapeHelper.rotateAABB(direction, box);
    }

    public static VoxelShape rotateShape(Direction direction, VoxelShape shape) {
        return PlaceMatShapeHelper.rotateShape(direction, shape);
    }

    public static VoxelShape rotateShape(Direction direction, VoxelShape... shapes) {
        return PlaceMatShapeHelper.rotateShape(direction, shapes);
    }

    public static VoxelShape rotateShape(Direction direction, AABB... boxes) {
        return PlaceMatShapeHelper.rotateShape(direction, boxes);
    }

    public static Map<Direction, VoxelShape> createCardinalRotations(VoxelShape shape) {
        return PlaceMatShapeHelper.createCardinalRotations(shape);
    }

    public static Map<Direction, VoxelShape> createCardinalRotations(VoxelShape... shapes) {
        return PlaceMatShapeHelper.createCardinalRotations(shapes);
    }

    public static Map<Direction, VoxelShape> createCardinalRotations(AABB... boxes) {
        return PlaceMatShapeHelper.createCardinalRotations(boxes);
    }

    public static Vec3 getLocalHitVec(BlockState state, Vec3 relativeHitVec) {
        return PlaceMatShapeHelper.getLocalHitVec(state, relativeHitVec);
    }

    public static Vec3 getWorldHitVec(BlockState state, Vec3 localHitVec) {
        return PlaceMatShapeHelper.getWorldHitVec(state, localHitVec);
    }

    public static Vec3 rotateDirection(Direction facing, Vec3 dir) {
        return PlaceMatShapeHelper.rotateDirection(facing, dir);
    }

    public static Vec3 rotateDirectionInverse(Direction facing, Vec3 dir) {
        return PlaceMatShapeHelper.rotateDirectionInverse(facing, dir);
    }

    public static void addPlacementRangesStatic(BlockState state, Consumer<PlacementRange> consumer) {
        IPlacementRangeProvider.addPlacementRangesStatic(state, consumer);
    }
}

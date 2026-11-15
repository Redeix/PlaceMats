package net.placemats.common.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.placemats.config.ConfigHelpers;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;

import net.placemats.common.blockentity.IDynamicTexturedBlockEntity;
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

    public static final EnumProperty<LockType> LOCKED = EnumProperty.create("locked", LockType.class);

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
    private float previewYOffset = 0.0f;
    private boolean previewItemModel = false;

    public PlaceMatBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(LOCKED, LockType.NONE));
    }

    public PlaceMatBlock dynamicBlock(int keyCount, Map<String, ResourceLocation> defaults, Map<Integer, Component> labels) {
        return dynamicBlock(keyCount, defaults, labels, 0.0f);
    }

    public PlaceMatBlock dynamicBlock(int keyCount, Map<String, ResourceLocation> defaults, Map<Integer, Component> labels, float previewYOffset) {
        this.dynamic = true;
        this.textureKeyCount = keyCount;
        this.defaultTextures = new HashMap<>(defaults);
        List<String> keys = new ArrayList<>(defaults.keySet());
        keys.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (NumberFormatException e) {
                return a.compareTo(b);
            }
        });
        this.textureKeys = keys;
        this.textureKeyLabels = new HashMap<>(labels);
        this.previewYOffset = previewYOffset;
        return this;
    }

    public PlaceMatBlock previewYOffset(float offset) {
        this.previewYOffset = offset;
        return this;
    }

    public PlaceMatBlock previewItemModel(boolean previewItemModel) {
        this.previewItemModel = previewItemModel;
        return this;
    }

    public PlaceMatBlock previewItemModel() {
        return previewItemModel(true);
    }

    @Override
    public float getPreviewYOffset() {
        return this.previewYOffset;
    }

    @Override
    public boolean shouldPreviewItemModel() {
        return this.previewItemModel;
    }

    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side, @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        if (level.getBlockEntity(pos) instanceof IDynamicTexturedBlockEntity dtbe) {
            Map<String, BlockState> mats = dtbe.getDynamicMaterials();
            if (queryState != null && mats.containsValue(queryState)) {
                return queryState;
            }
            BlockState mat = dtbe.getDynamicMaterial();
            if (mat != null) {
                return mat;
            }
            if (queryState != null && dtbe.hasDynamicTextures()) {
                ResourceLocation queryBlockId = BuiltInRegistries.BLOCK.getKey(queryState.getBlock());
                for (ResourceLocation tex : dtbe.getDynamicTextures().values()) {
                    if (tex != null && tex.getNamespace().equals(queryBlockId.getNamespace())) {
                        String path = tex.getPath();
                        if (path.endsWith(queryBlockId.getPath()) || queryBlockId.getPath().endsWith(path.substring(path.lastIndexOf('/') + 1))) {
                            return queryState;
                        }
                    }
                }
            }
        }
        return super.getAppearance(state, level, pos, side, queryState, queryPos);
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
        onDynamicBlockPlacedBy(level, pos, stack);
        if (level.getBlockEntity(pos) instanceof IDynamicTexturedBlockEntity) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    /* ======================== Game Interactions ========================== */

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof PlaceMatBlockEntity pmbe && !preventsInteraction(pmbe.getBlockState())) {
            InteractionResult result = PlaceMatInteractions.handleInteraction(pmbe, player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST) && !preventsInteraction(state)) {
            Vec3 location = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            PlacementRange targetedRange = getTargetedPlacementRange(state, location);
            if (targetedRange != null) {
                if (!targetedRange.chair() && (targetedRange.whitelistTag() == null || held.is(targetedRange.whitelistTag()))) {
                    return InteractionResult.SUCCESS;
                }
            } else if (getPlacementRanges(state).isEmpty()) {
                return InteractionResult.SUCCESS;
            }
        }

        if (held.is(PlaceMatTags.Items.KEY)) {
            return handleKeyInteraction(level, pos, state, player, be);
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
            return IDynamicTexturedBlock.applyDynamicDataToItemStack(stack, pmbe);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        applyDynamicDrops(drops, params, this);
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
                    && state.setValue(LOCKED, LockType.NONE).equals(newState.setValue(LOCKED, LockType.NONE));
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
            if (pmbe.getTargetedItem(eyePos, lookVec, pos) != null || preventsDestruction(pmbe.getBlockState())) {
                return 0.0f;
            }
        } else if (preventsDestruction(state)) {
            return 0.0f;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        if (preventsExplosion(state)) {
            return 3600000.0F;
        }
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (preventsBurning(state)) {
            return false;
        }
        return super.isFlammable(state, level, pos, direction);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (preventsBurning(state)) {
            return 0;
        }
        return super.getFlammability(state, level, pos, direction);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (preventsBurning(state)) {
            return 0;
        }
        return super.getFireSpreadSpeed(state, level, pos, direction);
    }

    /* ======================== Locking API ========================== */

    public static LockType getLockType(@Nullable BlockState state) {
        if (state != null && state.hasProperty(LOCKED)) {
            return state.getValue(LOCKED);
        }
        return LockType.NONE;
    }

    public static boolean isLocked(@Nullable BlockState state) {
        return getLockType(state).isLocked();
    }

    public static boolean preventsInteraction(@Nullable BlockState state) {
        return getLockType(state).preventsInteraction;
    }

    public static boolean preventsDestruction(@Nullable BlockState state) {
        return getLockType(state).preventsDestruction;
    }

    public static boolean preventsExplosion(@Nullable BlockState state) {
        return getLockType(state).preventsExplosion;
    }

    public static boolean preventsBurning(@Nullable BlockState state) {
        return getLockType(state).preventsBurning;
    }

    public static boolean preventsOutlines(@Nullable BlockState state) {
        return getLockType(state).preventsOutlines;
    }

    public static boolean setLockType(Level level, BlockPos pos, BlockState state, LockType lockType, @Nullable UUID owner) {
        if (!state.hasProperty(LOCKED)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlaceMatBlockEntity pmbe) {
            pmbe.setLockedBy(owner);
            pmbe.setChanged();
        }
        return level.setBlockAndUpdate(pos, state.setValue(LOCKED, lockType));
    }

    public static InteractionResult handleKeyInteraction(Level level, BlockPos pos, BlockState state, Player player, @Nullable BlockEntity be) {
        if (!state.hasProperty(LOCKED)) {
            return InteractionResult.PASS;
        }

        LockType currentLock = state.getValue(LOCKED);
        boolean isStrict = currentLock == LockType.STRICT;
        LockType newLock = isStrict ? LockType.NONE : LockType.STRICT;

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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!level.isClientSide && entity instanceof LivingEntity living && !(entity instanceof Player)) {
            trySitMob(level, pos, state, living);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide && entity instanceof LivingEntity living && !(entity instanceof Player)) {
            trySitMob(level, pos, state, living);
        }
    }

    public void trySitMob(Level level, BlockPos pos, BlockState state, LivingEntity mob) {
        if (mob.isPassenger() || mob.isVehicle() || !mob.isAlive()) {
            return;
        }
        if (ConfigHelpers.isMobChairBlacklisted(mob.getType())) {
            return;
        }
        if (mob.getPersistentData().contains(PlaceMatInteractions.CHAIR_COOLDOWN_TAG)) {
            long cooldownUntil = mob.getPersistentData().getLong(PlaceMatInteractions.CHAIR_COOLDOWN_TAG);
            if (level.getGameTime() < cooldownUntil) {
                return;
            } else {
                mob.getPersistentData().remove(PlaceMatInteractions.CHAIR_COOLDOWN_TAG);
            }
        }
        for (PlacementRange range : getPlacementRanges(state)) {
            if (range.chair()) {
                AABB worldRangeBox = getWorldRangeBox(state, pos, range).inflate(0.05, 0.1, 0.05);
                if (mob.getBoundingBox().intersects(worldRangeBox)) {
                    InteractionResult result = PlaceMatInteractions.sitEntity(level, pos, state, range, mob);
                    if (result.consumesAction()) {
                        break;
                    }
                }
            }
        }
    }

    public static @Nullable Entity getEntityInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.getEntityInRange(level, pos, state, range);
    }

    public static @Nullable UUID getEntityUUIDInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.getEntityUUIDInRange(level, pos, state, range);
    }

    public static @Nullable EntityType<?> getEntityTypeInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.getEntityTypeInRange(level, pos, state, range);
    }

    public static boolean hasEntityInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.hasEntityInRange(level, pos, state, range);
    }

    public static @Nullable Mob getMobInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.getMobInRange(level, pos, state, range);
    }

    public static @Nullable java.util.UUID getMobUUIDInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.getMobUUIDInRange(level, pos, state, range);
    }

    public static @Nullable EntityType<?> getMobTypeInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.getMobTypeInRange(level, pos, state, range);
    }

    public static boolean hasMobInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return PlaceMatInteractions.hasMobInRange(level, pos, state, range);
    }

    public static AABB getWorldRangeBox(BlockState state, BlockPos pos, PlacementRange range) {
        AABB localBox = range.box();
        if (state.hasProperty(PlaceMatCardinalBlock.FACING)) {
            if (!(state.getBlock() instanceof IPlacementRangeProvider provider) || provider.isRotateZones()) {
                localBox = PlaceMatShapeHelper.rotateAABB(state.getValue(PlaceMatCardinalBlock.FACING), localBox);
            }
        }
        return localBox.move(pos);
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

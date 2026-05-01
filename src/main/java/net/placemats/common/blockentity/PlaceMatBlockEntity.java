package net.placemats.common.blockentity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.eerussianguy.firmalife.config.FLConfig;

import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import lombok.Getter;
import lombok.Setter;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.resource.DefinitionManager;

@SuppressWarnings("unused")
@Getter
public class PlaceMatBlockEntity extends BlockEntity implements ClimateReceiver {
    private static final FoodTrait[] POSSIBLE_SHELVED_TRAITS = { FLFoodTraits.SHELVED, FLFoodTraits.SHELVED_2, FLFoodTraits.SHELVED_3 };

    private final List<PlacedItem> placedItems = new ArrayList<>();
    @Setter
    private float currentRotation = 0;
    @Setter
    private float currentPitch = 0;
    @Setter
    private float currentRoll = 0;
    @Setter
    private float currentHeight = 0;
    private long lastPlacementTick = -1;
    private long lastInteractionTick = -1;
    private boolean climateValid = false;

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() {
            if (level == null)
                return 0;
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof PlaceMatBlock pmb) {
                return pmb.getContainerSize();
            }
            return 12;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < placedItems.size()) {
                return placedItems.get(slot).stack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (level == null || stack.isEmpty())
                return stack;
            if (!(getBlockState().getBlock() instanceof PlaceMatBlock pmb))
                return stack;

            if (slot >= 0 && slot < placedItems.size()) {
                PlacedItem target = placedItems.get(slot);
                // Try to stack into existing item.
                if (ItemHandlerHelper.canItemStacksStack(target.stack, stack)) {
                    // Find the range this item belongs to.
                    PlaceMatBlock.PlacementRange range = null;
                    for (PlaceMatBlock.PlacementRange r : pmb.getPlacementRanges()) {
                        if (Math.abs(target.baseHeight - r.box().minY) < 0.001f) {
                            float multiplier = getScaleMultiplier(r);
                            DefinitionManager.PlaceMatDefinition targetDef = getEffectiveDefinition(target.stack, r);
                            if (target.pos.x >= r.box().minX && target.pos.x <= r.box().maxX &&
                                    target.pos.y >= r.box().minZ && target.pos.y <= r.box().maxZ) {
                                range = r;
                                break;
                            }
                        }
                    }
                    if (range != null && isInsertable(range, stack)) {
                        int maxStack = Math.min(stack.getMaxStackSize(), Math.min(pmb.getMaxStackSize(), range.maxStackSize()));
                        int space = maxStack - target.stack.getCount();
                        int toAdd = Math.min(space, stack.getCount());
                        if (toAdd > 0) {
                            if (!simulate) {
                                target.stack.grow(toAdd);
                                updateTargetHeights();
                                applyTraits(target.stack, range);
                                setChanged();
                                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                            }
                            ItemStack remainder = stack.copy();
                            remainder.shrink(toAdd);
                            return remainder;
                        }
                    }
                }
                // You can't park there mate.
                return stack;
            } else if (slot >= placedItems.size() && slot < getSlots()) {
                // Try to find a random position for a new item.
                DefinitionManager.PlaceMatDefinition def = getEffectiveDefinition(stack, null);
                for (PlaceMatBlock.PlacementRange range : pmb.getPlacementRanges()) {
                    if (!isInsertable(range, stack))
                        continue;

                    float multiplier = getScaleMultiplier(range);
                    DefinitionManager.PlaceMatDefinition rangeDef = getEffectiveDefinition(stack, range);

                    // Try random positions.
                    for (int i = 0; i < 20; i++) {
                        float minX = (float) range.box().minX;
                        float maxX = (float) range.box().maxX - rangeDef.size().x * multiplier;
                        float minZ = (float) range.box().minZ;
                        float maxZ = (float) range.box().maxZ - rangeDef.size().y * multiplier;

                        if (maxX < minX || maxZ < minZ)
                            continue;

                        float rx = minX + level.random.nextFloat() * (maxX - minX);
                        float rz = minZ + level.random.nextFloat() * (maxZ - minZ);
                        Vec2 pos = new Vec2(rx, rz);

                        // We check if it can be placed at base level of the range.
                        if (canPlace(stack, pos, 0, range)) {
                            int maxStack = Math.min(stack.getMaxStackSize(), Math.min(pmb.getMaxStackSize(), range.maxStackSize()));
                            int toPlaceCount = Math.min(stack.getCount(), maxStack);
                            if (!simulate) {
                                ItemStack toPlace = stack.copy();
                                toPlace.setCount(toPlaceCount);
                                placeItem(toPlace, pos, 0, 0, 0, 0, range);
                            }
                            ItemStack remainder = stack.copy();
                            remainder.shrink(toPlaceCount);
                            return remainder;
                        }
                    }
                }
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (level == null || amount <= 0)
                return ItemStack.EMPTY;
            if (slot >= 0 && slot < placedItems.size()) {
                PlacedItem item = placedItems.get(slot);
                if (!isExtractable(item))
                    return ItemStack.EMPTY;

                ItemStack stack = item.stack;
                if (stack.isEmpty())
                    return ItemStack.EMPTY;

                int extractAmount = Math.min(amount, stack.getCount());
                ItemStack result = stack.copy();
                result.setCount(extractAmount);

                if (!simulate) {
                    if (extractAmount >= stack.getCount()) {
                        placedItems.remove(slot);
                    } else {
                        stack.shrink(extractAmount);
                    }
                    updateTargetHeights();
                    removeTraits(result, getRangeForItem(item));
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                } else {
                    removeTraits(result, getRangeForItem(item));
                }
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                return pmb.getMaxStackSize();
            }
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    });

    public PlaceMatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private boolean shouldStackOn(float myManualHeight, int myIndex, float otherManualHeight, int otherIndex) {
        if (myManualHeight > otherManualHeight + 0.001f)
            return true;
        if (otherManualHeight > myManualHeight + 0.001f)
            return false;
        return myIndex > otherIndex;
    }

    public void updateTargetHeights() {
        int n = placedItems.size();
        if (n == 0)
            return;

        List<Integer> sortedIndices = new ArrayList<>();
        for (int i = 0; i < n; i++)
            sortedIndices.add(i);

        sortedIndices.sort((i, j) -> {
            float mi = placedItems.get(i).manualHeight;
            float mj = placedItems.get(j).manualHeight;
            if (Math.abs(mi - mj) > 0.001f)
                return Float.compare(mi, mj);
            return Integer.compare(i, j);
        });

        float[] results = new float[n];
        for (int idx : sortedIndices) {
            PlacedItem item = placedItems.get(idx);
            PlaceMatBlock.PlacementRange range = getRangeForItem(item);
            DefinitionManager.PlaceMatDefinition def = getEffectiveDefinition(item.stack, range);
            float multiplier = getScaleMultiplier(range);

            float currentMax = item.manualHeight;
            if (range != null && range.restricted() && range.snapToCenter()) {
                currentMax = item.manualHeight; // Manual height already has snapToCenter offset
            }

            boolean stackingEnabled = (range == null || range.stackingEnabled()) && (range == null || !range.restricted());
            boolean collisionDisabled = range != null && range.collisionDisabled();

            if (stackingEnabled && !collisionDisabled && def.stackable()) {
                for (int otherIdx : sortedIndices) {
                    if (otherIdx == idx)
                        break;

                    PlacedItem other = placedItems.get(otherIdx);
                    if (Math.abs(other.baseHeight - item.baseHeight) > 0.001f)
                        continue;

                    PlaceMatBlock.PlacementRange otherRange = getRangeForItem(other);
                    DefinitionManager.PlaceMatDefinition otherDef = getEffectiveDefinition(other.stack, otherRange);
                    float otherMultiplier = getScaleMultiplier(otherRange);
                    if (!otherDef.allowsStackingOnTop())
                        continue;

                    if (intersects2D(item.pos, new Vec2(def.size().x * multiplier, def.size().y * multiplier),
                            other.pos, new Vec2(otherDef.size().x * otherMultiplier, otherDef.size().y * otherMultiplier))) {
                        float otherTop = results[otherIdx] + otherDef.getItemHeight() * otherMultiplier + 0.005f;
                        if (otherTop > currentMax) {
                            currentMax = otherTop;
                        }
                    }
                }
            }
            results[idx] = currentMax;
        }

        for (int i = 0; i < n; i++) {
            placedItems.get(i).targetHeight = results[i];
        }
    }

    public float calculateEffectiveHeight(ItemStack stack, Vec2 pos, float manualHeight, @Nullable PlaceMatBlock.PlacementRange targetRange) {
        DefinitionManager.PlaceMatDefinition def = getEffectiveDefinition(stack, targetRange);
        float multiplier = getScaleMultiplier(targetRange);

        if (targetRange != null && targetRange.restricted()) {
            if (targetRange.snapToCenter()) {
                float centerY = (float) (targetRange.box().minY + targetRange.box().maxY) / 2f;
                float itemHalfHeight = (def.getItemHeight() * multiplier) / 2f;
                return centerY - (float) targetRange.box().minY - itemHalfHeight;
            }
            return 0;
        }

        float effectiveHeight = manualHeight;

        boolean stackingEnabled = targetRange == null || targetRange.stackingEnabled();
        boolean collisionDisabled = targetRange != null && targetRange.collisionDisabled();
        if (stackingEnabled && !collisionDisabled && def.stackable()) {
            AABB targetBox = targetRange != null ? targetRange.box() : null;
            float baseHeight = targetBox != null ? (float) targetBox.minY : 0.0625f;

            boolean changed;
            int safety = 0;
            do {
                changed = false;
                for (int i = 0; i < placedItems.size(); i++) {
                    PlacedItem placed = placedItems.get(i);
                    if (Math.abs(placed.baseHeight - baseHeight) > 0.001f)
                        continue;
                    if (!shouldStackOn(manualHeight, placedItems.size(), placed.manualHeight, i))
                        continue;

                    PlaceMatBlock.PlacementRange otherRange = getRangeForItem(placed);
                    DefinitionManager.PlaceMatDefinition placedDef = getEffectiveDefinition(placed.stack, otherRange);
                    float placedMultiplier = getScaleMultiplier(otherRange);
                    if (!placedDef.allowsStackingOnTop())
                        continue;

                    if (intersects2D(pos, new Vec2(def.size().x * multiplier, def.size().y * multiplier),
                            placed.pos, new Vec2(placedDef.size().x * placedMultiplier, placedDef.size().y * placedMultiplier))) {
                        float newHeight = (placed.targetHeight + placedDef.getItemHeight() * placedMultiplier) + 0.005f;
                        if (newHeight > effectiveHeight) {
                            effectiveHeight = newHeight;
                            changed = true;
                        }
                    }
                }
                safety++;
            } while (changed && safety < 10);
        }

        return effectiveHeight;
    }

    private boolean intersects2D(Vec2 pos1, Vec2 size1, Vec2 pos2, Vec2 size2) {
        return pos1.x < pos2.x + size2.x && pos1.x + size1.x > pos2.x &&
                pos1.y < pos2.y + size2.y && pos1.y + size1.y > pos2.y;
    }

    public boolean canPlace(ItemStack stack, Vec2 pos, float height, @Nullable PlaceMatBlock.PlacementRange targetRange) {
        if (stack.isEmpty() || stack.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
            return false;
        }

        PlaceMatBlock pmb = getBlockState().getBlock() instanceof PlaceMatBlock ? (PlaceMatBlock) getBlockState().getBlock() : null;
        if (targetRange != null) {
            TagKey<Item> whitelist = targetRange.whitelistTag();
            if (whitelist != null && !stack.is(whitelist)) {
                return false;
            }

            if (targetRange.restricted()) {
                AABB targetBox = targetRange.box();
                for (PlacedItem placed : placedItems) {
                    if (Math.abs(placed.baseHeight - targetBox.minY) < 0.001f) {
                        if (placed.pos.x >= targetBox.minX && placed.pos.x <= targetBox.maxX &&
                                placed.pos.y >= targetBox.minZ && placed.pos.y <= targetBox.maxZ) {
                            if (ItemHandlerHelper.canItemStacksStack(placed.stack, stack)) {
                                int maxStackSize = Math.min(stack.getMaxStackSize(), Math.min(pmb != null ? pmb.getMaxStackSize() : 64, targetRange.maxStackSize()));
                                return placed.stack.getCount() < maxStackSize;
                            }
                            return false;
                        }
                    }
                }
            }
        }
        if (pmb != null) {
            if (placedItems.size() >= pmb.getContainerSize()) {
                return false;
            }
        } else {
            if (placedItems.size() >= 10) {
                return false;
            }
        }

        DefinitionManager.PlaceMatDefinition def = getEffectiveDefinition(stack, targetRange);
        float multiplier = getScaleMultiplier(targetRange);

        // Placement test.
        float effectiveHeight = calculateEffectiveHeight(stack, pos, height, targetRange);

        if (effectiveHeight < -0.001f) {
            return false;
        }
        float absoluteTop = (targetRange != null ? (float) targetRange.box().minY : 0.0625f) + effectiveHeight + def.getItemHeight() * multiplier;
        float limit = targetRange != null ? targetRange.maxHeight() : 1.0f;
        if (absoluteTop > limit + 0.001f) {
            return false;
        }

        Vec2 simulatedPos = pos;
        float simulatedHeight = height;
        if (targetRange != null && targetRange.restricted()) {
            AABB targetBox = targetRange.box();
            float centerX = (float) (targetBox.minX + targetBox.maxX) / 2f;
            float centerZ = (float) (targetBox.minZ + targetBox.maxZ) / 2f;
            simulatedPos = new Vec2(centerX - def.size().x * multiplier / 2f, centerZ - def.size().y * multiplier / 2f);
            if (targetRange.snapToCenter()) {
                float centerY = (float) (targetBox.minY + targetBox.maxY) / 2f;
                float itemHalfHeight = (def.getItemHeight() * multiplier) / 2f;
                simulatedHeight = centerY - (float) targetBox.minY - itemHalfHeight;
            } else {
                simulatedHeight = 0;
            }
        }

        // Simulate addition to check if it pushes other items over their limits or causes 3D collisions.
        PlacedItem testItem = new PlacedItem(this, stack, simulatedPos, 0, 0, 0, simulatedHeight, targetRange != null ? (float) targetRange.box().minY : 0.0625f);
        placedItems.add(testItem);
        updateTargetHeights();

        boolean ok = true;
        for (PlacedItem placed : placedItems) {
            PlaceMatBlock.PlacementRange range = getRangeForItem(placed);
            float pLimit = range != null ? range.maxHeight() : 1.0f;
            DefinitionManager.PlaceMatDefinition pDef = getEffectiveDefinition(placed.stack, range);
            float pMultiplier = getScaleMultiplier(range);
            if (placed.baseHeight + placed.targetHeight + pDef.getItemHeight() * pMultiplier > pLimit + 0.001f) {
                ok = false;
                break;
            }

            boolean pCollisionDisabled = range != null && range.collisionDisabled();
            if (!pCollisionDisabled) {
                for (PlacedItem other : placedItems) {
                    if (placed == other)
                        continue;
                    PlaceMatBlock.PlacementRange otherRange = getRangeForItem(other);
                    if (otherRange != null && otherRange.collisionDisabled())
                        continue;

                    if (placed.getLogicalBox().intersects(other.getLogicalBox())) {
                        ok = false;
                        break;
                    }
                }
            }
            if (!ok)
                break;
        }

        placedItems.remove(placedItems.size() - 1);
        updateTargetHeights();

        return ok;
    }

    public void placeItem(ItemStack stack, Vec2 pos, float rotation, float pitch, float roll, float height, @Nullable PlaceMatBlock.PlacementRange targetRange) {
        if (canPlace(stack, pos, height, targetRange)) {
            float finalRotation = rotation;
            float finalPitch = pitch;
            float finalRoll = roll;

            if (targetRange != null) {
                if (targetRange.yawDisabled())
                    finalRotation = 0;
                if (targetRange.pitchDisabled())
                    finalPitch = 0;
                if (targetRange.rollDisabled())
                    finalRoll = 0;

                if (targetRange.elevationDisabled() && (!(targetRange.stackingEnabled()) || targetRange.collisionDisabled()))
                    height = 0;
            }

            DefinitionManager.PlaceMatDefinition def = DefinitionManager.getDefinition(stack.getItem());
            // Clamp position within target box if provided.
            Vec2 effectivePos = pos;
            float effectiveHeight = height;
            AABB targetBox = targetRange != null ? targetRange.box() : null;
            if (targetRange != null) {
                if (targetRange.restricted()) {
                    float centerX = (float) (targetBox.minX + targetBox.maxX) / 2f;
                    float centerZ = (float) (targetBox.minZ + targetBox.maxZ) / 2f;
                    float multiplier = getScaleMultiplier(targetRange);
                    DefinitionManager.PlaceMatDefinition defEffective = getEffectiveDefinition(stack, targetRange);

                    effectivePos = new Vec2(centerX - defEffective.size().x * multiplier / 2f, centerZ - defEffective.size().y * multiplier / 2f);
                    if (targetRange.snapToCenter()) {
                        float centerY = (float) (targetBox.minY + targetBox.maxY) / 2f;
                        float itemHalfHeight = (defEffective.getItemHeight() * multiplier) / 2f;
                        effectiveHeight = centerY - (float) targetBox.minY - itemHalfHeight;
                    } else {
                        effectiveHeight = 0;
                    }
                } else {
                    float minX = (float) targetBox.minX;
                    float multiplier = getScaleMultiplier(targetRange);
                    DefinitionManager.PlaceMatDefinition defEffective = getEffectiveDefinition(stack, targetRange);
                    float maxX = (float) targetBox.maxX - defEffective.size().x * multiplier;
                    float minZ = (float) targetBox.minZ;
                    float maxZ = (float) targetBox.maxZ - defEffective.size().y * multiplier;

                    float clampedX = Math.max(minX, Math.min(maxX, pos.x));
                    float clampedZ = Math.max(minZ, Math.min(maxZ, pos.y));
                    effectivePos = new Vec2(clampedX, clampedZ);
                    effectiveHeight = calculateEffectiveHeight(stack, effectivePos, height, targetRange);
                }
            } else {
                effectiveHeight = calculateEffectiveHeight(stack, effectivePos, height, null);
            }

            if (targetRange != null && targetRange.restricted()) {
                // Try to merge with existing item in this range.
                for (PlacedItem placed : placedItems) {
                    if (Math.abs(placed.baseHeight - targetBox.minY) < 0.001f) {
                        if (placed.pos.x >= targetBox.minX && placed.pos.x <= targetBox.maxX &&
                                placed.pos.y >= targetBox.minZ && placed.pos.y <= targetBox.maxZ) {
                            if (ItemHandlerHelper.canItemStacksStack(placed.stack, stack)) {
                                PlaceMatBlock pmb2 = getBlockState().getBlock() instanceof PlaceMatBlock ? (PlaceMatBlock) getBlockState().getBlock() : null;
                                int maxStack = Math.min(stack.getMaxStackSize(), Math.min(pmb2 != null ? pmb2.getMaxStackSize() : 64, targetRange.maxStackSize()));
                                int space = maxStack - placed.stack.getCount();
                                int toAdd = Math.min(space, stack.getCount());
                                if (toAdd > 0) {
                                    placed.stack.grow(toAdd);
                                    applyTraits(placed.stack, targetRange);
                                    updateTargetHeights();
                                    lastPlacementTick = level != null ? level.getGameTime() : -1;
                                    setChanged();
                                    if (level != null) {
                                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                                        level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_TURTLE, SoundSource.BLOCKS, 0.2f, 2.0f);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            ItemStack toPlace = stack.copy();
            applyTraits(toPlace, targetRange);
            float baseHeight = targetBox != null ? (float) targetBox.minY : 0.0625f;
            placedItems.add(new PlacedItem(this, toPlace, effectivePos, finalRotation, finalPitch, finalRoll, effectiveHeight, baseHeight));
            updateTargetHeights();
            lastPlacementTick = level != null ? level.getGameTime() : -1;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.playSound(null, worldPosition, SoundEvents.ARMOR_EQUIP_TURTLE, SoundSource.BLOCKS, 0.2f, 2.0f);
            }
        }
    }

    public boolean isExtractable(PlacedItem item) {
        if (level == null)
            return false;
        if (!(getBlockState().getBlock() instanceof PlaceMatBlock pmb))
            return false;
        if (pmb.isExtractionDisabled())
            return false;

        for (PlaceMatBlock.PlacementRange range : pmb.getPlacementRanges()) {
            if (Math.abs(item.baseHeight - range.box().minY) < 0.001f) {
                if (item.pos.x >= range.box().minX && item.pos.x <= range.box().maxX &&
                        item.pos.y >= range.box().minZ && item.pos.y <= range.box().maxZ) {
                    return !range.extractionDisabled();
                }
            }
        }
        return true;
    }

    public boolean isInsertable(PlaceMatBlock.PlacementRange range, ItemStack stack) {
        if (level == null)
            return false;
        if (!(getBlockState().getBlock() instanceof PlaceMatBlock pmb))
            return false;
        if (pmb.isInsertionDisabled())
            return false;
        if (range.insertionDisabled())
            return false;
        return range.whitelistTag() == null || stack.is(range.whitelistTag());
    }

    public void removeItem(PlacedItem item) {
        if (placedItems.remove(item)) {
            if (level != null && !level.isClientSide) {
                removeTraits(item.stack, getRangeForItem(item));
            }
            updateTargetHeights();
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void removeItem(PlacedItem item, int amount) {
        if (amount >= item.stack.getCount()) {
            removeItem(item);
        } else {
            item.stack.shrink(amount);
            updateTargetHeights();
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public FoodTrait getFoodTrait() {
        if (level != null) {
            final float temp = Climate.getAverageTemperature(level, getBlockPos());
            if (temp < FLConfig.SERVER.cellarLevel3Temperature.get()) {
                return FLFoodTraits.SHELVED_3;
            }
            if (temp < FLConfig.SERVER.cellarLevel2Temperature.get()) {
                return FLFoodTraits.SHELVED_2;
            }
        }
        return FLFoodTraits.SHELVED;
    }

    public FoodTrait[] getPossibleTraits() {
        return POSSIBLE_SHELVED_TRAITS;
    }

    public void updatePreservation(boolean preserved) {
        for (PlacedItem placed : placedItems) {
            if (preserved) {
                FoodCapability.applyTrait(placed.stack, getFoodTrait());
            } else {
                for (FoodTrait trait : getPossibleTraits()) {
                    FoodCapability.removeTrait(placed.stack, trait);
                }
            }
        }
    }

    @Override
    public void setValid(@NotNull Level level, @NotNull BlockPos pos, boolean valid, int tier, @NotNull ClimateType climate) {
        if (climate == ClimateType.CELLAR) {
            this.climateValid = valid;
            updatePreservation(valid);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            updatePreservation(climateValid);
        }
    }

    public void removeTraitsFromStack(ItemStack stack, PlacedItem item) {
        removeTraits(stack, getRangeForItem(item));
    }

    private void applyTraits(ItemStack stack, @Nullable PlaceMatBlock.PlacementRange range) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!(getBlockState().getBlock() instanceof PlaceMatBlock pmb)) {
            return;
        }

        if (climateValid) {
            FoodCapability.applyTrait(stack, getFoodTrait());
        }

        ResourceLocation blockTraitId = pmb.getFoodTrait();
        if (blockTraitId != null) {
            FoodTrait trait = FoodTrait.getTrait(blockTraitId);
            if (trait != null) {
                FoodCapability.applyTrait(stack, trait);
            }
        }

        if (range != null) {
            ResourceLocation rangeTraitId = range.foodTrait();
            if (rangeTraitId != null) {
                FoodTrait trait = FoodTrait.getTrait(rangeTraitId);
                if (trait != null) {
                    FoodCapability.applyTrait(stack, trait);
                }
            }
        }
    }

    private void removeTraits(ItemStack stack, @Nullable PlaceMatBlock.PlacementRange range) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!(getBlockState().getBlock() instanceof PlaceMatBlock pmb)) {
            return;
        }

        for (FoodTrait trait : getPossibleTraits()) {
            FoodCapability.removeTrait(stack, trait);
        }

        ResourceLocation blockTraitId = pmb.getFoodTrait();
        if (blockTraitId != null) {
            FoodTrait trait = FoodTrait.getTrait(blockTraitId);
            if (trait != null) {
                FoodCapability.removeTrait(stack, trait);
            }
        }

        if (range != null) {
            ResourceLocation rangeTraitId = range.foodTrait();
            if (rangeTraitId != null) {
                FoodTrait trait = FoodTrait.getTrait(rangeTraitId);
                if (trait != null) {
                    FoodCapability.removeTrait(stack, trait);
                }
            }
        }
    }

    @Nullable
    public PlaceMatBlock.PlacementRange getRangeForItem(PlacedItem item) {
        if (!(getBlockState().getBlock() instanceof PlaceMatBlock pmb)) {
            return null;
        }
        for (PlaceMatBlock.PlacementRange range : pmb.getPlacementRanges()) {
            if (Math.abs(item.baseHeight - range.box().minY) < 0.001f) {
                if (item.pos.x >= range.box().minX && item.pos.x <= range.box().maxX &&
                        item.pos.y >= range.box().minZ && item.pos.y <= range.box().maxZ) {
                    return range;
                }
            }
        }
        return null;
    }

    public DefinitionManager.PlaceMatDefinition getEffectiveDefinition(ItemStack stack, @Nullable PlaceMatBlock.PlacementRange range) {
        DefinitionManager.PlaceMatDefinition def = DefinitionManager.getDefinition(stack.getItem());
        if (getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
            if (pmb.isDisableCustomModels() || (range != null && range.disableCustomModels())) {
                return DefinitionManager.PlaceMatDefinition.DEFAULT(stack.getItem());
            }
        }
        return def;
    }

    public float getScaleMultiplier(@Nullable PlaceMatBlock.PlacementRange range) {
        if (getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
            float multiplier = pmb.getScaleMultiplier();
            if (range != null) {
                multiplier *= range.scaleMultiplier();
            }
            return multiplier;
        }
        return 1.0f;
    }

    public float getEffectiveElevation(@Nullable PlaceMatBlock.PlacementRange range) {
        if (getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
            float elevation = pmb.getDefaultElevation();
            if (range != null) {
                elevation += range.defaultElevation();
            }
            return elevation;
        }
        return 0.0f;
    }

    public float[] getEffectiveRotation(@Nullable PlaceMatBlock.PlacementRange range) {
        if (getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
            float yaw = pmb.getDefaultYaw();
            float pitch = pmb.getDefaultPitch();
            float roll = pmb.getDefaultRoll();
            if (range != null) {
                yaw += range.defaultYaw();
                pitch += range.defaultPitch();
                roll += range.defaultRoll();
            }
            return new float[] { yaw, pitch, roll };
        }
        return new float[] { 0, 0, 0 };
    }

    @Nullable
    public PlacedItem getTargetedItem(Vec3 eyePos, Vec3 lookVec, BlockPos pos, @Nullable PlaceMatBlock.PlacementRange targetedRange) {
        double minDistance = Double.MAX_VALUE;
        PlacedItem targeted = null;

        BlockState state = getBlockState();
        net.minecraft.core.Direction facing = state.hasProperty(PlaceMatBlock.FACING) ? state.getValue(PlaceMatBlock.FACING) : net.minecraft.core.Direction.NORTH;
        Vec3 localEyePos = PlaceMatBlock.getLocalHitVec(state, eyePos.subtract(pos.getX(), pos.getY(), pos.getZ()));
        Vec3 localLookVec = PlaceMatBlock.rotateDirectionInverse(facing, lookVec);

        for (PlacedItem placed : placedItems) {
            if (targetedRange != null) {
                if (placed.baseHeight < targetedRange.box().minY - 0.001 || placed.baseHeight > targetedRange.box().maxY + 0.001) {
                    continue;
                }
            }

            // Makes the hitbox slightly larger to help with aiming.
            AABB box = placed.getVisualBox().inflate(0.02);
            var result = box.clip(localEyePos, localEyePos.add(localLookVec.scale(8)));
            if (result.isPresent()) {
                double dist = result.get().distanceToSqr(localEyePos);
                if (dist < minDistance) {
                    minDistance = dist;
                    targeted = placed;
                }
            }
        }
        return targeted;
    }

    @Nullable
    public PlacedItem getTargetedItem(Vec3 eyePos, Vec3 lookVec, BlockPos pos) {
        return getTargetedItem(eyePos, lookVec, pos, null);
    }

    private AABB getBoxForItem(Vec2 pos, Vec2 size, float height, @Nullable AABB targetBox, float itemHeight) {
        float yMin = targetBox != null ? (float) targetBox.minY : 0.0625f;
        float y = yMin + height;
        return new AABB(pos.x, y, pos.y, (double) pos.x + size.x, (double) y + itemHeight, (double) pos.y + size.y);
    }

    public void dropItems() {
        if (level != null) {
            for (PlacedItem placed : placedItems) {
                removeTraits(placed.stack, getRangeForItem(placed));
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), placed.stack);
            }
            placedItems.clear();
        }
    }

    public void tick(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        for (PlacedItem placed : placedItems) {
            placed.tick();
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("climateValid", climateValid);
        ListTag list = new ListTag();
        for (PlacedItem placed : placedItems) {
            CompoundTag itemTag = new CompoundTag();
            placed.stack.save(itemTag);
            itemTag.putFloat("x", placed.pos.x);
            itemTag.putFloat("y", placed.pos.y);
            itemTag.putFloat("rotation", placed.rotation);
            itemTag.putFloat("pitch", placed.pitch);
            itemTag.putFloat("roll", placed.roll);
            itemTag.putFloat("height", placed.manualHeight);
            itemTag.putFloat("baseHeight", placed.baseHeight);
            list.add(itemTag);
        }
        tag.put("Items", list);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.climateValid = tag.getBoolean("climateValid");
        List<PlacedItem> oldItems = new ArrayList<>(placedItems);
        placedItems.clear();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            ItemStack stack = ItemStack.of(itemTag);
            float x = itemTag.getFloat("x");
            float y = itemTag.getFloat("y");
            float rotation = itemTag.getFloat("rotation");
            float pitch = itemTag.getFloat("pitch");
            float roll = itemTag.getFloat("roll");
            float height = itemTag.getFloat("height");
            float baseHeight = itemTag.contains("baseHeight") ? itemTag.getFloat("baseHeight") : 0.0625f;

            PlacedItem newItem = new PlacedItem(this, stack, new Vec2(x, y), rotation, pitch, roll, height, baseHeight);

            for (int j = 0; j < oldItems.size(); j++) {
                PlacedItem old = oldItems.get(j);
                if (old.pos.x == x && old.pos.y == y && ItemStack.isSameItemSameTags(old.stack, stack)) {
                    newItem.setVisualHeight(old.getVisualHeight());
                    newItem.setVelocity(old.getVelocity());
                    oldItems.remove(j);
                    break;
                }
            }

            placedItems.add(newItem);
        }
        updateTargetHeights();
        if (level != null && level.isClientSide) {
            for (PlacedItem placed : placedItems) {
                placed.setVisualHeight(placed.targetHeight);
            }
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        assert pkt.getTag() != null;
        load(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0);
    }

    public boolean wasPlacedThisTick() {
        return level != null && level.getGameTime() == lastPlacementTick;
    }

    public boolean canInteract() {
        return level == null || lastInteractionTick == -1 || level.getGameTime() - lastInteractionTick >= 20;
    }

    public void markInteracted() {
        if (level != null) {
            this.lastInteractionTick = level.getGameTime();
        }
    }

    public static class PlacedItem {
        private final PlaceMatBlockEntity parent;
        public ItemStack stack;
        public final Vec2 pos;
        public final float rotation;
        public final float pitch;
        public final float roll;
        public final float manualHeight;
        public final float baseHeight;

        @Getter
        @Setter
        private float visualHeight;
        @Getter
        @Setter
        private float targetHeight;
        @Getter
        @Setter
        private float velocity;

        public PlacedItem(PlaceMatBlockEntity parent, ItemStack stack, Vec2 pos, float rotation, float pitch, float roll, float manualHeight, float baseHeight) {
            this.parent = parent;
            this.stack = stack;
            this.pos = pos;
            this.rotation = rotation;
            this.pitch = pitch;
            this.roll = roll;
            this.manualHeight = manualHeight;
            this.baseHeight = baseHeight;
            this.targetHeight = manualHeight;
            this.visualHeight = manualHeight;
            this.velocity = 0;
        }

        public void tick() {
            if (visualHeight > targetHeight + 0.001f) {
                velocity -= 0.005f; // Gravity
                visualHeight += velocity;
                if (visualHeight <= targetHeight) {
                    visualHeight = targetHeight;
                    velocity = 0;
                }
            } else if (visualHeight < targetHeight - 0.001f) {
                visualHeight = targetHeight;
                velocity = 0;
            } else {
                visualHeight = targetHeight;
                velocity = 0;
            }
        }

        public float getEffectiveHeight() {
            return targetHeight;
        }

        public DefinitionManager.PlaceMatDefinition getEffectiveDefinition() {
            return parent.getEffectiveDefinition(stack, parent.getRangeForItem(this));
        }

        public AABB getLogicalBox() {
            PlaceMatBlock.PlacementRange range = parent.getRangeForItem(this);
            float multiplier = parent.getScaleMultiplier(range);
            float elevation = parent.getEffectiveElevation(range);
            float y = baseHeight + targetHeight + elevation;
            DefinitionManager.PlaceMatDefinition def = getEffectiveDefinition();
            return new AABB(pos.x, y, pos.y, (double) pos.x + def.size().x * multiplier, (double) y + def.getItemHeight() * multiplier, (double) pos.y + def.size().y * multiplier);
        }

        public AABB getVisualBox() {
            PlaceMatBlock.PlacementRange range = parent.getRangeForItem(this);
            float multiplier = parent.getScaleMultiplier(range);
            float elevation = parent.getEffectiveElevation(range);
            float y = baseHeight + visualHeight + elevation;
            DefinitionManager.PlaceMatDefinition def = getEffectiveDefinition();
            return new AABB(pos.x, y, pos.y, (double) pos.x + def.size().x * multiplier, (double) y + def.getItemHeight() * multiplier, (double) pos.y + def.size().y * multiplier);
        }

        @Deprecated
        public AABB getBox() {
            return getLogicalBox();
        }
    }
}

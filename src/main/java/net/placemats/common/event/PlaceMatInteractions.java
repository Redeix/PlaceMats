package net.placemats.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.registries.ForgeRegistries;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlacementRange;
import net.placemats.config.ConfigHelpers;
import org.jetbrains.annotations.Nullable;

import net.placemats.compat.tfc.TFCCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.data.PlaceMatEntities;
import net.placemats.common.entity.SeatEntity;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;
import net.placemats.common.data.PlaceMatRecipeTypes;
import net.placemats.common.recipe.PlaceMatRecipe;
import net.placemats.compat.kjs.KJSCompat;

/**
 * Handles placemat interactions for recipes and events.
 * TODO: Rewrite this class. A lot of it is redundant.
 */
@SuppressWarnings("unused")
public class PlaceMatInteractions {

    public static final String CHAIR_COOLDOWN_TAG = "PlaceMatChairCooldown";
    public static final int KICK_COOLDOWN_TICKS = 60;

    public static InteractionResult handleInteraction(PlaceMatBlockEntity placeMat, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.FAIL;
        }
        Level level = placeMat.getLevel();
        if (PlaceMatBlock.preventsInteraction(placeMat.getBlockState()))
            return InteractionResult.PASS;
        if (level == null)
            return InteractionResult.PASS;
        BlockPos pos = placeMat.getBlockPos();
        ItemStack held = player.getItemInHand(hand);

        if (placeMat.wasPlacedThisTick()) {
            return InteractionResult.SUCCESS;
        }

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        PlacedItem targeted = placeMat.getTargetedItem(eyePos, lookVec, pos);

        if (tryRecipe(placeMat, player, hand, targeted)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (targeted != null) {
            int zoneIndex = getZoneIndex(placeMat, targeted);

            InteractionResult result = KJSCompat.INSTANCE.onInteraction(player, targeted, zoneIndex, placeMat);
            if (result != InteractionResult.PASS) {
                return result;
            }

            if (held.isEmpty()) {
                if (player.isShiftKeyDown()) {
                    // Extract whole stack.
                    return onStackPickup(placeMat, player, targeted);
                } else {
                    // Extract item.
                    return onPickup(placeMat, player, targeted);
                }
            }
        }

        // Sitting interaction
        if (placeMat.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
            Vec3 relativeHit = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            PlacementRange targetedRange = pmb.getTargetedPlacementRange(placeMat.getBlockState(), relativeHit);
            if (targetedRange != null && targetedRange.chair()) {
                if (!player.isShiftKeyDown()) {
                    if (!level.isClientSide) {
                        // Sit Mob
                        List<Mob> leashedMobs = level.getEntitiesOfClass(
                                Mob.class,
                                player.getBoundingBox().inflate(7.0),
                                mob -> mob.getLeashHolder() == player && !ConfigHelpers.isMobChairBlacklisted(mob.getType())
                        );
                        if (!leashedMobs.isEmpty()) {
                            Mob leashedMob = leashedMobs.get(0);
                            leashedMob.getPersistentData().remove(CHAIR_COOLDOWN_TAG);
                            InteractionResult sitResult = sitEntity(level, pos, placeMat.getBlockState(), targetedRange, leashedMob);
                            if (sitResult.consumesAction()) {
                                //leashedMob.dropLeash(true, !player.getAbilities().instabuild);
                                return sitResult;
                            }
                        }
                    }
                    // Sit Player
                    return sitPlayer(level, pos, placeMat.getBlockState(), targetedRange, player);
                } else {
                    if (!level.isClientSide) {
                        return kickEntity(level, pos, targetedRange);
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Attempts to sit the player in the targeted placement range.
     */
    public static InteractionResult sitPlayer(Level level, BlockPos pos, BlockState state, PlacementRange range, Player player) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        return sitEntity(level, pos, state, range, player);
    }

    /**
     * Attempts to sit an entity in the targeted placement range.
     */
    public static InteractionResult sitEntity(Level level, BlockPos pos, BlockState state, PlacementRange range, Entity entity) {
        if (!range.chair()
            || (!(entity instanceof Player) && entity.isPassenger())
            || entity.isVehicle()
            || !entity.isAlive()
            || PlaceMatBlock.preventsInteraction(state)
            || ConfigHelpers.isMobChairBlacklisted(entity.getType())
        ) {
            return InteractionResult.PASS;
        }

        if (entity.getPersistentData().contains(CHAIR_COOLDOWN_TAG)) {
            if (level.getGameTime() < entity.getPersistentData().getLong(CHAIR_COOLDOWN_TAG)) {
                return InteractionResult.PASS;
            }
        }

        double localCenterX = (range.box().minX + range.box().maxX) / 2.0;
        double localCenterY = range.box().minY;
        double localCenterZ = (range.box().minZ + range.box().maxZ) / 2.0;

        Vec3 worldHit = PlaceMatBlock.getWorldHitVec(state, new Vec3(localCenterX, localCenterY, localCenterZ));
        double x = pos.getX() + worldHit.x;
        double y = pos.getY() + worldHit.y;
        double z = pos.getZ() + worldHit.z;

        List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(x - 0.2, y - 0.2, z - 0.2, x + 0.2, y + 0.2, z + 0.2));
        for (SeatEntity seat : seats) {
            if (seat.isVehicle()) {
                return InteractionResult.CONSUME;
            }
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        for (SeatEntity seat : seats) {
            seat.discard();
        }

        SeatEntity seat = PlaceMatEntities.SEAT.get().create(level);
        if (seat != null) {
            seat.setPos(x, y, z);
            if (state.hasProperty(PlaceMatCardinalBlock.FACING)) {
                seat.setYRot(state.getValue(PlaceMatCardinalBlock.FACING).toYRot());
            }
            level.addFreshEntity(seat);
            entity.startRiding(seat, true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }


    /**
     * Attempts to kick an entity from a seat anywhere on the block.
     */
    public static InteractionResult kickEntity(Level level, BlockPos pos) {
        return kickEntity(level, pos, null);
    }

    /**
     * Attempts to kick an entity from a seat in the specified placement range.
     */
    public static InteractionResult kickEntity(Level level, BlockPos pos, @Nullable PlacementRange range) {
        BlockState state = level.getBlockState(pos);
        List<SeatEntity> seats;

        if (range != null) {
            double localCenterX = (range.box().minX + range.box().maxX) / 2.0;
            double localCenterY = range.box().minY;
            double localCenterZ = (range.box().minZ + range.box().maxZ) / 2.0;

            Vec3 worldHit = PlaceMatBlock.getWorldHitVec(state, new Vec3(localCenterX, localCenterY, localCenterZ));
            double x = pos.getX() + worldHit.x;
            double y = pos.getY() + worldHit.y;
            double z = pos.getZ() + worldHit.z;

            seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(x - 0.2, y - 0.2, z - 0.2, x + 0.2, y + 0.2, z + 0.2), seat -> !seat.getPassengers().isEmpty());
        } else {
            AABB seatArea = new AABB(pos).inflate(0, 1, 0);
            seats = level.getEntitiesOfClass(SeatEntity.class, seatArea, seat -> !seat.getPassengers().isEmpty());
        }

        for (SeatEntity seat : seats) {
            Entity passenger = seat.getFirstPassenger();
            if (passenger != null && !(passenger instanceof Player)) {
                passenger.stopRiding();
                seat.discard();

                // Prevent immediate re-entry with cooldown.
                passenger.getPersistentData().putLong(CHAIR_COOLDOWN_TAG, level.getGameTime() + KICK_COOLDOWN_TICKS);

                // Displace entity away from the block.
                Direction facing = state.hasProperty(PlaceMatCardinalBlock.FACING) ? state.getValue(PlaceMatCardinalBlock.FACING) : Direction.SOUTH;
                Vec3 seatPos = seat.position();
                Vec3 dismountPos = null;

                Direction[] tryDirs = new Direction[]{
                        facing,
                        facing.getClockWise(),
                        facing.getCounterClockWise(),
                        facing.getOpposite()
                };

                for (Direction dir : tryDirs) {
                    BlockPos checkPos = pos.relative(dir);
                    if (level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty() &&
                            !level.getBlockState(checkPos.below()).getCollisionShape(level, checkPos.below()).isEmpty()) {
                        dismountPos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                        facing = dir;
                        break;
                    }
                }

                if (dismountPos == null) {
                    for (Direction dir : tryDirs) {
                        BlockPos checkPos = pos.relative(dir);
                        if (level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                            dismountPos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                            facing = dir;
                            break;
                        }
                    }
                }

                if (dismountPos != null) {
                    passenger.teleportTo(dismountPos.x, dismountPos.y, dismountPos.z);
                } else {
                    passenger.setPos(seatPos.x + facing.getStepX() * 0.8, seatPos.y, seatPos.z + facing.getStepZ() * 0.8);
                }

                passenger.setDeltaMovement(facing.getStepX() * 0.25, 0.15, facing.getStepZ() * 0.25);
                passenger.hurtMarked = true;

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Gets the entity currently in the placement range.
     */
    public static @Nullable Entity getEntityInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        if (level == null || range == null) {
            return null;
        }

        if (range.chair()) {
            double localCenterX = (range.box().minX + range.box().maxX) / 2.0;
            double localCenterY = range.box().minY;
            double localCenterZ = (range.box().minZ + range.box().maxZ) / 2.0;

            Vec3 worldHit = PlaceMatBlock.getWorldHitVec(state, new Vec3(localCenterX, localCenterY, localCenterZ));
            double x = pos.getX() + worldHit.x;
            double y = pos.getY() + worldHit.y;
            double z = pos.getZ() + worldHit.z;

            List<SeatEntity> seats = level.getEntitiesOfClass(
                SeatEntity.class,
                new AABB(x - 0.2, y - 0.2, z - 0.2, x + 0.2, y + 0.2, z + 0.2)
            );
            for (SeatEntity seat : seats) {
                Entity passenger = seat.getFirstPassenger();
                if (passenger != null && passenger.isAlive()) {
                    return passenger;
                }
            }

            return null;
        }

        AABB worldRangeBox = PlaceMatBlock.getWorldRangeBox(state, pos, range);
        List<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            worldRangeBox,
            e -> e.isAlive() && !(e instanceof SeatEntity)
        );
        return entities.isEmpty() ? null : entities.get(0);
    }

    /**
     * Gets the UUID of the entity in the placement range, if any.
     */
    public static @Nullable UUID getEntityUUIDInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        Entity entity = getEntityInRange(level, pos, state, range);
        return entity != null ? entity.getUUID() : null;
    }

    /**
     * Gets the EntityType of the entity in the placement range, if any.
     */
    public static @Nullable EntityType<?> getEntityTypeInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        Entity entity = getEntityInRange(level, pos, state, range);
        return entity != null ? entity.getType() : null;
    }

    /**
     * Returns whether there is an entity currently in the placement range.
     */
    public static boolean hasEntityInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return getEntityInRange(level, pos, state, range) != null;
    }

    /**
     * Gets the Mob currently in the specified placement range, if any.
     */
    public static @Nullable Mob getMobInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        Entity entity = getEntityInRange(level, pos, state, range);
        return entity instanceof Mob mob ? mob : null;
    }

    /**
     * Gets the UUID of the Mob in the placement range, if any.
     */
    public static @Nullable UUID getMobUUIDInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        Mob mob = getMobInRange(level, pos, state, range);
        return mob != null ? mob.getUUID() : null;
    }

    /**
     * Gets the EntityType of the Mob in the placement range, if any.
     */
    public static @Nullable EntityType<?> getMobTypeInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        Mob mob = getMobInRange(level, pos, state, range);
        return mob != null ? mob.getType() : null;
    }

    /**
     * Returns whether there is a Mob currently in the placement range.
     */
    public static boolean hasMobInRange(Level level, BlockPos pos, BlockState state, @Nullable PlacementRange range) {
        return getMobInRange(level, pos, state, range) != null;
    }


    public static boolean handleLeftClick(PlaceMatBlockEntity placeMat, Player player, @Nullable BlockHitResult hit) {
        if (player.isSpectator()) {
            return false;
        }
        Level level = placeMat.getLevel();
        if (PlaceMatBlock.preventsInteraction(placeMat.getBlockState()))
            return false;
        if (level == null)
            return false;
        BlockPos pos = placeMat.getBlockPos();

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        if (hit == null) {
            hit = player.pick(5.0, 0, false) instanceof BlockHitResult bhr ? bhr : null;
            if (hit != null && !hit.getBlockPos().equals(pos)) {
                hit = null;
            }
        }

        PlacedItem targeted = placeMat.getTargetedItem(eyePos, lookVec, pos);

        if (targeted != null) {
            if (!level.isClientSide) {
                if (!placeMat.wasPlacedThisTick() && placeMat.canInteract()) {
                    // Try offhand recipe first.
                    if (tryRecipe(placeMat, player, InteractionHand.OFF_HAND, targeted)) {
                        placeMat.markInteracted();
                        return true;
                    }

                    // Then try main hand recipe.
                    if (tryRecipe(placeMat, player, InteractionHand.MAIN_HAND, targeted)) {
                        placeMat.markInteracted();
                        return true;
                    }

                    int zoneIndex = getZoneIndex(placeMat, targeted);

                    InteractionResult result = KJSCompat.INSTANCE.onInteraction(player, targeted, zoneIndex, placeMat);
                    if (result != InteractionResult.PASS) {
                        placeMat.markInteracted();
                        return true;
                    }

                    if (handleEatDrink(placeMat, player, targeted, level) != InteractionResult.PASS) {
                        placeMat.markInteracted();
                        return true;
                    }
                }
            }
            return true;
        }

        return false;
    }

    private static int getZoneIndex(PlaceMatBlockEntity foodPlacer, PlacedItem targeted) {
        if (foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
            var ranges = pmb.getPlacementRanges(foodPlacer.getBlockState());
            var range = foodPlacer.getRangeForItem(targeted);
            if (range != null) {
                return ranges.indexOf(range);
            }
        }
        return -1;
    }

    public static boolean tryRecipe(PlaceMatBlockEntity placeMat, Player player, InteractionHand hand, @Nullable PlacedItem targeted) {
        Level level = placeMat.getLevel();
        if (level == null)
            return false;
        if (PlaceMatBlock.preventsInteraction(placeMat.getBlockState()))
            return false;

        ItemStack inputStack = player.getItemInHand(hand);
        BlockState state = placeMat.getBlockState();
        Block block = state.getBlock();
        int zoneIndex = targeted != null ? getZoneIndex(placeMat, targeted) : -1;

        List<PlaceMatRecipe> recipes = level.getRecipeManager().getAllRecipesFor(PlaceMatRecipeTypes.PLACE_MAT.get());
        for (PlaceMatRecipe recipe : recipes) {
            // Check block/tag.
            if (recipe.getBlock() != null && recipe.getBlock() != block)
                continue;
            if (recipe.getBlockTag() != null && !state.is(recipe.getBlockTag()))
                continue;

            // Check zone index.
            if (recipe.getZoneIndex() != null && recipe.getZoneIndex() != zoneIndex)
                continue;

            // Check input.
            Ingredient recipeInput = recipe.getInput();
            if (!recipeInput.test(inputStack))
                continue;
            if (isRotten(inputStack))
                continue;
            int inputCount = recipe.getInputCount();
            if (inputStack.isDamageableItem()) {
                int remaining = inputStack.getMaxDamage() - inputStack.getDamageValue();
                if (remaining < inputCount)
                    continue;
            } else {
                if (inputStack.getCount() < inputCount)
                    continue;
            }

            // Check target input.
            ItemStack targetStack = targeted != null ? targeted.stack : ItemStack.EMPTY;
            Ingredient recipeTarget = recipe.getTargetInput();
            if (!recipeTarget.test(targetStack))
                continue;
            if (isRotten(targetStack))
                continue;
            int targetCount = recipe.getTargetInputCount();
            if (targetStack.isDamageableItem()) {
                int remaining = targetStack.getMaxDamage() - targetStack.getDamageValue();
                if (remaining < targetCount)
                    continue;
            } else {
                if (targetStack.getCount() < targetCount)
                    continue;
            }

            if (level.isClientSide)
                return true;

            ItemStack inputCopy = inputStack.copy();
            ItemStack targetCopy = targetStack.copy();

            // TFC context.
            List<ItemStack> ctx = new ArrayList<>();
            if (targeted != null && !targetCopy.isEmpty())
                ctx.add(targetCopy);
            else if (!inputCopy.isEmpty())
                ctx.add(inputCopy);
            else
                ctx.add(ItemStack.EMPTY);

            TFCCompat.INSTANCE.setCraftingInput(ctx);
            java.util.List<ItemStack> results = new java.util.ArrayList<>();
            for (int i = 0; i < recipe.getResultProviders().size(); i++) {
                Object provider = recipe.getResultProviders().get(i);
                if (i == 0) {
                    results.add(TFCCompat.INSTANCE.getStackFromProvider(provider, ctx.get(0)));
                } else {
                    results.add(((ItemStack) provider).copy());
                }
            }
            TFCCompat.INSTANCE.clearCraftingInput();

            // Consume/Damage input item.
            if (inputCount > 0) {
                if (inputStack.isDamageableItem()) {
                    inputStack.hurtAndBreak(inputCount, player, (p) -> p.broadcastBreakEvent(hand));
                } else {
                    inputStack.shrink(inputCount);
                }
            }

            // Consume/Damage target item.
            boolean targetDepleted = false;
            if (targeted != null && targetCount > 0) {
                if (targeted.stack.isDamageableItem()) {
                    targeted.stack.hurtAndBreak(targetCount, player, (p) -> {
                    });
                } else {
                    targeted.stack.shrink(targetCount);
                }
                if (targeted.stack.isEmpty()) {
                    targetDepleted = true;
                }
            }

            for (ItemStack result : results) {
                if (result.isEmpty()) continue;
                if (targeted != null && targetDepleted) {
                    // Try to put it back on the mat.
                    PlacementRange range = placeMat.getRangeForItem(targeted);
                    if (range != null && placeMat.isInsertable(range, result)) {
                        targeted.stack = result;
                        targetDepleted = false;
                    } else {
                        ItemHandlerHelper.giveItemToPlayer(player, result);
                    }
                } else {
                    ItemHandlerHelper.giveItemToPlayer(player, result);
                }
            }

            // Sound.
            if (recipe.getSound() != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getValue(recipe.getSound())),
                    SoundSource.PLAYERS, recipe.getVolume(), recipe.getPitch());
            }

            if (targetDepleted) {
                placeMat.removeItem(targeted);
            } else if (targeted != null) {
                placeMat.updateTargetHeights();
                placeMat.setChanged();
                level.sendBlockUpdated(placeMat.getBlockPos(), state, state, 3);
            }

            return true;
        }
        return false;
    }

    private static boolean isRotten(ItemStack stack) {
        return TFCCompat.INSTANCE.isRotten(stack);
    }


    public static InteractionResult onPickup(PlaceMatBlockEntity be, Player player, PlacedItem item) {
        assert be.getLevel() != null;
        if (!be.getLevel().isClientSide) {
            ItemStack toGive = item.stack.copy();
            toGive.setCount(1);
            be.removeTraitsFromStack(toGive, item);
            be.removeItem(item, 1);
            ItemHandlerHelper.giveItemToPlayer(player, toGive);
        }
        return InteractionResult.sidedSuccess(be.getLevel().isClientSide);
    }

    public static InteractionResult onStackPickup(PlaceMatBlockEntity be, Player player, PlacedItem item) {
        assert be.getLevel() != null;
        if (!be.getLevel().isClientSide) {
            ItemStack stack = item.stack;
            be.removeItem(item);
            ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
        return InteractionResult.sidedSuccess(be.getLevel().isClientSide);
    }

    public static InteractionResult handleEatDrink(PlaceMatBlockEntity pmbe, Player player, PlacedItem targeted, Level level) {
        ItemStack stack = targeted.stack;
        int countBefore = stack.getCount();

        UseAnim anim = stack.getUseAnimation();
        boolean isEdible = stack.getItem().isEdible() || TFCCompat.INSTANCE.hasFoodCapability(stack);
        boolean isDrinkable = anim == UseAnim.DRINK;

        if ((isEdible || isDrinkable) && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
            FoodProperties foodProperties = stack.getFoodProperties(player);
            boolean canAlwaysEat = foodProperties != null && foodProperties.canAlwaysEat();
            if (isDrinkable || player.canEat(canAlwaysEat)) {
                if (!level.isClientSide) {
                    // TFC fallback for bowl logic.
                    ItemStack container = ItemStack.EMPTY;
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.contains("bowl", Tag.TAG_COMPOUND)) {
                        container = ItemStack.of(tag.getCompound("bowl"));
                    }

                    ItemStack result = stack.finishUsingItem(level, player);

                    // Prefer TFC container if present.
                    if (!container.isEmpty()) {
                        result = container;
                    }

                    // Handle shrinking of the stack.
                    if (stack.getCount() == countBefore && !stack.isEmpty()) {
                        if (result != stack || player.isCreative() || (isEdible && !isDrinkable)) {
                            stack.shrink(1);
                        }
                    }

                    if (!result.isEmpty()) {
                        if (result != stack) {
                            // Replaced item.
                            pmbe.removeTraitsFromStack(result, targeted);
                            if (stack.isEmpty()) {
                                // Put it back on the mat.
                                PlacementRange range = pmbe.getRangeForItem(targeted);
                                if (range != null && pmbe.isInsertable(range, result)) {
                                    targeted.stack = result;
                                } else {
                                    ItemHandlerHelper.giveItemToPlayer(player, result);
                                }
                            } else {
                                // Multi-stack, give container to player.
                                ItemHandlerHelper.giveItemToPlayer(player, result);
                            }
                        } else {
                            // Same item, already on mat.
                            pmbe.removeTraitsFromStack(result, targeted);
                        }
                    }

                    if (isDrinkable) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }

                    if (targeted.stack.isEmpty()) {
                        pmbe.removeItem(targeted);
                    } else {
                        pmbe.updateTargetHeights();
                        pmbe.setChanged();
                        level.sendBlockUpdated(pmbe.getBlockPos(), pmbe.getBlockState(), pmbe.getBlockState(), 3);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
}

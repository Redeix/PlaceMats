package net.placemats.common.event;

import java.util.ArrayList;
import java.util.List;

import net.placemats.common.block.PlaceMatBlock;
import org.jetbrains.annotations.Nullable;

import net.placemats.compat.tfc.TFCCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;
import net.placemats.common.data.PlaceMatRecipeTypes;
import net.placemats.common.recipe.PlaceMatRecipe;
import net.placemats.compat.kjs.KJSCompat;

public class PlaceMatInteractions {

    public static InteractionResult handleInteraction(PlaceMatBlockEntity placeMat, Player player, InteractionHand hand, BlockHitResult hit) {
        Level level = placeMat.getLevel();
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

        return InteractionResult.PASS;
    }

    public static boolean handleLeftClick(PlaceMatBlockEntity placeMat, Player player, @Nullable BlockHitResult hit) {
        Level level = placeMat.getLevel();
        if (level == null)
            return false;
        BlockPos pos = placeMat.getBlockPos();

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        if (hit == null) {
            // Raytrace on server to find the hit point on the block.
            hit = player.pick(5.0, 0, false) instanceof BlockHitResult bhr ? bhr : null;
            if (hit != null && !hit.getBlockPos().equals(pos)) {
                hit = null;
            }
        }

        PlacedItem targeted = placeMat.getTargetedItem(eyePos, lookVec, pos);

        if (targeted != null) {
            if (!level.isClientSide) {
                if (!placeMat.wasPlacedThisTick() && placeMat.canInteract()) {
                    // Try offhand recipe first
                    if (tryRecipe(placeMat, player, InteractionHand.OFF_HAND, targeted)) {
                        placeMat.markInteracted();
                        return true;
                    }

                    // Then try main hand recipe
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
            var ranges = pmb.getPlacementRanges();
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

        ItemStack inputStack = player.getItemInHand(hand);
        BlockState state = placeMat.getBlockState();
        Block block = state.getBlock();
        int zoneIndex = targeted != null ? getZoneIndex(placeMat, targeted) : -1;

        List<PlaceMatRecipe> recipes = level.getRecipeManager().getAllRecipesFor(PlaceMatRecipeTypes.PLACE_MAT.get());
        for (PlaceMatRecipe recipe : recipes) {
            // Check block/tag
            if (recipe.getBlock() != null && recipe.getBlock() != block)
                continue;
            if (recipe.getBlockTag() != null && !state.is(recipe.getBlockTag()))
                continue;

            // Check zone index
            if (recipe.getZoneIndex() != null && recipe.getZoneIndex() != zoneIndex)
                continue;

            // Check input
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

            // Check target input
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

            // Match found!
            if (level.isClientSide)
                return true;

            // Pre-consumption copies for context
            ItemStack inputCopy = inputStack.copy();
            ItemStack targetCopy = targetStack.copy();

            // Seed TFC context
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

            // Consume/Damage input item
            if (inputCount > 0) {
                if (inputStack.isDamageableItem()) {
                    inputStack.hurtAndBreak(inputCount, player, (p) -> p.broadcastBreakEvent(hand));
                } else {
                    inputStack.shrink(inputCount);
                }
            }

            // Consume/Damage target item
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
                    // Try to put it back on the mat
                    PlaceMatBlock.PlacementRange range = placeMat.getRangeForItem(targeted);
                    if (range != null && placeMat.isInsertable(range, result)) {
                        targeted.stack = result;
                        targetDepleted = false; // Successfully replaced!
                    } else {
                        ItemHandlerHelper.giveItemToPlayer(player, result);
                    }
                } else {
                    ItemHandlerHelper.giveItemToPlayer(player, result);
                }
            }

            // Sound
            if (recipe.getSound() != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        BuiltInRegistries.SOUND_EVENT.get(recipe.getSound()),
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

    public static InteractionResult handleEatDrink(PlaceMatBlockEntity foodPlacer, Player player, PlacedItem targeted, Level level) {
        ItemStack stack = targeted.stack;
        int countBefore = stack.getCount();

        UseAnim anim = stack.getUseAnimation();
        boolean isEdible = stack.getItem().isEdible() || TFCCompat.INSTANCE.hasFoodCapability(stack);
        boolean isDrinkable = anim == UseAnim.DRINK;

        if (isEdible || isDrinkable) {
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
                    // If finishUsingItem didn't shrink the stack, and it's not a multi-use item, shrink it manually.
                    if (stack.getCount() == countBefore && !stack.isEmpty()) {
                        if (result != stack || player.isCreative() || (isEdible && !isDrinkable)) {
                            stack.shrink(1);
                        }
                    }

                    if (!result.isEmpty()) {
                        if (result != stack) {
                            // Replaced item.
                            foodPlacer.removeTraitsFromStack(result, targeted);
                            if (stack.isEmpty()) {
                                // Put it back on the mat.
                                PlaceMatBlock.PlacementRange range = foodPlacer.getRangeForItem(targeted);
                                if (range != null && foodPlacer.isInsertable(range, result)) {
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
                            foodPlacer.removeTraitsFromStack(result, targeted);
                        }
                    }

                    if (isDrinkable) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }

                    if (targeted.stack.isEmpty()) {
                        foodPlacer.removeItem(targeted);
                    } else {
                        foodPlacer.updateTargetHeights();
                        foodPlacer.setChanged();
                        level.sendBlockUpdated(foodPlacer.getBlockPos(), foodPlacer.getBlockState(), foodPlacer.getBlockState(), 3);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }
}

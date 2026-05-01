/*
 * This file includes code from TFC (https://github.com/TerraFirmaCraft/TerraFirmaCraft?tab=EUPL-1.2-1-ov-file)
 * EUPL © the European Union 2007, 2016
 * European Union Public Licence
 * V. 1.2
 */
package net.placemats.compat.jade;

import java.util.ArrayList;
import java.util.List;

import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.capabilities.food.IFood;
import net.dries007.tfc.common.capabilities.food.Nutrient;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.blockentity.PlaceMatBlockEntity;

/**
 * Provides tooltip information for Place Mat block entities.
 */
public enum PlaceMatProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof PlaceMatBlockEntity foodPlacer) {
            IElementHelper elementHelper = tooltip.getElementHelper();
            Vec3 hitVec = accessor.getHitResult().getLocation().subtract(accessor.getPosition().getX(), accessor.getPosition().getY(), accessor.getPosition().getZ());
            Vec3 eyePos = accessor.getPlayer().getEyePosition(1.0f);
            Vec3 lookVec = accessor.getPlayer().getViewVector(1.0f);

            PlaceMatBlock.PlacementRange targetedRange = null;
            if (foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                targetedRange = pmb.getTargetedPlacementRange(foodPlacer.getBlockState(), hitVec);
            }

            PlaceMatBlockEntity.PlacedItem targetedItem = foodPlacer.getTargetedItem(eyePos, lookVec, accessor.getPosition(), targetedRange);
            boolean containsItems = !foodPlacer.getPlacedItems().isEmpty();

            for (PlaceMatBlockEntity.PlacedItem placed : foodPlacer.getPlacedItems()) {
                // If we are looking at a specific box, only show items in that box.
                if (targetedRange != null) {
                    if (placed.baseHeight < targetedRange.box().minY - 0.001 || placed.baseHeight > targetedRange.box().maxY + 0.001) {
                        continue;
                    }
                }

                // Single item view by default.
                if (placed == targetedItem && !Screen.hasAltDown()) {
                    ItemStack stack = placed.stack;
                    tooltip.add(elementHelper.item(stack));
                    MutableComponent name = stack.getHoverName().copy();

                    name.withStyle(ChatFormatting.GOLD);
                    tooltip.append(name);

                    IFood food = FoodCapability.get(stack);
                    if (food != null) {
                        List<Component> foodInfo = new ArrayList<>();
                        addFoodTooltipInfo(food, stack, foodInfo);
                        for (Component component : foodInfo) {
                            tooltip.add(component);
                        }
                    }
                }
                // Detailed view when holding alt.
                if (Screen.hasAltDown()) {
                    ItemStack stack = placed.stack;
                    tooltip.add(elementHelper.item(stack));
                    MutableComponent name = stack.getHoverName().copy();

                    name.withStyle(ChatFormatting.GRAY);
                    tooltip.append(name);

                    // Makes the hovered item name gold.
                    if (placed == targetedItem) {
                        name.withStyle(ChatFormatting.GOLD);
                    }

                    IFood food = FoodCapability.get(stack);
                    if (food != null) {
                        List<Component> foodInfo = new ArrayList<>();
                        addFoodTooltipInfo(food, stack, foodInfo);
                        for (Component component : foodInfo) {
                            tooltip.add(component);
                        }
                    }
                }
            }
            if (!Screen.hasAltDown() && containsItems) {
                tooltip.add(Component.translatable("place_mats.tooltip.placemat.hold_alt_for_nutrition_info").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
        }
    }

    /**
     * Attaches tfc food tooltip information to the jade list entries.
     * Mostly copied from {@link net.dries007.tfc.common.capabilities.food.IFood}
     */
    private void addFoodTooltipInfo(IFood food, ItemStack stack, List<Component> text) {
        // Expiration.
        if (Screen.hasAltDown()) {
            if (food.isRotten()) {
                text.add(Component.translatable("tfc.tooltip.food_rotten").withStyle(ChatFormatting.RED));
            } else {
                final long rottenDate = food.getRottenDate();
                if (rottenDate == FoodHandler.NEVER_DECAY_DATE) {
                    if (!food.isTransientNonDecaying()) {
                        text.add(Component.translatable("tfc.tooltip.food_infinite_expiry").withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    final long rottenCalendarTime = Calendars.CLIENT.ticksToCalendarTicks(rottenDate);
                    final long ticksRemaining = rottenDate - Calendars.CLIENT.getTicks();

                    final MutableComponent expiryTooltip = switch (TFCConfig.CLIENT.foodExpiryTooltipStyle.get()) {
                        case EXPIRY -> Component.translatable("tfc.tooltip.food_expiry_date", ICalendar.getTimeAndDate(rottenCalendarTime, Calendars.CLIENT.getCalendarDaysInMonth()));
                        case TIME_LEFT -> Component.translatable("tfc.tooltip.food_expiry_left", Calendars.CLIENT.getTimeDelta(ticksRemaining));
                        case BOTH -> Component.translatable("tfc.tooltip.food_expiry_date_and_left", ICalendar.getTimeAndDate(rottenCalendarTime, Calendars.CLIENT.getCalendarDaysInMonth()),
                                Calendars.CLIENT.getTimeDelta(ticksRemaining));
                        default -> null;
                    };
                    if (expiryTooltip != null) {
                        text.add(expiryTooltip.withStyle(ChatFormatting.DARK_GREEN));
                    }
                }
            }

            // Nutrition.
            text.add(Component.translatable("tfc.tooltip.nutrition").withStyle(ChatFormatting.GRAY));

            boolean any = false;
            if (!food.isRotten()) {
                final FoodData data = food.getData();

                float saturation = data.saturation();
                if (saturation > 0) {
                    text.add(Component.translatable("tfc.tooltip.nutrition_saturation", String.format("%d", (int) (saturation * 5))).withStyle(ChatFormatting.GRAY));
                    any = true;
                }
                int water = (int) data.water();
                if (water > 0) {
                    text.add(Component.translatable("tfc.tooltip.nutrition_water", String.format("%d", water)).withStyle(ChatFormatting.GRAY));
                    any = true;
                }

                for (Nutrient nutrient : Nutrient.VALUES) {
                    float value = data.nutrient(nutrient);
                    if (value > 0) {
                        text.add(Component.literal(" - ")
                                .append(Helpers.translateEnum(nutrient))
                                .append(": " + String.format("%.1f", value))
                                .withStyle(nutrient.getColor()));
                        any = true;
                    }
                }
            }
            if (!any) {
                text.add(Component.translatable("tfc.tooltip.nutrition_none").withStyle(ChatFormatting.GRAY));
            }
        }

        // Add info for each trait.
        for (FoodTrait trait : food.getTraits()) {
            trait.addTooltipInfo(stack, text);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return PlaceMatJadePlugin.PLACE_MAT_INFO;
    }
}

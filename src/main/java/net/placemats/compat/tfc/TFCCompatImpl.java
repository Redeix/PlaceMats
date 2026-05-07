package net.placemats.compat.tfc;

import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.capabilities.food.IFood;
import net.dries007.tfc.common.capabilities.food.Nutrient;
import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.common.recipes.outputs.ItemStackProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

public class TFCCompatImpl implements TFCCompat {

    @Override
    public void applyTrait(ItemStack stack, ResourceLocation traitId) {
        FoodTrait trait = FoodTrait.getTrait(traitId);
        if (trait != null) {
            FoodCapability.applyTrait(stack, trait);
        }
    }

    @Override
    public void applyTrait(ItemStack stack, Object trait) {
        if (trait instanceof FoodTrait foodTrait) {
            FoodCapability.applyTrait(stack, foodTrait);
        }
    }

    @Override
    public void removeTrait(ItemStack stack, Object trait) {
        if (trait instanceof FoodTrait foodTrait) {
            FoodCapability.removeTrait(stack, foodTrait);
        }
    }

    @Override
    public Object getTrait(ResourceLocation id) {
        return FoodTrait.getTrait(id);
    }

    @Override
    public boolean isRotten(ItemStack stack) {
        return stack.getCapability(FoodCapability.CAPABILITY).map(net.dries007.tfc.common.capabilities.food.IFood::isRotten).orElse(false);
    }

    @Override
    public float getAverageTemperature(Level level, BlockPos pos) {
        return Climate.getAverageTemperature(level, pos);
    }

    @Override
    public boolean hasFoodCapability(ItemStack stack) {
        return FoodCapability.has(stack);
    }

    @Override
    public void setCraftingInput(List<ItemStack> items) {
        RecipeHelpers.setCraftingInput(new SimulatedCraftingContainer(items));
    }

    @Override
    public void clearCraftingInput() {
        RecipeHelpers.clearCraftingInput();
    }

    @Override
    public void addFoodTooltipInfo(ItemStack stack, List<Component> text) {
        IFood food = FoodCapability.get(stack);
        if (food != null) {
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
    }

    @Override
    public ItemStack getStackFromProvider(Object provider, ItemStack input) {
        if (provider instanceof ItemStackProvider stackProvider) {
            return stackProvider.getStack(input);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Object readResultFromJson(com.google.gson.JsonElement json) {
        if (json.isJsonObject()) {
            return ItemStackProvider.fromJson(json.getAsJsonObject());
        } else {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("stack", json.getAsString());
            return ItemStackProvider.fromJson(obj);
        }
    }

    @Override
    public Object readResultFromNetwork(FriendlyByteBuf buffer) {
        return ItemStackProvider.fromNetwork(buffer);
    }

    @Override
    public void writeResultToNetwork(FriendlyByteBuf buffer, Object provider) {
        if (provider instanceof ItemStackProvider stackProvider) {
            stackProvider.toNetwork(buffer);
        }
    }

    @Override
    public Block createPlaceMatBlock(Block.Properties properties, boolean cardinal) {
        return cardinal ? new TFCPlaceMatBlock.Cardinal(properties) : new TFCPlaceMatBlock(properties);
    }

    private static class SimulatedCraftingContainer implements CraftingContainer {
        private final List<ItemStack> _items = new ArrayList<>();

        public SimulatedCraftingContainer(List<ItemStack> items) {
            for (ItemStack itemStack : items) {
                if (itemStack.isEmpty()) {
                    _items.add(ItemStack.EMPTY);
                } else {
                    for (int i = 0; i < itemStack.getCount(); i++) {
                        _items.add(itemStack.copyWithCount(1));
                    }
                }
            }
        }

        @Override public int getContainerSize() { return _items.size(); }
        @Override public boolean isEmpty() { return _items.isEmpty(); }
        @Override public @NotNull ItemStack getItem(int pSlot) { return pSlot >= _items.size() ? ItemStack.EMPTY : _items.get(pSlot); }
        @Override public @NotNull ItemStack removeItem(int pSlot, int pAmount) { return pSlot >= _items.size() ? ItemStack.EMPTY : _items.get(pSlot); }
        @Override public @NotNull ItemStack removeItemNoUpdate(int pSlot) { return ItemStack.EMPTY; }
        @Override public void setItem(int pSlot, @NotNull ItemStack pStack) {}
        @Override public void setChanged() {}
        @Override public boolean stillValid(@NotNull net.minecraft.world.entity.player.Player pPlayer) { return false; }
        @Override public void clearContent() {}
        @Override public void fillStackedContents(@NotNull StackedContents pContents) {}
        @Override public int getWidth() { return 1; }
        @Override public int getHeight() { return 1; }
        @Override public @NotNull List<ItemStack> getItems() { return _items; }
    }
}

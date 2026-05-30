package net.placemats.compat.emi;

import java.util.Arrays;
import java.util.List;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.recipe.PlaceMatRecipe;
import net.placemats.compat.tfc.TFCCompat;

public class PlaceMatEmiRecipe implements EmiRecipe {
    private final PlaceMatRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public PlaceMatEmiRecipe(PlaceMatRecipe recipe) {
        this.recipe = recipe;

        ItemStack targetInputStack = recipe.getTargetInput().isEmpty() ? ItemStack.EMPTY : recipe.getTargetInput().getItems()[0].copy();
        targetInputStack.setCount(recipe.getTargetInputCount());

        java.util.List<ItemStack> resultStacks = new java.util.ArrayList<>();
        for (int i = 0; i < recipe.getResultProviders().size(); i++) {
            Object provider = recipe.getResultProviders().get(i);
            if (i == 0) {
                ItemStack resultStack = TFCCompat.INSTANCE.getStackFromProvider(provider, targetInputStack);
                if (resultStack.isEmpty()) {
                    resultStack = recipe.getResultItem(RegistryAccess.EMPTY);
                }
                resultStacks.add(resultStack);
            } else {
                resultStacks.add(((ItemStack) provider).copy());
            }
        }

        this.inputs = List.of(
                EmiIngredient.of(Arrays.stream(recipe.getInput().getItems()).map(s -> {
                    ItemStack copy = s.copy();
                    copy.setCount(recipe.getInputCount());
                    return EmiStack.of(copy);
                }).toList()),
                EmiIngredient.of(Arrays.stream(recipe.getTargetInput().getItems()).map(s -> {
                    ItemStack copy = s.copy();
                    copy.setCount(recipe.getTargetInputCount());
                    return EmiStack.of(copy);
                }).toList()));
        this.outputs = resultStacks.stream().map(EmiStack::of).toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PlaceMatEmiPlugin.PLACE_MAT;
    }

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 140;
    }

    @Override
    public int getDisplayHeight() {
        return 40;
    }

    @Override
    public void addWidgets(WidgetHolder holder) {
        int x = 15;
        int y = 12;

        // Input (Offhand).
        holder.addSlot(inputs.get(0), x, y);
        x += 24;

        // Plus.
        PlaceMatEmiPlugin.createPlusWidget(holder, y + 1, x, 13);
        x += 20;

        // Target Input (On Mat).
        holder.addSlot(inputs.get(1), x, y - 9);

        // Display restrictions if present.
        if (recipe.getBlock() != null) {
            holder.addSlot(EmiStack.of(recipe.getBlock()), x, y + 10).drawBack(false);
        } else if (recipe.getBlockTag() != null) {
            var tag = BuiltInRegistries.BLOCK.getOrCreateTag(recipe.getBlockTag());
            List<EmiStack> items = tag.stream()
                    .map(holder_ -> EmiStack.of(holder_.value().asItem()))
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!items.isEmpty()) {
                holder.addSlot(EmiIngredient.of(items), x, y + 10).drawBack(false);
            } else {
                holder.addSlot(EmiIngredient.of(PlaceMatTags.Items.PLACE_MATS), x, y + 10).drawBack(false);
            }
        } else {
            holder.addSlot(EmiIngredient.of(PlaceMatTags.Items.PLACE_MATS), x, y + 10).drawBack(false);
        }

        if (recipe.getZoneIndex() != null) {
            holder.addText(Component.translatable("place_mats.emi.placemat.zone" + ": " + recipe.getZoneIndex()), x + 10, y + 18, 0xAAAAAA, false);
        }
        x += 24;

        // Arrow.
        PlaceMatEmiPlugin.createArrowWidget(holder, y, x, 22);
        x += 30;

        // Result.
        for (EmiStack output : outputs) {
            holder.addSlot(output, x, y - 1).recipeContext(this);
            x += 18;
        }
    }
}

package net.placemats.common.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.placemats.common.data.RecipeSerializers;
import org.jetbrains.annotations.NotNull;

/**
 * Recipe for copying the dynamic color of a block to another block.
 */
public class CopyDynamicColorRecipe extends CustomRecipe {

    public CopyDynamicColorRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        ItemStack firstStack = ItemStack.EMPTY;
        ItemStack secondStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (firstStack.isEmpty()) {
                    firstStack = stack;
                } else if (secondStack.isEmpty()) {
                    secondStack = stack;
                } else {
                    return false;
                }
            }
        }

        if (firstStack.isEmpty() || secondStack.isEmpty()) {
            return false;
        }

        if (!ItemStack.isSameItem(firstStack, secondStack)) {
            return false;
        }

        boolean firstHas = IDynamicTexturedBlock.hasDynamicColors(firstStack) || IDynamicTexturedBlock.hasDynamicTextures(firstStack);
        boolean secondHas = IDynamicTexturedBlock.hasDynamicColors(secondStack) || IDynamicTexturedBlock.hasDynamicTextures(secondStack);

        return firstHas || secondHas;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        ItemStack firstStack = ItemStack.EMPTY;
        ItemStack secondStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (firstStack.isEmpty()) {
                    firstStack = stack;
                } else if (secondStack.isEmpty()) {
                    secondStack = stack;
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (firstStack.isEmpty() || secondStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!ItemStack.isSameItem(firstStack, secondStack)) {
            return ItemStack.EMPTY;
        }

        boolean firstHas = IDynamicTexturedBlock.hasDynamicColors(firstStack) || IDynamicTexturedBlock.hasDynamicTextures(firstStack);
        boolean secondHas = IDynamicTexturedBlock.hasDynamicColors(secondStack) || IDynamicTexturedBlock.hasDynamicTextures(secondStack);

        if (!firstHas && !secondHas) {
            return ItemStack.EMPTY;
        }

        // If they both have dynamic coloring, copy the first slot in the list's value
        ItemStack source = firstHas ? firstStack : secondStack;
        return source.copyWithCount(2);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.COPY_DYNAMIC_COLOR.get();
    }
}

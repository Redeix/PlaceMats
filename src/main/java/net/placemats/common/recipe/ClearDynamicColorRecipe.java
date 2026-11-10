package net.placemats.common.recipe;

import java.util.Collections;
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
 * Recipe for clearing the dynamic color of a block.
 */
public class ClearDynamicColorRecipe extends CustomRecipe {

    public ClearDynamicColorRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        ItemStack targetStack = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (!targetStack.isEmpty()) {
                    return false;
                }
                if (IDynamicTexturedBlock.hasDynamicColors(stack) || IDynamicTexturedBlock.hasDynamicTextures(stack)) {
                    targetStack = stack;
                } else {
                    return false;
                }
            }
        }
        return !targetStack.isEmpty();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container, @NotNull RegistryAccess registryAccess) {
        ItemStack targetStack = ItemStack.EMPTY;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (!targetStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                if (IDynamicTexturedBlock.hasDynamicColors(stack) || IDynamicTexturedBlock.hasDynamicTextures(stack)) {
                    targetStack = stack;
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (targetStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = targetStack.copyWithCount(1);
        IDynamicTexturedBlock.setDynamicColors(result, Collections.emptyMap());
        IDynamicTexturedBlock.setDynamicTextures(result, Collections.emptyMap());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.CLEAR_DYNAMIC_COLOR.get();
    }
}

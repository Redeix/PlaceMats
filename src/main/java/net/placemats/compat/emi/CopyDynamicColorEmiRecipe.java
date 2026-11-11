package net.placemats.compat.emi;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.placemats.PlaceMatMain;

public class CopyDynamicColorEmiRecipe implements EmiRecipe {

    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public CopyDynamicColorEmiRecipe(Item item, EmiIngredient hiddenMaterialTextures) {
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(item);
        this.id = PlaceMatMain.id("/copy_dynamic_color/" + (itemKey != null ? itemKey.getNamespace() + "/" + itemKey.getPath() : "unknown"));

        EmiStack baseStack = EmiStack.of(item);
        this.inputs = List.of(
            new DynamicTextureStack(baseStack, hiddenMaterialTextures),
            baseStack
        );

        EmiStack outputStack = new DynamicTextureStack(baseStack, hiddenMaterialTextures).setAmount(2);
        this.outputs = List.of(outputStack);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.CRAFTING;
    }

    @Override
    public ResourceLocation getId() {
        return id;
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
        return 118;
    }

    @Override
    public int getDisplayHeight() {
        Font font = Minecraft.getInstance().font;
        int maxWidth = getDisplayWidth() - 4;
        int currentY = 30;

        int lineCount = font.split(Component.translatable("place_mats.emi.crafting.copy_dynamic_color.desc"), maxWidth).size();
        currentY += (lineCount * font.lineHeight) + 3;

        return Math.max(72, currentY);
    }

    @Override
    public void addWidgets(WidgetHolder holder) {
        int slotSize = 18;
        int y = 6;
        int x = (getDisplayWidth() / 2) - (slotSize * 3);

        // Customized Input
        holder.addSlot(inputs.get(0), x, y);
        x += slotSize + 3;

        // Material Slot
        holder.addSlot(inputs.get(1), x, y);
        x += slotSize + 6;

        // Arrow
        holder.addFillingArrow(x, y + 1, Integer.MAX_VALUE);
        x += slotSize + 12;

        // Customized Output
        holder.addSlot(outputs.get(0), x, y).recipeContext(this);

        // Explanation
        Font font = Minecraft.getInstance().font;
        int maxWidth = getDisplayWidth() - 4;
        int currentY = 30;

        List<FormattedCharSequence> lines = font.split(Component.translatable("place_mats.emi.crafting.copy_dynamic_color.desc"), maxWidth);
        for (FormattedCharSequence line : lines) {
            holder.addText(line, 2, currentY, 0x555555, false);
            currentY += font.lineHeight;
        }
    }
}

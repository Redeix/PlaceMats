package net.placemats.compat.emi;

import java.util.List;
import java.util.stream.Stream;
import dev.emi.emi.api.stack.ListEmiIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraftforge.registries.tags.ITagManager;
import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatTags;

public class CopyDynamicColorEmiRecipe implements EmiRecipe {

    private static final ResourceLocation ID = PlaceMatMain.id("/copy_dynamic_color");

    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    public CopyDynamicColorEmiRecipe() {
        ITagManager<Item> itemTags = ForgeRegistries.ITEMS.tags();

        List<EmiStack> cleanBaseStacks = (itemTags != null
            ? itemTags.getTag(PlaceMatTags.Items.DYNAMIC_MATERIALS).stream()
            : Stream.<Item>empty())
            .map(EmiStack::of)
            .toList();

        List<EmiStack> rawMaterialStacks = Stream.of(
                ItemTags.LOGS,
                ItemTags.WOOL,
                Tags.Items.STONE,
                Tags.Items.GLASS
            )
            .flatMap(tag -> itemTags != null ? itemTags.getTag(tag).stream() : Stream.empty())
            .filter(item -> !item.builtInRegistryHolder().is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST))
            .distinct()
            .map(EmiStack::of)
            .toList();

        EmiIngredient dynamicBaseList = new ListEmiIngredient(cleanBaseStacks, 1);
        EmiIngredient hiddenMaterialTextures = new ListEmiIngredient(rawMaterialStacks, 1);

        this.inputs = List.of(
            new DynamicTextureStack(dynamicBaseList, hiddenMaterialTextures),
            new DynamicTextureStack(dynamicBaseList, EmiStack.EMPTY)
        );

        EmiStack outputStack = new DynamicTextureStack(dynamicBaseList, hiddenMaterialTextures).setAmount(2);
        this.outputs = List.of(outputStack);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.CRAFTING;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
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

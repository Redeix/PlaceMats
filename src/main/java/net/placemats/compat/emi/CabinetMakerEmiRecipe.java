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
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;
import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatTags;

public class CabinetMakerEmiRecipe implements EmiRecipe {

    private static final ResourceLocation ID = PlaceMatMain.id("/cabinetmaker_customization");

    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    public CabinetMakerEmiRecipe() {
        ITagManager<Item> itemTags = ForgeRegistries.ITEMS.tags();

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

        EmiIngredient materialInput = new ListEmiIngredient(rawMaterialStacks, 1);

        List<EmiStack> dynamicBaseStacks = (itemTags != null
            ? itemTags.getTag(PlaceMatTags.Items.DYNAMIC_MATERIALS).stream()
            : Stream.<Item>empty())
            .map(EmiStack::of)
            .toList();

        EmiIngredient dynamicBaseInput = new ListEmiIngredient(dynamicBaseStacks, 1);

        this.inputs = List.of(
            dynamicBaseInput,
            materialInput
        );

        this.outputs = List.of(new DynamicTextureStack(dynamicBaseInput, materialInput));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PlaceMatEmiPlugin.CABINET_MAKER;
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
        return 180;
    }

    @Override
    public int getDisplayHeight() {
        Font font = Minecraft.getInstance().font;
        int maxWidth = getDisplayWidth() - 4;
        int currentY = 30;

        String[] keys = {
            "place_mats.emi.cabinet_maker.desc1",
            "place_mats.emi.cabinet_maker.desc2",
            "place_mats.emi.cabinet_maker.desc3"
        };

        for (String key : keys) {
            int lineCount = font.split(Component.translatable(key), maxWidth).size();
            currentY += (lineCount * font.lineHeight) + 3;
        }

        return Math.max(72, currentY);
    }

    @Override
    public void addWidgets(WidgetHolder holder) {
        int slotSize = 18;
        int y = 6;
        int x = (getDisplayWidth() / 2) - (slotSize * 3);

        // Input Slot
        holder.addSlot(inputs.get(0), x, y);
        x += slotSize + 3;

        // Material Slot
        holder.addSlot(inputs.get(1), x, y);
        x += slotSize + 6;

        // Arrow
        holder.addFillingArrow(x, y + 1, Integer.MAX_VALUE);
        x += slotSize + 12;

        // Output Slot
        holder.addSlot(outputs.get(0), x, y).recipeContext(this);

        // Explanations
        Font font = Minecraft.getInstance().font;
        int maxWidth = getDisplayWidth() - 4;
        int currentY = 30;

        String[] keys = {
            "place_mats.emi.cabinet_maker.desc1",
            "place_mats.emi.cabinet_maker.desc2",
            "place_mats.emi.cabinet_maker.desc3"
        };

        for (String key : keys) {
            List<FormattedCharSequence> lines = font.split(Component.translatable(key), maxWidth);
            for (FormattedCharSequence line : lines) {
                holder.addText(line, 2, currentY, 0x555555, false);
                currentY += font.lineHeight;
            }
            currentY += 3;
        }
    }
}

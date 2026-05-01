package net.placemats.compat.emi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextureWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatRecipeTypes;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.blocks.PlaceMatBlocks;
import net.placemats.common.recipe.PlaceMatRecipe;

@EmiEntrypoint
public class PlaceMatEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory PLACE_MAT = new EmiRecipeCategory(PlaceMatMain.id("place_mat"),
            EmiStack.of(PlaceMatBlocks.STORAGE_RACK.get()));

    @Override
    public void register(EmiRegistry emiRegistry) {

        // Place Mat
        emiRegistry.addCategory(PLACE_MAT);
        TagKey<Item> placeMats = PlaceMatTags.Items.PLACE_MATS;
        EmiIngredient workstationIngredient = EmiIngredient.of(placeMats);
        emiRegistry.addWorkstation(PLACE_MAT, workstationIngredient);
        for (PlaceMatRecipe recipe : emiRegistry.getRecipeManager().getAllRecipesFor(PlaceMatRecipeTypes.PLACE_MAT.get())) {
            emiRegistry.addRecipe(new PlaceMatEmiRecipe(recipe));
        }
    }

    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,
            "textures/gui/emi/emi_screen_arrow.png");
    private static final ResourceLocation PLUS = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,
            "textures/gui/emi/emi_screen_plus.png");

    public static int createArrowWidget(WidgetHolder holder, int offsetY, int offsetX, int length) {
        int image_height = 15;
        int image_width = 22;
        int u_start = image_width - length;

        TextureWidget widget = new TextureWidget(ARROW, offsetX, offsetY, length, image_height, u_start, 0, length, image_height - 1, image_width, image_height);
        holder.add(widget);
        return offsetX + 2 + length;
    }

    public static void createPlusWidget(WidgetHolder holder, int offsetY, int offsetX, int length) {
        int image_height = 13;
        int image_width = 13;
        int u_start = image_width - length;

        TextureWidget widget = new TextureWidget(PLUS, offsetX, offsetY, length, image_height, u_start, 0, length, image_height - 1, image_width, image_height);
        holder.add(widget);
    }

    public static void createItemWidget(WidgetHolder holder, int offsetY, int offsetX, EmiIngredient stack) {
        SlotWidget widget = new SlotWidget(stack, offsetX, offsetY);
        holder.add(widget);
    }
}

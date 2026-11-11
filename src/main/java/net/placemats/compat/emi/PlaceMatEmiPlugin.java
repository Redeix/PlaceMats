package net.placemats.compat.emi;

import dev.emi.emi.config.EmiConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextureWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.placemats.PlaceMatMain;
import net.placemats.client.gui.CabinetMakerTableScreen;
import net.placemats.client.gui.util.ColorPickerWidget;
import net.placemats.common.data.PlaceMatRecipeTypes;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.PlaceMatBlocks;
import net.placemats.common.recipe.PlaceMatRecipe;

// TODO: Remake EMI compat.
@SuppressWarnings("unused")
@EmiEntrypoint
public class PlaceMatEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory PLACE_MAT = new EmiRecipeCategory(PlaceMatMain.id("place_mat"),
            EmiStack.of(PlaceMatBlocks.STORAGE_RACK.get()));
    public static final EmiRecipeCategory CABINET_MAKER = new EmiRecipeCategory(PlaceMatMain.id("cabinetmaker_table"),
            EmiStack.of(PlaceMatBlocks.CABINETMAKER_TABLE.get()));

    @Override
    public void register(EmiRegistry emiRegistry) {

        // Place Mat Interaction
        emiRegistry.addCategory(PLACE_MAT);
        TagKey<Item> placeMats = PlaceMatTags.Items.PLACE_MATS;
        EmiIngredient workstationIngredient = EmiIngredient.of(placeMats);
        emiRegistry.addWorkstation(PLACE_MAT, workstationIngredient);
        for (PlaceMatRecipe recipe : emiRegistry.getRecipeManager().getAllRecipesFor(PlaceMatRecipeTypes.PLACE_MAT.get())) {
            emiRegistry.addRecipe(new PlaceMatEmiRecipe(recipe));
        }

        // Cabinetmaker's Table Customization
        emiRegistry.addCategory(CABINET_MAKER);
        emiRegistry.addWorkstation(CABINET_MAKER, EmiStack.of(PlaceMatBlocks.CABINETMAKER_TABLE.get()));
        emiRegistry.addRecipe(new CabinetMakerEmiRecipe());

        // Special Crafting Recipes (Clear & Copy Customizations)
        ITagManager<Item> itemTags = ForgeRegistries.ITEMS.tags();
        if (itemTags != null) {
            EmiIngredient hiddenMaterialTextures = ClearDynamicColorEmiRecipe.createMaterialIngredient();
            for (Item item : itemTags.getTag(PlaceMatTags.Items.DYNAMIC_MATERIALS)) {
                emiRegistry.addRecipe(new ClearDynamicColorEmiRecipe(item, hiddenMaterialTextures));
                emiRegistry.addRecipe(new CopyDynamicColorEmiRecipe(item, hiddenMaterialTextures));
            }
        }

        emiRegistry.addDragDropHandler(CabinetMakerTableScreen.class, new EmiDragDropHandler<>()
        {
            @Override
            public boolean dropStack(CabinetMakerTableScreen screen, EmiIngredient stack, int x, int y)
            {
                int slotIndex = screen.getMaterialSlotIndexAt(x, y);
                if (slotIndex == -1)
                {
                    return false;
                }
                for (EmiStack emiStack : stack.getEmiStacks())
                {
                    ItemStack itemStack = emiStack.getItemStack();
                    if (itemStack != null && !itemStack.isEmpty())
                    {
                        return screen.setGhostMaterial(slotIndex, itemStack);
                    }
                }
                return false;
            }

            @Override
            public void render(CabinetMakerTableScreen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta)
            {
                screen.renderGhostDropTargets(draw, mouseX, mouseY);
            }
        });

        registerExclusionZones(emiRegistry);
    }

    private void registerExclusionZones(EmiRegistry registry) {
        registry.addExclusionArea(CabinetMakerTableScreen.class, (screen, consumer) -> {
            ColorPickerWidget widget = screen.getColorPickerWidget();
            if (widget != null && widget.visible) {
                consumer.accept(new Bounds(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
            }
        });
    }

    private static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,
            "textures/gui/emi/emi_screen_arrow.png");
    private static final ResourceLocation PLUS = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID,
            "textures/gui/emi/emi_screen_plus.png");

    public static void createArrowWidget(WidgetHolder holder, int offsetY, int offsetX, int length) {
        int image_height = 15;
        int image_width = 22;
        int u_start = image_width - length;

        TextureWidget widget = new TextureWidget(ARROW, offsetX, offsetY, length, image_height, u_start, 0, length, image_height - 1, image_width, image_height);
        holder.add(widget);
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

    /**
     * Gets the key name for the EMI view uses button. (Default "U")
     */
    public static String getUsesKeyName() {
        Component keyName = EmiConfig.viewUses.isBound()
            ? EmiConfig.viewUses.boundKeys.get(0).key().getDisplayName()
            : EmiConfig.viewUses.defaultKeys.get(0).key().getDisplayName();

        return keyName.getString();
    }

    /**
     * Gets the key name for the EMI view recipes button. (Default "R")
     */
    public static String getRecipesKeyName() {
        Component keyName =  EmiConfig.viewRecipes.isBound()
            ? EmiConfig.viewRecipes.boundKeys.get(0).key().getDisplayName()
            : EmiConfig.viewRecipes.defaultKeys.get(0).key().getDisplayName();

        return keyName.getString();
    }
}

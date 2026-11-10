package net.placemats.common.data;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.placemats.common.recipe.ClearDynamicColorRecipe;
import net.placemats.common.recipe.CopyDynamicColorRecipe;
import net.placemats.common.recipe.PlaceMatRecipe;

public class RecipeSerializers {

    public static void init() {
    }

    public static final RegistryEntry<PlaceMatRecipe.Serializer> PLACE_MAT = PlaceMatRegistries.REGISTRATE
            .generic("place_mat", Registries.RECIPE_SERIALIZER, PlaceMatRecipe.Serializer::new)
            .register();

    public static final RegistryEntry<SimpleCraftingRecipeSerializer<ClearDynamicColorRecipe>> CLEAR_DYNAMIC_COLOR = PlaceMatRegistries.REGISTRATE
            .generic("crafting_special_cleardynamiccolor", Registries.RECIPE_SERIALIZER, () -> new SimpleCraftingRecipeSerializer<>(ClearDynamicColorRecipe::new))
            .register();

    public static final RegistryEntry<SimpleCraftingRecipeSerializer<CopyDynamicColorRecipe>> COPY_DYNAMIC_COLOR = PlaceMatRegistries.REGISTRATE
            .generic("crafting_special_copydynamiccolor", Registries.RECIPE_SERIALIZER, () -> new SimpleCraftingRecipeSerializer<>(CopyDynamicColorRecipe::new))
            .register();
}

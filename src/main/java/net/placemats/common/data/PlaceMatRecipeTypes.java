package net.placemats.common.data;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.placemats.common.recipe.PlaceMatRecipe;

public class PlaceMatRecipeTypes {

    public static void init() {
    }

    public static final RegistryEntry<RecipeType<PlaceMatRecipe>> PLACE_MAT = PlaceMatRegistries.REGISTRATE
            .generic("place_mat", Registries.RECIPE_TYPE, () -> (RecipeType<PlaceMatRecipe>) new RecipeType<PlaceMatRecipe>() {
                @Override
                public String toString() {
                    return "place_mat";
                }
            })
            .register();

}

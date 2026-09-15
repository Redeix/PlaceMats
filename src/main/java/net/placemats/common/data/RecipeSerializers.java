package net.placemats.common.data;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.registries.Registries;
import net.placemats.common.recipe.PlaceMatRecipe;

public class RecipeSerializers {

    public static void init() {
    }

    public static final RegistryEntry<PlaceMatRecipe.Serializer> PLACE_MAT = PlaceMatRegistries.REGISTRATE
            .generic("place_mat", Registries.RECIPE_SERIALIZER, PlaceMatRecipe.Serializer::new)
            .register();
}

package net.placemats.compat.kjs;

import net.minecraft.resources.ResourceLocation;

import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

public interface PlaceMatRecipeSchema {
    RecipeKey<String> ID = StringComponent.ID.key("id").defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(
            PlaceMatRecipeJS.class,
            PlaceMatRecipeJS::new,
            ID)
            .constructor((recipe, schemaType, keys, from) -> recipe.id(ResourceLocation.parse(from.getValue(recipe, ID))), ID)
            .constructor((recipe, schemaType, keys, from) -> {
            });
}

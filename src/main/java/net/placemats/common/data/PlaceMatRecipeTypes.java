package net.placemats.common.data;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.placemats.PlaceMatMain;
import net.placemats.common.recipe.PlaceMatRecipe;

public class PlaceMatRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister
            .create(ForgeRegistries.RECIPE_TYPES, PlaceMatMain.MOD_ID);

    public static final RegistryObject<RecipeType<PlaceMatRecipe>> PLACE_MAT = register("place_mat");

    private static <R extends Recipe<?>> RegistryObject<RecipeType<R>> register(String name) {
        return RECIPE_TYPES.register(name, () -> new RecipeType<R>() {
            @Override
            public String toString() {
                return name;
            }
        });
    }

}

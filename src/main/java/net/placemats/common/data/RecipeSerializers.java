package net.placemats.common.data;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.placemats.PlaceMatMain;
import net.placemats.common.recipe.PlaceMatRecipe;

public class RecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, PlaceMatMain.MOD_ID);

    public static final RegistryObject<PlaceMatRecipe.Serializer> PLACE_MAT = RECIPE_SERIALIZERS.register("place_mat", PlaceMatRecipe.Serializer::new);
}

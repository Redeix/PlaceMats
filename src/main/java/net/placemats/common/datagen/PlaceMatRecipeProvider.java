package net.placemats.common.datagen;

import java.util.function.Consumer;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import net.placemats.common.data.blocks.PlaceMatBlocks;
import org.jetbrains.annotations.NotNull;

public class PlaceMatRecipeProvider extends RecipeProvider {
    public PlaceMatRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlaceMatBlocks.STORAGE_RACK_ITEM.get())
                .pattern("SSS")
                .pattern("I I")
                .pattern("SSS")
                .define('S', Items.SMOOTH_STONE_SLAB)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_ingot", has(Tags.Items.INGOTS_IRON))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlaceMatBlocks.OAK_STORAGE_RACK_ITEM.get())
                .pattern("SSS")
                .pattern("I I")
                .pattern("SSS")
                .define('S', Items.OAK_SLAB)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_ingot", has(Tags.Items.INGOTS_IRON))
                .save(consumer);
    }
}

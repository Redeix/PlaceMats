package net.placemats.common.datagen;

import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.common.crafting.conditions.TrueCondition;
import net.minecraftforge.registries.ForgeRegistries;

import net.placemats.common.data.PlaceMatBlocks;
import org.jetbrains.annotations.NotNull;

public class PlaceMatRecipeProvider extends RecipeProvider {
    public PlaceMatRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    private static Item getSlab(String woodName) {
        Item slab = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", woodName + "_slab"));
        if (slab != null && slab != Items.AIR) {
            return slab;
        }
        return null;
    }
    private static Item getStairs(String woodName) {
        Item stairs = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", woodName + "_stairs"));
        if (stairs != null && stairs != Items.AIR) {
            return stairs;
        }
        return null;
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> consumer) {

        ResourceLocation tfcIronBars = ResourceLocation.fromNamespaceAndPath("tfc", "metal/bars/wrought_iron");
        Ingredient tfcIronBarsIngredient = Ingredient.of(new ItemStack(
                ForgeRegistries.ITEMS.getValue(tfcIronBars) == Items.AIR
                        ? Items.IRON_BARS : Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(tfcIronBars))
        ));

        ResourceLocation tfcAlabaster = ResourceLocation.fromNamespaceAndPath("tfc", "alabaster/polished/light_gray_slab");
        Ingredient tfcAlabasterIngredient = Ingredient.of(new ItemStack(
                ForgeRegistries.ITEMS.getValue(tfcAlabaster) == Items.AIR
                        ? Items.SMOOTH_STONE_SLAB : Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(tfcAlabaster))
        ));


        ConditionalRecipe.builder()
                // TFC version.
                .addCondition(new ModLoadedCondition("tfc"))
                .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlaceMatBlocks.STORAGE_RACK.get())
                        .pattern("SSS")
                        .pattern("I I")
                        .pattern("ISI")
                        .define('S', tfcAlabasterIngredient)
                        .define('I', tfcIronBarsIngredient)
                        .unlockedBy("always", PlayerTrigger.TriggerInstance.tick())
                        .save(consumer1))
                // Regular version.
                .addCondition(TrueCondition.INSTANCE)
                .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlaceMatBlocks.STORAGE_RACK.get())
                        .pattern("SSS")
                        .pattern("I I")
                        .pattern("ISI")
                        .define('S', Items.SMOOTH_STONE_SLAB)
                        .define('I', Blocks.IRON_BARS)
                        .unlockedBy("has_iron_bars", has(Blocks.IRON_BARS))
                        .save(consumer1))
                .generateAdvancement()
                .build(consumer, PlaceMatBlocks.STORAGE_RACK.getId());

        PlaceMatBlocks.WOOD_STORAGE_RACKS.forEach(blockReg -> {
            String woodName = blockReg.getId().getPath().replace("_storage_rack", "");
            var slab = getSlab(woodName);

            if (slab != null) {
                ConditionalRecipe.builder()
                        // TFC version.
                        .addCondition(new ModLoadedCondition("tfc"))
                        .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get())
                                .pattern("SSS")
                                .pattern("I I")
                                .pattern("ISI")
                                .define('S', slab)
                                .define('I', tfcIronBarsIngredient)
                                .unlockedBy("always", PlayerTrigger.TriggerInstance.tick())
                                .save(consumer1))
                        // Regular version.
                        .addCondition(TrueCondition.INSTANCE)
                        .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get())
                                .pattern("SSS")
                                .pattern("I I")
                                .pattern("ISI")
                                .define('S', slab)
                                .define('I', Blocks.IRON_BARS)
                                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                                .save(consumer1))
                        .generateAdvancement()
                        .build(consumer, blockReg.getId());
            }
        });

        PlaceMatBlocks.WOOD_ORNATE_SHELVES.forEach(blockReg -> {
            String woodName = blockReg.getId().getPath().replace("_ornate_shelf", "");
            var slab = getSlab(woodName);

            if (slab != null) {
                ConditionalRecipe.builder()
                    // TFC version.
                    .addCondition(new ModLoadedCondition("tfc"))
                    .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get(),2)
                        .pattern("   ")
                        .pattern("SSS")
                        .pattern(" II")
                        .define('S', slab)
                        .define('I', tfcIronBarsIngredient)
                        .unlockedBy("always", PlayerTrigger.TriggerInstance.tick())
                        .save(consumer1))
                    // Regular version.
                    .addCondition(TrueCondition.INSTANCE)
                    .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get(), 2)
                        .pattern("   ")
                        .pattern("SSS")
                        .pattern(" II")
                        .define('S', slab)
                        .define('I', Blocks.IRON_BARS)
                        .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                        .save(consumer1))
                    .generateAdvancement()
                    .build(consumer, blockReg.getId());
            }
        });

        PlaceMatBlocks.WOOD_FLOATING_SHELVES.forEach(blockReg -> {
            String woodName = blockReg.getId().getPath().replace("_floating_shelf", "");
            var slab = getSlab(woodName);
            var stairs = getStairs(woodName);

            if (slab != null && stairs != null) {
                ConditionalRecipe.builder()
                    .addCondition(TrueCondition.INSTANCE)
                    .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get(), 2)
                        .pattern("   ")
                        .pattern("SSS")
                        .pattern("  I")
                        .define('S', slab)
                        .define('I', stairs)
                        .unlockedBy(getHasName(slab), has(slab))
                        .unlockedBy(getHasName(stairs), has(stairs))
                        .save(consumer1))
                    .generateAdvancement()
                    .build(consumer, blockReg.getId());
            }
        });

        PlaceMatBlocks.WOOD_ORNATE_DOUBLE_SHELVES.forEach(blockReg -> {
            String woodName = blockReg.getId().getPath().replace("_ornate_double_shelf", "");
            var slab = getSlab(woodName);

            if (slab != null) {
                ConditionalRecipe.builder()
                    // TFC version.
                    .addCondition(new ModLoadedCondition("tfc"))
                    .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get(),2)
                        .pattern(" SS")
                        .pattern("  I")
                        .pattern(" SS")
                        .define('S', slab)
                        .define('I', tfcIronBarsIngredient)
                        .unlockedBy("always", PlayerTrigger.TriggerInstance.tick())
                        .save(consumer1))
                    // Regular version.
                    .addCondition(TrueCondition.INSTANCE)
                    .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, blockReg.get(), 2)
                        .pattern(" SS")
                        .pattern("  I")
                        .pattern(" SS")
                        .define('S', slab)
                        .define('I', Blocks.IRON_BARS)
                        .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                        .save(consumer1))
                    .generateAdvancement()
                    .build(consumer, blockReg.getId());
            }
        });
    }
}

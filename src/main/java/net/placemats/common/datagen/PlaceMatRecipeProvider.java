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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.common.crafting.conditions.TrueCondition;
import net.minecraftforge.registries.ForgeRegistries;

import net.placemats.common.data.blocks.PlaceMatBlocks;
import org.jetbrains.annotations.NotNull;

public class PlaceMatRecipeProvider extends RecipeProvider {
    public PlaceMatRecipeProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> consumer) {

        ResourceLocation tfcIronBars = ResourceLocation.fromNamespaceAndPath("tfc", "metal/bars/wrought_iron");
        Ingredient tfcIronBarsIngredient = Ingredient.of(new net.minecraft.world.item.ItemStack(
                ForgeRegistries.ITEMS.getValue(tfcIronBars) == Items.AIR
                        ? Items.IRON_BARS : Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(tfcIronBars))
        ));

        ResourceLocation tfcAlabaster = ResourceLocation.fromNamespaceAndPath("tfc", "alabaster/polished/light_gray_slab");
        Ingredient tfcAlabasterIngredient = Ingredient.of(new net.minecraft.world.item.ItemStack(
                ForgeRegistries.ITEMS.getValue(tfcAlabaster) == Items.AIR
                        ? Items.SMOOTH_STONE_SLAB : Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(tfcAlabaster))
        ));

        assert PlaceMatBlocks.STORAGE_RACK_ITEM.getId() != null;
        ConditionalRecipe.builder()
                // TFC version.
                .addCondition(new ModLoadedCondition("tfc"))
                .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlaceMatBlocks.STORAGE_RACK_ITEM.get())
                        .pattern("SSS")
                        .pattern("I I")
                        .pattern("ISI")
                        .define('S', tfcAlabasterIngredient)
                        .define('I', tfcIronBarsIngredient)
                        .unlockedBy("always", PlayerTrigger.TriggerInstance.tick())
                        .save(consumer1))
                // Regular version.
                .addCondition(TrueCondition.INSTANCE)
                .addRecipe(consumer1 -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlaceMatBlocks.STORAGE_RACK_ITEM.get())
                        .pattern("SSS")
                        .pattern("I I")
                        .pattern("ISI")
                        .define('S', Items.SMOOTH_STONE_SLAB)
                        .define('I', Blocks.IRON_BARS)
                        .unlockedBy("has_iron_bars", has(Blocks.IRON_BARS))
                        .save(consumer1))
                .generateAdvancement()
                .build(consumer, PlaceMatBlocks.STORAGE_RACK_ITEM.getId());

        PlaceMatBlocks.WOOD_STORAGE_RACKS.forEach(blockReg -> {
            assert blockReg.getId() != null;
            String woodName = blockReg.getId().getPath().replace("_storage_rack", "");
            Item slab = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", woodName + "_slab"));

            if (slab != null && slab != Items.AIR) {
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
    }
}

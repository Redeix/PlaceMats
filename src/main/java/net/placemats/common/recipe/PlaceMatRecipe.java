package net.placemats.common.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.placemats.common.data.RecipeSerializers;
import net.placemats.common.data.PlaceMatRecipeTypes;
import net.placemats.compat.tfc.TFCCompat;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import lombok.Getter;

public class PlaceMatRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    @Nullable
    private final Block block;
    @Nullable
    private final TagKey<Block> blockTag;
    @Nullable
    private final Integer zoneIndex;
    @Getter
    private final Ingredient input;
    @Getter
    private final int inputCount;
    @Getter
    private final Ingredient targetInput;
    @Getter
    private final int targetInputCount;
    private final java.util.List<Object> results;
    @Nullable
    private final ResourceLocation sound;
    @Getter
    private final float volume;
    @Getter
    private final float pitch;

    public PlaceMatRecipe(ResourceLocation id, @Nullable Block block, @Nullable TagKey<Block> blockTag, @Nullable Integer zoneIndex,
            Ingredient input, int inputCount, Ingredient targetInput, int targetInputCount,
            java.util.List<Object> results, @Nullable ResourceLocation sound, float volume, float pitch) {
        this.id = id;
        this.block = block;
        this.blockTag = blockTag;
        this.zoneIndex = zoneIndex;
        this.input = input;
        this.inputCount = inputCount;
        this.targetInput = targetInput;
        this.targetInputCount = targetInputCount;
        this.results = results;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public @Nullable Block getBlock() {
        return block;
    }

    public @Nullable TagKey<Block> getBlockTag() {
        return blockTag;
    }

    public @Nullable Integer getZoneIndex() {
        return zoneIndex;
    }

    public java.util.List<Object> getResultProviders() {
        return results;
    }

    public @Nullable ResourceLocation getSound() {
        return sound;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        if (results.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return TFCCompat.INSTANCE.getStackFromProvider(results.get(0), ItemStack.EMPTY);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return RecipeSerializers.PLACE_MAT.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return PlaceMatRecipeTypes.PLACE_MAT.get();
    }

    public static class Serializer implements RecipeSerializer<PlaceMatRecipe> {
        @Override
        public @NotNull PlaceMatRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Block block = null;
            TagKey<Block> blockTag = null;
            if (json.has("block")) {
                String s = GsonHelper.getAsString(json, "block");
                if (s.startsWith("#")) {
                    blockTag = TagKey.create(Registries.BLOCK, ResourceLocation.parse(s.substring(1)));
                } else {
                    block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
                }
            }
            Integer zoneIndex = json.has("zone_index") ? GsonHelper.getAsInt(json, "zone_index") : null;
            Ingredient input = json.has("input") ? Ingredient.fromJson(json.get("input")) : Ingredient.EMPTY;
            int inputCount = json.has("input_count") ? GsonHelper.getAsInt(json, "input_count") : 1;
            Ingredient targetInput = json.has("target_input") ? Ingredient.fromJson(json.get("target_input")) : Ingredient.EMPTY;
            int targetInputCount = json.has("target_input_count") ? GsonHelper.getAsInt(json, "target_input_count") : 1;

            java.util.List<Object> results = new java.util.ArrayList<>();
            JsonElement resultElement = json.get("result");
            if (resultElement != null && resultElement.isJsonArray()) {
                com.google.gson.JsonArray array = resultElement.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement e = array.get(i);
                    if (i == 0) {
                        results.add(TFCCompat.INSTANCE.readResultFromJson(e));
                    } else {
                        if (e.isJsonObject()) {
                            results.add(net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(e.getAsJsonObject()));
                        } else {
                            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getAsString()));
                            results.add(new net.minecraft.world.item.ItemStack(item));
                        }
                    }
                }
            } else {
                results.add(TFCCompat.INSTANCE.readResultFromJson(resultElement));
            }

            ResourceLocation sound = json.has("sound") ? ResourceLocation.parse(GsonHelper.getAsString(json, "sound")) : null;
            float volume = GsonHelper.getAsFloat(json, "volume", 1.0f);
            float pitch = GsonHelper.getAsFloat(json, "pitch", 1.0f);

            return new PlaceMatRecipe(recipeId, block, blockTag, zoneIndex, input, inputCount, targetInput, targetInputCount, results, sound, volume, pitch);
        }

        @Override
        public @Nullable PlaceMatRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            Block block = null;
            TagKey<Block> blockTag = null;
            byte type = buffer.readByte();
            if (type == 1) {
                block = BuiltInRegistries.BLOCK.get(buffer.readResourceLocation());
            } else if (type == 2) {
                blockTag = TagKey.create(Registries.BLOCK, buffer.readResourceLocation());
            }
            Integer zoneIndex = buffer.readBoolean() ? buffer.readInt() : null;
            Ingredient input = Ingredient.fromNetwork(buffer);
            int inputCount = buffer.readVarInt();
            Ingredient targetInput = Ingredient.fromNetwork(buffer);
            int targetInputCount = buffer.readVarInt();

            int resultSize = buffer.readVarInt();
            java.util.List<Object> results = new java.util.ArrayList<>(resultSize);
            for (int i = 0; i < resultSize; i++) {
                if (i == 0) {
                    results.add(TFCCompat.INSTANCE.readResultFromNetwork(buffer));
                } else {
                    results.add(buffer.readItem());
                }
            }

            ResourceLocation sound = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            float volume = buffer.readFloat();
            float pitch = buffer.readFloat();

            return new PlaceMatRecipe(recipeId, block, blockTag, zoneIndex, input, inputCount, targetInput, targetInputCount, results, sound, volume, pitch);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull PlaceMatRecipe recipe) {
            if (recipe.block != null) {
                buffer.writeByte(1);
                buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(recipe.block));
            } else if (recipe.blockTag != null) {
                buffer.writeByte(2);
                buffer.writeResourceLocation(recipe.blockTag.location());
            } else {
                buffer.writeByte(0);
            }
            buffer.writeBoolean(recipe.zoneIndex != null);
            if (recipe.zoneIndex != null)
                buffer.writeInt(recipe.zoneIndex);
            recipe.input.toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount);
            recipe.targetInput.toNetwork(buffer);
            buffer.writeVarInt(recipe.targetInputCount);

            buffer.writeVarInt(recipe.results.size());
            for (int i = 0; i < recipe.results.size(); i++) {
                Object res = recipe.results.get(i);
                if (i == 0) {
                    TFCCompat.INSTANCE.writeResultToNetwork(buffer, res);
                } else {
                    buffer.writeItem((ItemStack) res);
                }
            }

            buffer.writeBoolean(recipe.sound != null);
            if (recipe.sound != null)
                buffer.writeResourceLocation(recipe.sound);
            buffer.writeFloat(recipe.volume);
            buffer.writeFloat(recipe.pitch);
        }
    }
}

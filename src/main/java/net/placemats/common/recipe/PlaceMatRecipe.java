package net.placemats.common.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.dries007.tfc.common.recipes.RecipeSerializerImpl;
import net.dries007.tfc.common.recipes.outputs.ItemStackProvider;
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

import net.placemats.common.data.RecipeSerializers;
import net.placemats.common.data.PlaceMatRecipeTypes;

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
    private final ItemStackProvider result;
    @Nullable
    private final ResourceLocation sound;
    @Getter
    private final float volume;
    @Getter
    private final float pitch;

    public PlaceMatRecipe(ResourceLocation id, @Nullable Block block, @Nullable TagKey<Block> blockTag, @Nullable Integer zoneIndex,
            Ingredient input, int inputCount, Ingredient targetInput, int targetInputCount,
            ItemStackProvider result, @Nullable ResourceLocation sound, float volume, float pitch) {
        this.id = id;
        this.block = block;
        this.blockTag = blockTag;
        this.zoneIndex = zoneIndex;
        this.input = input;
        this.inputCount = inputCount;
        this.targetInput = targetInput;
        this.targetInputCount = targetInputCount;
        this.result = result;
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

    public ItemStackProvider getResultProvider() {
        return result;
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
        return result.getStack(ItemStack.EMPTY);
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

    public static class Serializer extends RecipeSerializerImpl<PlaceMatRecipe> {
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

            JsonElement resultElement = json.get("result");
            ItemStackProvider result;
            if (resultElement.isJsonObject()) {
                result = ItemStackProvider.fromJson(resultElement.getAsJsonObject());
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("stack", resultElement.getAsString());
                result = ItemStackProvider.fromJson(obj);
            }

            ResourceLocation sound = json.has("sound") ? ResourceLocation.parse(GsonHelper.getAsString(json, "sound")) : null;
            float volume = GsonHelper.getAsFloat(json, "volume", 1.0f);
            float pitch = GsonHelper.getAsFloat(json, "pitch", 1.0f);

            return new PlaceMatRecipe(recipeId, block, blockTag, zoneIndex, input, inputCount, targetInput, targetInputCount, result, sound, volume, pitch);
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
            ItemStackProvider result = ItemStackProvider.fromNetwork(buffer);
            ResourceLocation sound = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            float volume = buffer.readFloat();
            float pitch = buffer.readFloat();

            return new PlaceMatRecipe(recipeId, block, blockTag, zoneIndex, input, inputCount, targetInput, targetInputCount, result, sound, volume, pitch);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, PlaceMatRecipe recipe) {
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
            recipe.result.toNetwork(buffer);
            buffer.writeBoolean(recipe.sound != null);
            if (recipe.sound != null)
                buffer.writeResourceLocation(recipe.sound);
            buffer.writeFloat(recipe.volume);
            buffer.writeFloat(recipe.pitch);
        }
    }
}

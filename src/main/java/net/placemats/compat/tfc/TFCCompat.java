package net.placemats.compat.tfc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.placemats.compat.ModCompat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import com.google.gson.JsonElement;

public interface TFCCompat {
    TFCCompat INSTANCE = ModCompat.TFC_LOADED ? new TFCCompatImpl() : new NoTFCCompat();

    void applyTrait(ItemStack stack, ResourceLocation traitId);
    void applyTrait(ItemStack stack, Object trait);
    void removeTrait(ItemStack stack, Object trait);
    Object getTrait(ResourceLocation id);
    boolean isRotten(ItemStack stack);
    float getAverageTemperature(Level level, BlockPos pos);
    boolean hasFoodCapability(ItemStack stack);
    void setCraftingInput(List<ItemStack> items);
    void clearCraftingInput();
    void addFoodTooltipInfo(ItemStack stack, List<Component> text);
    ItemStack getStackFromProvider(Object provider, ItemStack input);
    Object readResultFromJson(JsonElement json);
    Object readResultFromNetwork(FriendlyByteBuf buffer);
    void writeResultToNetwork(FriendlyByteBuf buffer, Object provider);
    Block createPlaceMatBlock(Block.Properties properties, boolean cardinal);
}

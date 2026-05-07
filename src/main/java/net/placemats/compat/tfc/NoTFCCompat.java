package net.placemats.compat.tfc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import com.google.gson.JsonElement;
import net.placemats.common.block.PlaceMatBlock;

public class NoTFCCompat implements TFCCompat {
    @Override public void applyTrait(ItemStack stack, ResourceLocation traitId) {}
    @Override public void applyTrait(ItemStack stack, Object trait) {}
    @Override public void removeTrait(ItemStack stack, Object trait) {}
    @Override public Object getTrait(ResourceLocation id) { return null; }
    @Override public boolean isRotten(ItemStack stack) { return false; }
    @Override public float getAverageTemperature(Level level, BlockPos pos) { return 0; }
    @Override public boolean hasFoodCapability(ItemStack stack) { return false; }
    @Override public void setCraftingInput(List<ItemStack> items) {}
    @Override public void clearCraftingInput() {}
    @Override public void addFoodTooltipInfo(ItemStack stack, List<Component> text) {}
    @Override public ItemStack getStackFromProvider(Object provider, ItemStack input) { return ItemStack.EMPTY; }
    @Override public Object readResultFromJson(JsonElement json) { return null; }
    @Override public Object readResultFromNetwork(FriendlyByteBuf buffer) { return null; }
    @Override public void writeResultToNetwork(FriendlyByteBuf buffer, Object provider) {}
    @Override public Block createPlaceMatBlock(Block.Properties properties, boolean cardinal) {
        return cardinal ? new PlaceMatBlock.Cardinal(properties) : new PlaceMatBlock(properties);
    }
}

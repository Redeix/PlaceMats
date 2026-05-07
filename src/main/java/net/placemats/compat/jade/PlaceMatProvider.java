/*
 * This file includes code from TFC (https://github.com/TerraFirmaCraft/TerraFirmaCraft?tab=EUPL-1.2-1-ov-file)
 * EUPL © the European Union 2007, 2016
 * European Union Public Licence
 * V. 1.2
 */
package net.placemats.compat.jade;

import java.util.ArrayList;
import java.util.List;

import net.placemats.compat.tfc.TFCCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.blockentity.PlaceMatBlockEntity;

/**
 * Provides tooltip information for Place Mat block entities.
 */
public enum PlaceMatProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof PlaceMatBlockEntity foodPlacer) {
            IElementHelper elementHelper = tooltip.getElementHelper();
            Vec3 hitVec = accessor.getHitResult().getLocation().subtract(accessor.getPosition().getX(), accessor.getPosition().getY(), accessor.getPosition().getZ());
            Vec3 eyePos = accessor.getPlayer().getEyePosition(1.0f);
            Vec3 lookVec = accessor.getPlayer().getViewVector(1.0f);

            PlaceMatBlock.PlacementRange targetedRange = null;
            if (foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                targetedRange = pmb.getTargetedPlacementRange(foodPlacer.getBlockState(), hitVec);
            }

            PlaceMatBlockEntity.PlacedItem targetedItem = foodPlacer.getTargetedItem(eyePos, lookVec, accessor.getPosition(), targetedRange);
            boolean containsItems = !foodPlacer.getPlacedItems().isEmpty();

            for (PlaceMatBlockEntity.PlacedItem placed : foodPlacer.getPlacedItems()) {
                // If we are looking at a specific box, only show items in that box.
                if (targetedRange != null) {
                    if (placed.baseHeight < targetedRange.box().minY - 0.001 || placed.baseHeight > targetedRange.box().maxY + 0.001) {
                        continue;
                    }
                }

                // Single item view by default.
                if (placed == targetedItem && !Screen.hasAltDown()) {
                    ItemStack stack = placed.stack;
                    tooltip.add(elementHelper.item(stack));
                    MutableComponent name = stack.getHoverName().copy();

                    name.withStyle(ChatFormatting.GOLD);
                    tooltip.append(name);

                    List<Component> foodInfo = new ArrayList<>();
                    TFCCompat.INSTANCE.addFoodTooltipInfo(stack, foodInfo);
                    for (Component component : foodInfo) {
                        tooltip.add(component);
                    }
                }
                // Detailed view when holding alt.
                if (Screen.hasAltDown()) {
                    ItemStack stack = placed.stack;
                    tooltip.add(elementHelper.item(stack));
                    MutableComponent name = stack.getHoverName().copy();

                    name.withStyle(ChatFormatting.GRAY);
                    tooltip.append(name);

                    // Makes the hovered item name gold.
                    if (placed == targetedItem) {
                        name.withStyle(ChatFormatting.GOLD);
                    }

                    List<Component> foodInfo = new ArrayList<>();
                    TFCCompat.INSTANCE.addFoodTooltipInfo(stack, foodInfo);
                    for (Component component : foodInfo) {
                        tooltip.add(component);
                    }
                }
            }
            if (!Screen.hasAltDown() && containsItems) {
                tooltip.add(Component.translatable("place_mats.tooltip.placemat.hold_alt_for_nutrition_info").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
        }
    }


    @Override
    public ResourceLocation getUid() {
        return PlaceMatJadePlugin.PLACE_MAT_INFO;
    }
}

package net.placemats.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.placemats.PlaceMatMain;
import net.placemats.common.block.CabinetMakerTableBlock;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.compat.ModCompat;
import net.placemats.compat.emi.PlaceMatEmiPlugin;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("unused")
public class PlaceMatTooltips{

    // Block Tooltips
    @SubscribeEvent
    public static void onTooltip(@NotNull ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        Block block = Block.byItem(stack.getItem());

        // Cabinetmaker's table
        if (block instanceof CabinetMakerTableBlock) {
            MutableComponent text = Component.translatable("place_mats.emi.cabinet_maker.desc1");
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.emi.cabinet_maker.desc2"));
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.emi.cabinet_maker.desc3"));

            tooltip.add(1, shiftHintTooltip(text));
        }
    }

    // Item Tooltips
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // Key
        if (stack.is(PlaceMatTags.Items.KEY)) {
            MutableComponent text = Component.translatable("place_mats.tooltip.key.explain").withStyle(ChatFormatting.WHITE);
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.tooltip.key.joke").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

            tooltip.add(1, shiftHintTooltip(text));
        }

        // Supports Dynamic Materials
        if (stack.is(PlaceMatTags.Items.DYNAMIC_MATERIALS)) {
            MutableComponent text = Component.translatable("place_mats.tooltip.dynamic_materials");

            if (ModCompat.EMI_LOADED) {
                text.append(CommonComponents.NEW_LINE);
                text.append(Component.translatable("place_mats.tooltip.press_for_info", PlaceMatEmiPlugin.getUsesKeyName()));
            }
            tooltip.add(1, text);
        }
    }

    /**
     * Component helper for "press-shift" tooltips.
     * @param component Component to display.
     * @return Component to display or shift display.
     */
    public static Component shiftHintTooltip(Component component) {
        if (Screen.hasShiftDown()) {
            return component;
        } else {
            return Component.translatable("place_mats.tooltip.shift_hint").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);
        }
    }
}

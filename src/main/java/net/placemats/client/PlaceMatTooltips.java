package net.placemats.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.placemats.PlaceMatMain;
import net.placemats.common.data.PlaceMatTags;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
@SuppressWarnings("unused")
public class PlaceMatTooltips{

    // Block Tooltips
    @SubscribeEvent
    public static void onTooltip(@NotNull ItemTooltipEvent event) {
        var tooltip = event.getToolTip();
        var stack = event.getItemStack();
        Block block = Block.byItem(stack.getItem());
    }

    // Item Tooltips
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final List<Component> text = event.getToolTip();

        if (stack.is(PlaceMatTags.Items.KEY)) {
            if (Screen.hasShiftDown()) {
                text.add(1, Component.translatable("place_mats.tooltip.key.explain").withStyle(ChatFormatting.WHITE));
                text.add(2, Component.literal(""));
                text.add(3, Component.translatable("place_mats.tooltip.key.joke").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                text.add(1, Component.translatable("place_mats.tooltip.shift_hint").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            }
        }
    }
}

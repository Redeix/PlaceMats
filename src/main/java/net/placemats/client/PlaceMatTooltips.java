package net.placemats.client;

import com.mojang.datafixers.util.Either;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.placemats.PlaceMatMain;
import net.placemats.client.gui.tooltip.TextureTooltip;
import net.placemats.client.gui.tooltip.TextureTooltipHelper;
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

        if (stack.isEmpty()) {
            return;
        }

        // Cabinetmaker's Table
        if (block instanceof CabinetMakerTableBlock) {
            MutableComponent text = Component.translatable("place_mats.emi.cabinet_maker.desc1");
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.emi.cabinet_maker.desc2"));
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.emi.cabinet_maker.desc3"));

            tooltip.add(shiftHintTooltip(text));
        }
    }

    // Item Tooltips
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (stack.isEmpty()) {
            return;
        }

        // Key
        if (stack.is(PlaceMatTags.Items.KEY)) {
            MutableComponent text = Component.translatable("place_mats.tooltip.key.explain").withStyle(ChatFormatting.WHITE);
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.tooltip.key.joke").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

            tooltip.add(shiftHintTooltip(text));
        }

        // Tall Tables
        if (stack.is(PlaceMatTags.Items.TALL_TABLES)) {
            MutableComponent text = Component.translatable("place_mats.tooltip.tall_tables.desc1");
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.tooltip.tall_tables.desc2"));
            text.append(CommonComponents.NEW_LINE);
            text.append(Component.translatable("place_mats.tooltip.tall_tables.desc3"));

            tooltip.add(shiftHintTooltip(text));
        }

        // Supports Dynamic Materials
        if (stack.is(PlaceMatTags.Items.DYNAMIC_MATERIALS)) {
            MutableComponent text = Component.translatable("place_mats.tooltip.dynamic_materials");

            if (ModCompat.EMI_LOADED) {
                text.append(CommonComponents.NEW_LINE);
                text.append(Component.translatable("place_mats.tooltip.press_for_info", PlaceMatEmiPlugin.getUsesKeyName()));
            }
            tooltip.add(text);
        }

        // Texture Bundles
        if (stack.is(PlaceMatTags.Items.TEXTURE_BUNDLES)) {
            MutableComponent text = Component.translatable("place_mats.tooltip.texture_bundles");
            tooltip.add(text);
        }
    }

    // Texture Provider Tooltips
    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(PlaceMatTags.Items.TEXTURE_PROVIDERS)) {
            return;
        }

        List<ResourceLocation> textures = TextureTooltipHelper.getTooltipTextures(stack);
        if (textures.isEmpty()) {
            return;
        }

        int insertIndex = Math.min(1, event.getTooltipElements().size());
        event.getTooltipElements().add(insertIndex, Either.right(new TextureTooltip(textures)));
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

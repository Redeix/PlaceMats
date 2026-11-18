package net.placemats.client.gui.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.placemats.PlaceMatMain;
import net.placemats.client.PlaceMatKeyMappings;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlacementRange;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.config.PlaceMatConfig;

@Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class PlaceMatHudOverlay {
    private static boolean wasLookingAtPlacemat = false;

    public record PlaceMatContext(
            Minecraft mc,
            Player player,
            Level level,
            BlockPos pos,
            BlockState state,
            PlaceMatBlock placeMatBlock,
            PlaceMatBlockEntity blockEntity,
            PlacementRange targetedRange,
            PlaceMatBlockEntity.PlacedItem targetedItem,
            ItemStack heldItem,
            boolean isLocked,
            boolean holdingKey
    ) {}

    public static PlaceMatContext getContext(Minecraft mc) {
        if (mc == null) return null;
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return null;

        if (!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        BlockState state = level.getBlockState(pos);

        if (!(be instanceof PlaceMatBlockEntity pmbe) || !(state.getBlock() instanceof PlaceMatBlock pmb)) {
            return null;
        }

        if (pmb.getPlacementRanges(state).isEmpty()) {
            return null;
        }

        ItemStack held = player.getMainHandItem();
        boolean isLocked = PlaceMatBlock.preventsInteraction(state);
        boolean holdingKey = held.is(PlaceMatTags.Items.KEY);

        if (isLocked && !holdingKey) {
            return null;
        }
        if (held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST) && !holdingKey) {
            return null;
        }

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 localHit = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

        PlaceMatBlockEntity.PlacedItem targetedItem = pmbe.getTargetedItem(eyePos, lookVec, pos);
        PlacementRange targetedRange = (targetedItem != null)
                ? pmbe.getRangeForItem(targetedItem)
                : pmb.getTargetedPlacementRange(state, localHit);

        return new PlaceMatContext(
                mc, player, level, pos, state, pmb, pmbe,
                targetedRange, targetedItem, held, isLocked, holdingKey
        );
    }

    public static boolean isSidebarActive(PlaceMatContext ctx) {
        if (ctx == null) return false;
        return !getSidebarEntries(ctx).isEmpty();
    }

    public static boolean shouldHideScoreboard(Minecraft mc) {
        PlaceMatContext ctx = getContext(mc);
        return isSidebarActive(ctx);
    }

    public static Component getSidebarTitle(PlaceMatContext ctx) {
        if (ctx != null && ctx.targetedItem() != null && ctx.heldItem().isEmpty()) {
            return Component.translatable("place_mats.hud.interaction.title");
        }
        return Component.translatable("place_mats.hud.controls.title");
    }

    public static List<Component> getSidebarEntries(PlaceMatContext ctx) {
        if (ctx == null) {
            return Collections.emptyList();
        }
        if (ctx.isLocked() && !ctx.holdingKey()) {
            return Collections.emptyList();
        }
        if (ctx.holdingKey()) {
            return Collections.emptyList();
        }
        if (ctx.targetedRange() != null && ctx.targetedRange().chair()) {
            return Collections.emptyList();
        }

        ItemStack held = ctx.heldItem();
        if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
            int maxCapacity = ctx.placeMatBlock().getContainerSize();
            if (ctx.blockEntity().getPlacedItems().size() >= maxCapacity) {
                return Collections.emptyList();
            }

            TagKey<Item> whitelist = ctx.targetedRange() != null ? ctx.targetedRange().whitelistTag() : null;
            if (whitelist != null && !held.is(whitelist)) {
                return Collections.emptyList();
            }

            List<Component> entries = new ArrayList<>();
            if (ctx.targetedRange() != null && !PlaceMatConfig.CLIENT.disableScoreboardControlsPanel.get()) {
                addPlacementControlEntries(entries, ctx.targetedRange());
            }
            return entries;
        } else if (held.isEmpty() && ctx.targetedItem() != null && ctx.targetedRange() != null) {
            List<Component> entries = new ArrayList<>();
            if (!PlaceMatConfig.CLIENT.disableScoreboardInteractionsPanel.get()) {
                addInteractionControlEntries(entries, ctx.targetedItem(), ctx.targetedRange());
            }
            return entries;
        }

        return Collections.emptyList();
    }

    public static void addPlacementControlEntries(List<Component> entries, PlacementRange range) {
        if (range.restricted()) return;
        boolean keyboardRotation = !PlaceMatConfig.CLIENT.disableCombinationBasedRotationControls.get();
        boolean keyboardElevation = !PlaceMatConfig.CLIENT.disableCombinationBasedElevationControls.get();

        entries.add(Component.empty());

        if (!range.yawDisabled() || !range.pitchDisabled()) {
            // Pitch Controls (y-rot)
            if (!range.pitchDisabled()) {
                MutableComponent pitchControl = Component.empty();
                if (!PlaceMatKeyMappings.KEY_Y_ROT.isUnbound()) {
                    MutableComponent pitchKeybind = Component.keybind(PlaceMatKeyMappings.KEY_Y_ROT.getName());
                    pitchControl.append(pitchKeybind);
                }
                if (keyboardRotation) {
                    MutableComponent pitchCombination = Component.translatable("place_mats.faux_key.shift").append("+").append(Component.translatable("place_mats.faux_key.mmb"));
                    if (!PlaceMatKeyMappings.KEY_Y_ROT.isUnbound()) {
                        pitchControl.append(" / ");
                    }
                    pitchControl.append(pitchCombination);
                }
                if (PlaceMatKeyMappings.KEY_Y_ROT.isUnbound() && !keyboardRotation) {
                    pitchControl = Component.translatable("key.keyboard.unknown").withStyle(ChatFormatting.DARK_RED);
                }
                entries.add(Component.translatable("place_mats.tooltip.placemat.pitch", pitchControl));
            }
            // Roll Controls (x-rot)
            if (!range.yawDisabled()) {
                MutableComponent yawControl = Component.empty();
                if (!PlaceMatKeyMappings.KEY_X_ROT.isUnbound()) {
                    MutableComponent yawKeybind = Component.keybind(PlaceMatKeyMappings.KEY_X_ROT.getName());
                    yawControl.append(yawKeybind);
                }
                if (keyboardRotation) {
                    MutableComponent yawCombination = Component.translatable("place_mats.faux_key.ctrl").append("+").append(Component.translatable("place_mats.faux_key.mmb"));
                    if (!PlaceMatKeyMappings.KEY_X_ROT.isUnbound()) {
                        yawControl.append(" / ");
                    }
                    yawControl.append(yawCombination);
                }
                if (PlaceMatKeyMappings.KEY_X_ROT.isUnbound() && !keyboardRotation) {
                    yawControl = Component.translatable("key.keyboard.unknown").withStyle(ChatFormatting.DARK_RED);
                }
                entries.add(Component.translatable("place_mats.tooltip.placemat.roll", yawControl));
            }
            // Reset Controls
            MutableComponent resetControl;
            if (PlaceMatKeyMappings.KEY_RESET.isUnbound()) {
                resetControl = Component.translatable("key.keyboard.unknown").withStyle(ChatFormatting.DARK_RED);
            } else {
                resetControl = Component.keybind(PlaceMatKeyMappings.KEY_RESET.getName());
            }
            entries.add(Component.translatable("place_mats.tooltip.placemat.reset_rotation",resetControl));

            entries.add(Component.empty());
        }

        if (!range.elevationDisabled()) {
            // Raise Controls
            MutableComponent raiseControl = Component.empty();
            if (!PlaceMatKeyMappings.KEY_UP.isUnbound()) {
                MutableComponent raiseKeybind = Component.keybind(PlaceMatKeyMappings.KEY_UP.getName());
                raiseControl.append(raiseKeybind);
            }
            if (keyboardElevation) {
                MutableComponent raiseCombination = Component.translatable("place_mats.faux_key.shift").append("+").append(Component.translatable("place_mats.faux_key.space"));
                if (!PlaceMatKeyMappings.KEY_UP.isUnbound()) {
                    raiseControl.append(" / ");
                }
                raiseControl.append(raiseCombination);
            }
            if (PlaceMatKeyMappings.KEY_UP.isUnbound() && !keyboardElevation) {
                raiseControl = Component.translatable("key.keyboard.unknown").withStyle(ChatFormatting.DARK_RED);
            }
            entries.add(Component.translatable("place_mats.tooltip.placemat.raise", raiseControl));

            // Lower Controls
            MutableComponent lowerControl = Component.empty();
            if (!PlaceMatKeyMappings.KEY_DOWN.isUnbound()) {
                MutableComponent lowerKeybind = Component.keybind(PlaceMatKeyMappings.KEY_DOWN.getName());
                lowerControl.append(lowerKeybind);
            }
            if (keyboardElevation) {
                MutableComponent lowerCombination = Component.translatable("place_mats.faux_key.ctrl").append("+").append(Component.translatable("place_mats.faux_key.space"));
                if (!PlaceMatKeyMappings.KEY_DOWN.isUnbound()) {
                    lowerControl.append(" / ");
                }
                lowerControl.append(lowerCombination);
            }
            if (PlaceMatKeyMappings.KEY_DOWN.isUnbound() && !keyboardElevation) {
                lowerControl = Component.translatable("key.keyboard.unknown").withStyle(ChatFormatting.DARK_RED);
            }
            entries.add(Component.translatable("place_mats.tooltip.placemat.lower", lowerControl));
            entries.add(Component.empty());
        }

        // Place Controls
        entries.add(Component.translatable("place_mats.tooltip.placemat.place", Component.translatable("place_mats.faux_key.rmb")));

        MutableComponent placeStackKeybind = Component.translatable("place_mats.faux_key.shift").append("+").append(Component.translatable("place_mats.faux_key.rmb"));
        if (range.maxStackSize() > 1) {
            entries.add(Component.translatable("place_mats.tooltip.placemat.place_stack", placeStackKeybind));
        }
    }

    @SuppressWarnings("unused")
    public static void addInteractionControlEntries(List<Component> entries, PlaceMatBlockEntity.PlacedItem item, PlacementRange range) {
        if (range.restricted()) return;
        entries.add(Component.empty());

        // Remove Controls
        entries.add(Component.translatable("place_mats.tooltip.placemat.remove", Component.translatable("place_mats.faux_key.rmb")));

        MutableComponent placeStackKeybind = Component.translatable("place_mats.faux_key.shift").append("+").append(Component.translatable("place_mats.faux_key.rmb"));
        if (range.maxStackSize() > 1) {
            entries.add(Component.translatable("place_mats.tooltip.placemat.remove_stack", placeStackKeybind));
        }
        entries.add(Component.empty());

        // Interact Controls
        entries.add(Component.translatable("place_mats.tooltip.placemat.use", Component.translatable("place_mats.faux_key.lmb")));
    }

    public static void handleActionBar(Player player, PlaceMatContext ctx) {
        if (ctx.holdingKey()) {
            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.key").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
        } else if (ctx.targetedRange() != null && ctx.targetedRange().chair()) {
            if (!ctx.targetedRange().hasEntity(ctx.level(), ctx.pos(), ctx.state())) {
                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.sit").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
            } else if (!(ctx.targetedRange().getEntity(ctx.level(), ctx.pos(), ctx.state()) instanceof Player)) {
                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.kick").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
            }
        } else if (!ctx.heldItem().isEmpty() && !ctx.heldItem().is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
            int maxCapacity = ctx.placeMatBlock().getContainerSize();
            if (ctx.blockEntity().getPlacedItems().size() >= maxCapacity) {
                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.full", maxCapacity).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED), true);
            }
        }
    }

    public static void renderSidebar(GuiGraphics guiGraphics, Font font, Component title, List<Component> lines) {
        if (lines.isEmpty() && title == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int bgColor = mc.options.getBackgroundColor(0.3F);
        int titleBgColor = mc.options.getBackgroundColor(0.4F);

        int maxLineWidth = title != null ? font.width(title) : 0;
        for (Component line : lines) {
            int w = font.width(line);
            if (w > maxLineWidth) {
                maxLineWidth = w;
            }
        }

        int lineHeight = font.lineHeight;
        int padding = 3;
        int margin = 3;

        int totalLines = lines.size() + (title != null ? 1 : 0);
        int totalHeight = totalLines * lineHeight;

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        int startY = screenHeight / 2 + totalHeight / 3;
        int rightX = screenWidth - margin;
        int leftX = rightX - maxLineWidth - padding * 2;

        int currentY = startY - totalHeight;

        if (title != null) {
            guiGraphics.fill(leftX - 2, currentY - 1, rightX + 2, currentY + lineHeight, titleBgColor);
            int titleX = leftX + (maxLineWidth + padding * 2 - font.width(title)) / 2;
            guiGraphics.drawString(font, title, titleX, currentY, 0xFFFFFFFF, true);
            currentY += lineHeight;
        }

        for (Component line : lines) {
            guiGraphics.fill(leftX - 2, currentY, rightX + 2, currentY + lineHeight, bgColor);
            guiGraphics.drawString(font, line, leftX + padding, currentY, 0xFFFFFFFF, true);
            currentY += lineHeight;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (VanillaGuiOverlay.SCOREBOARD.id().equals(event.getOverlay().id())) {
            if (shouldHideScoreboard(Minecraft.getInstance())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        PlaceMatContext ctx = getContext(mc);
        if (ctx == null) {
            handleOutsideLook(player);
            return;
        }

        wasLookingAtPlacemat = true;
        if (!PlaceMatConfig.CLIENT.disableActionBarInformation.get()) {
            handleActionBar(player, ctx);
        }

        List<Component> entries = getSidebarEntries(ctx);
        if (!entries.isEmpty()) {
            Component title = getSidebarTitle(ctx);
            renderSidebar(event.getGuiGraphics(), mc.font, title, entries);
        }
    }

    public static void handleOutsideLook(Player player) {
        if (wasLookingAtPlacemat) {
            player.displayClientMessage(Component.empty(), true);
            wasLookingAtPlacemat = false;
        }
    }
}

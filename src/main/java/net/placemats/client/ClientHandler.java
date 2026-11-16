package net.placemats.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.placemats.PlaceMatMain;
import net.placemats.client.renderer.blockentity.PlaceMatRenderer;
import net.placemats.common.block.LockType;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlacementRange;
import net.placemats.common.event.PlaceMatInteractions;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.resource.DefinitionManager;
import net.placemats.network.PlaceMatsNetworkHandler;
import net.placemats.network.packet.PlaceMatPacket;

@SuppressWarnings("DuplicateExpressions")
public class ClientHandler {
    private static boolean wasLookingAtPlacemat = false;

    public static final TextureAtlasSprite LOCKED_OVERLAY = PlaceMatRenderer.getBlockAtlasSprite(
            ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/renders/key_render_locked")
    );
    public static final TextureAtlasSprite UNLOCKED_OVERLAY = PlaceMatRenderer.getBlockAtlasSprite(
            ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/renders/key_render_unlocked")
    );

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null)
            return;

        ItemStack held = player.getMainHandItem();

        HitResult hit = mc.hitResult;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Vec3 eyePos = player.getEyePosition(event.getPartialTick());
        Vec3 lookVec = player.getViewVector(event.getPartialTick());
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        // Handles key overlays.
        boolean holdingKey = held.is(PlaceMatTags.Items.KEY) || player.getOffhandItem().is(PlaceMatTags.Items.KEY);
        if (holdingKey) {
            BlockPos playerPos = player.blockPosition();

            // Check for placemats in a radius around the player.
            int radius = 5;
            List<BlockPos> targetBlocks = getBlocksInRadius(mc.level, playerPos, radius);
            List<BlockPos> lockedBlocks = new ArrayList<>(targetBlocks.stream().filter(pos -> {
                BlockState state = mc.level.getBlockState(pos);
                return state.hasProperty(PlaceMatBlock.LOCKED) && state.getValue(PlaceMatBlock.LOCKED) == LockType.STRICT;
            }).toList());
            List<BlockPos> unlockedBlocks = new ArrayList<>(targetBlocks.stream().filter(pos -> {
                BlockState state = mc.level.getBlockState(pos);
                return !state.hasProperty(PlaceMatBlock.LOCKED) || state.getValue(PlaceMatBlock.LOCKED) != LockType.STRICT;
            }).toList());

            // Render overlays on each block.
            renderBlockOverlays(poseStack, buffer, camPos, lockedBlocks, LOCKED_OVERLAY, new Color(0xFFFF0000, true));
            renderBlockOverlays(poseStack, buffer, camPos, unlockedBlocks, UNLOCKED_OVERLAY, new Color(0xFF00E1E1, true));

            buffer.endBatch(RenderType.lines());
            buffer.endBatch(RenderType.translucent());
            buffer.endBatch();
        }

        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsOutlines(pmbe.getBlockState())) {
                poseStack.pushPose();
                poseStack.translate(pos.getX() - mc.gameRenderer.getMainCamera().getPosition().x,
                        pos.getY() - mc.gameRenderer.getMainCamera().getPosition().y,
                        pos.getZ() - mc.gameRenderer.getMainCamera().getPosition().z);

                BlockState state = pmbe.getBlockState();
                Direction facing = (state.getBlock() instanceof PlaceMatBlock pmb && !pmb.isRotateZones())
                        ? Direction.NORTH
                        : (state.hasProperty(PlaceMatCardinalBlock.FACING) ? state.getValue(PlaceMatCardinalBlock.FACING) : Direction.NORTH);

                poseStack.pushPose();
                poseStack.translate(0.5, 0, 0.5);
                if (facing != Direction.NORTH) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
                }
                poseStack.translate(-0.5, 0, -0.5);

                if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
                    Vec3 location = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

                    if (pmbe.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                        if (pmb.getPlacementRanges(pmbe.getBlockState()).isEmpty()) {
                            poseStack.popPose();
                            poseStack.popPose();
                            return;
                        }
                        PlacementRange targetedRange = pmb.getTargetedPlacementRange(pmbe.getBlockState(), location);

                        // Render unhovered non-chair placement ranges.
                        pmb.addPlacementRanges(pmbe.getBlockState(), range -> {
                            if (range.chair() || range.equals(targetedRange)) {
                                return;
                            }
                            PlaceMatRenderer.renderBounds(poseStack, buffer, range, 0.5f, 0.5f, 0.5f);
                        });

                        // Render the hovered range highlight.
                        if (targetedRange != null) {
                            if (targetedRange.chair()) {
                                PlaceMatRenderer.renderBounds(poseStack, buffer, targetedRange, 1.0f, 1.0f, 0.0f);
                            } else if (targetedRange.restricted()) {
                                PlaceMatRenderer.renderBounds(poseStack, buffer, targetedRange, 1.0f, 0.0f, 1.0f);
                            } else {
                                PlaceMatRenderer.renderBounds(poseStack, buffer, targetedRange, 0.0f, 0.0f, 1.0f);
                            }

                            // Render item placement preview on non-chair ranges.
                            if (!targetedRange.chair()) {
                                DefinitionManager.PlaceMatDefinition def = DefinitionManager.getEffectiveDefinition(held, pmbe.getBlockState(), targetedRange);
                                if (pmb.isDisableCustomModels() || targetedRange.disableCustomModels()) {
                                    def = DefinitionManager.PlaceMatDefinition.DEFAULT(held.getItem());
                                }
                                float multiplier = pmb.getScaleMultiplier() * targetedRange.scaleMultiplier();

                                // Project ray onto the box's bottom plane.
                                double planeY = pos.getY() + targetedRange.box().minY;

                                Vec3 intersection = null;
                                if (Math.abs(lookVec.y) > 1e-6) {
                                    double t = (planeY - eyePos.y) / lookVec.y;
                                    if (t > 0) {
                                        intersection = eyePos.add(lookVec.scale(t));
                                    }
                                }

                                float placementX, placementZ;
                                if (intersection != null) {
                                    Vec3 relativeIntersection = intersection.subtract(pos.getX(), pos.getY(), pos.getZ());
                                    Vec3 localIntersection = PlaceMatBlock.getLocalHitVec(pmbe.getBlockState(), relativeIntersection);
                                    placementX = (float) localIntersection.x - (def.size().x * multiplier) / 2f;
                                    placementZ = (float) localIntersection.z - (def.size().y * multiplier) / 2f;
                                } else {
                                    // Fallback to hit location if ray is parallel to plane.
                                    Vec3 localLocation = PlaceMatBlock.getLocalHitVec(pmbe.getBlockState(), location);
                                    placementX = (float) localLocation.x - (def.size().x * multiplier) / 2f;
                                    placementZ = (float) localLocation.z - (def.size().y * multiplier) / 2f;
                                }

                                Vec2 placementPos;
                                float effectiveHeight;
                                if (targetedRange.restricted()) {
                                    float centerX = (float) (targetedRange.box().minX + targetedRange.box().maxX) / 2f;
                                    float centerZ = (float) (targetedRange.box().minZ + targetedRange.box().maxZ) / 2f;
                                    placementPos = new Vec2(centerX - (def.size().x * multiplier) / 2f, centerZ - (def.size().y * multiplier) / 2f);
                                    if (targetedRange.snapToCenter()) {
                                        float centerY = (float) (targetedRange.box().minY + targetedRange.box().maxY) / 2f;
                                        float itemHalfHeight = (def.getItemHeight() * multiplier) / 2f;
                                        effectiveHeight = centerY - (float) targetedRange.box().minY - itemHalfHeight;
                                    } else {
                                        effectiveHeight = 0;
                                    }
                                } else {
                                    float minX = (float) targetedRange.box().minX;
                                    float maxX = (float) targetedRange.box().maxX - def.size().x * multiplier;
                                    float minZ = (float) targetedRange.box().minZ;
                                    float maxZ = (float) targetedRange.box().maxZ - def.size().y * multiplier;

                                    float clampedX = Math.max(minX, Math.min(maxX, placementX));
                                    float clampedZ = Math.max(minZ, Math.min(maxZ, placementZ));
                                    placementPos = new Vec2(clampedX, clampedZ);
                                    effectiveHeight = pmbe.calculateEffectiveHeight(held, placementPos, pmbe.getCurrentHeight(), targetedRange);
                                }
                                boolean valid = pmbe.canPlace(held, placementPos, pmbe.getCurrentHeight(), targetedRange);

                                PlaceMatRenderer.renderPreview(poseStack, buffer, placementPos, def.size(), def.scale(),
                                        pmbe.getCurrentRotation(), pmbe.getCurrentPitch(), pmbe.getCurrentRoll(),
                                        effectiveHeight, valid, held, mc.getItemRenderer(), targetedRange, pmb);
                            }
                        }
                    }
                } else {
                    PlaceMatBlockEntity.PlacedItem targeted = pmbe.getTargetedItem(eyePos, lookVec, pos);
                    if (targeted != null) {
                        PlaceMatRenderer.renderItemOutline(poseStack, buffer, targeted, 1, 0.84f, 0);
                    } else if (pmbe.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                        // Chair Zone Render
                        Vec3 location = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
                        PlacementRange targetedRange = pmb.getTargetedPlacementRange(pmbe.getBlockState(), location);
                        if (targetedRange != null && targetedRange.chair()) {
                            PlaceMatRenderer.renderBounds(poseStack, buffer, targetedRange, 1.0f, 1.0f, 0.0f);
                        }
                    }
                }
                poseStack.popPose();
                poseStack.popPose();
            }
        }
        buffer.endBatch();
    }

    /**
     * Finds all place mat block positions within a spherical radius.
     *
     * @param level     The level to scan for matching blocks.
     * @param centerPos The center position of the scan.
     * @param radius    The radius of the spherical scan.
     * @return A list of matching block positions.
     */
    public static List<BlockPos> getBlocksInRadius(Level level, BlockPos centerPos, int radius) {
        List<BlockPos> matches = new ArrayList<>();
        int maxDistSqr = radius * radius;

        for (BlockPos pos : BlockPos.betweenClosed(
                centerPos.offset(-radius, -radius, -radius),
                centerPos.offset(radius, radius, radius)
        )) {
            if (pos.distSqr(centerPos) <= maxDistSqr) {
                if (level.getBlockEntity(pos) instanceof PlaceMatBlockEntity || level.getBlockState(pos).getBlock() instanceof PlaceMatBlock) {
                    matches.add(pos.immutable());
                }
            }
        }
        return matches;
    }

    /**
     * Generic method for rendering a textured block overlay.
     * Runs through a list of block positions and renders a textured overlay on each block with a given color.
     */
    public static void renderBlockOverlays(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffer,
            Vec3 camPos,
            List<BlockPos> positions,
            TextureAtlasSprite sprite,
            Color color
    ) {
        AABB unitBox = new AABB(-0.001, -0.001, -0.001, 1.001, 1.001, 1.001);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        for (BlockPos p : positions) {
            poseStack.pushPose();
            poseStack.translate(p.getX() - camPos.x, p.getY() - camPos.y, p.getZ() - camPos.z);
            PlaceMatRenderer.renderOverlay(poseStack, buffer, unitBox, r, g, b, a, LightTexture.FULL_BRIGHT, sprite);
            poseStack.popPose();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof PlaceMatBlockEntity pmbe) {
            BlockHitResult hit = null;
            if (event.getLevel().isClientSide && Minecraft.getInstance().hitResult instanceof BlockHitResult bhr) {
                hit = bhr;
            }
            if (PlaceMatInteractions.handleLeftClick(pmbe, event.getEntity(), hit)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.isCanceled()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
            if (pmbe.getBlockState().getBlock() instanceof PlaceMatBlock pmb && pmb.getPlacementRanges(pmbe.getBlockState()).isEmpty()) {
                return;
            }
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            Vec3 location = event.getHitVec().getLocation().subtract(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());

            // Interaction with existing items.
            PlacementRange targetedRange = null;
            PlaceMatBlock pmb = null;
            if (pmbe.getBlockState().getBlock() instanceof PlaceMatBlock block) {
                pmb = block;
                BlockState state = pmbe.getBlockState();
                targetedRange = pmb.getTargetedPlacementRange(state, location);
            }

            if (pmbe.getTargetedItem(eyePos, lookVec, event.getPos()) != null) {
                if (held.isEmpty()) {
                    return;
                }
            }

            // Placement logic.
            if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
                if (targetedRange != null && targetedRange.chair()) {
                    return;
                }
                if (pmb != null) {
                    TagKey<Item> whitelist = targetedRange != null ? targetedRange.whitelistTag() : null;
                    if (whitelist != null && !held.is(whitelist)) {
                        return;
                    }
                }

                if (event.getLevel().isClientSide) {
                    final PlacementRange[] targetedRangeArr = {targetedRange};
                    if (targetedRangeArr[0] != null) {
                        DefinitionManager.PlaceMatDefinition def = DefinitionManager.getEffectiveDefinition(held, pmbe.getBlockState(), targetedRangeArr[0]);
                        if (pmb != null && (pmb.isDisableCustomModels() || targetedRangeArr[0].disableCustomModels())) {
                            def = DefinitionManager.PlaceMatDefinition.DEFAULT(held.getItem());
                        }
                        float multiplier = pmb != null ? pmb.getScaleMultiplier() * targetedRangeArr[0].scaleMultiplier() : 1.0f;

                        // Project ray onto the box's bottom plane.
                        double planeY = event.getPos().getY() + targetedRangeArr[0].box().minY;

                        Vec3 intersection = null;
                        if (Math.abs(lookVec.y) > 1e-6) {
                            double t = (planeY - eyePos.y) / lookVec.y;
                            if (t > 0) {
                                intersection = eyePos.add(lookVec.scale(t));
                            }
                        }

                        float placementX, placementZ;
                        if (intersection != null) {
                            Vec3 relativeIntersection = intersection.subtract(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
                            Vec3 localIntersection = PlaceMatBlock.getLocalHitVec(pmbe.getBlockState(), relativeIntersection);
                            placementX = (float) localIntersection.x - (def.size().x * multiplier) / 2f;
                            placementZ = (float) localIntersection.z - (def.size().y * multiplier) / 2f;
                        } else {
                            Vec3 localLocation = PlaceMatBlock.getLocalHitVec(pmbe.getBlockState(), location);
                            placementX = (float) localLocation.x - (def.size().x * multiplier) / 2f;
                            placementZ = (float) localLocation.z - (def.size().y * multiplier) / 2f;
                        }

                        Vec2 placementPos;
                        if (targetedRangeArr[0].restricted()) {
                            float centerX = (float) (targetedRangeArr[0].box().minX + targetedRangeArr[0].box().maxX) / 2f;
                            float centerZ = (float) (targetedRangeArr[0].box().minZ + targetedRangeArr[0].box().maxZ) / 2f;
                            placementPos = new Vec2(centerX - (def.size().x * multiplier) / 2f, centerZ - (def.size().y * multiplier) / 2f);
                        } else {
                            float minX = (float) targetedRangeArr[0].box().minX;
                            float maxX = (float) targetedRangeArr[0].box().maxX - def.size().x * multiplier;
                            float minZ = (float) targetedRangeArr[0].box().minZ;
                            float maxZ = (float) targetedRangeArr[0].box().maxZ - def.size().y * multiplier;

                            float clampedX = Math.max(minX, Math.min(maxX, placementX));
                            float clampedZ = Math.max(minZ, Math.min(maxZ, placementZ));
                            placementPos = new Vec2(clampedX, clampedZ);
                        }

                        int count = Screen.hasShiftDown() ? held.getCount() : 1;
                        PlaceMatsNetworkHandler.INSTANCE
                                .sendToServer(new PlaceMatPacket(event.getPos(), placementPos, location, pmbe.getCurrentRotation(), pmbe.getCurrentPitch(),
                                        pmbe.getCurrentRoll(), pmbe.getCurrentHeight(), count));
                    }
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        if (!(mc.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            handleOutsideLook(player);
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (!(blockEntity instanceof PlaceMatBlockEntity foodPlacer) || !(blockEntity.getBlockState().getBlock() instanceof PlaceMatBlock pmb) || pmb.getPlacementRanges(blockEntity.getBlockState()).isEmpty()) {
            handleOutsideLook(player);
            return;
        }

        wasLookingAtPlacemat = true;
        BlockState state = blockEntity.getBlockState();
        ItemStack held = player.getMainHandItem();

        boolean isLocked = PlaceMatBlock.preventsInteraction(state);
        boolean holdingKey = held.is(PlaceMatTags.Items.KEY);

        if (isLocked && !holdingKey) return;
        if (held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST) && !holdingKey) return;

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 localHit = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

        PlaceMatBlockEntity.PlacedItem targetedItem = foodPlacer.getTargetedItem(eyePos, lookVec, pos);
        PlacementRange targetedRange = (targetedItem != null)
                ? foodPlacer.getRangeForItem(targetedItem)
                : pmb.getTargetedPlacementRange(state, localHit);

        if (holdingKey) {
            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.key").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
        } else if (targetedItem != null) {
            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.interacting").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
        } else if (targetedRange != null && targetedRange.chair()) {
            // Chair
            if (!targetedRange.hasEntity(mc.level, pos, state)) {
                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.sit").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
            } else if (targetedRange.hasEntity(mc.level, pos, state) && !(targetedRange.getEntity(mc.level, pos, state) instanceof Player)) {
                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.kick").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
            }
        } else if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {

            int maxCapacity = pmb.getContainerSize();
            if (foodPlacer.getPlacedItems().size() >= maxCapacity) {
                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.full", maxCapacity).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED), true);
            } else {
                TagKey<Item> whitelist = targetedRange != null ? targetedRange.whitelistTag() : null;

                if (whitelist != null && !held.is(whitelist)) {
                    player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.placing").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                } else {
                    MutableComponent instructions = Component.empty();
                    if (targetedRange == null || !targetedRange.yawDisabled())
                        instructions.append(Component.translatable("place_mats.tooltip.placemat.yaw"));
                    if (targetedRange == null || !targetedRange.pitchDisabled())
                        instructions.append(Component.translatable("place_mats.tooltip.placemat.pitch"));
                    if (targetedRange == null || !targetedRange.elevationDisabled())
                        instructions.append(Component.translatable("place_mats.tooltip.placemat.elevation"));

                    player.displayClientMessage(instructions.withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                }
            }
        } else {
            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.instructions").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
        }
    }

    private static void handleOutsideLook(Player player) {
        if (wasLookingAtPlacemat) {
            player.displayClientMessage(Component.empty(), true);
            wasLookingAtPlacemat = false;
        }
    }

    /**
     * Handles disabling jump when holding shift and looking at a placemat.
     * `onClientTick` Does not continuously suppress the jump input while modifying height.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && event.getAction() == 1 && mc.options.keyJump.matches(event.getKey(), event.getScanCode())) {
            if (Screen.hasShiftDown() && !player.getMainHandItem().isEmpty()) {
                HitResult hit = mc.hitResult;
                if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                    assert mc.level != null;
                    if (mc.level.getBlockEntity(blockHit.getBlockPos()) instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
                        if (pmbe.getBlockState().getBlock() instanceof PlaceMatBlock pmb && pmb.getPlacementRanges(pmbe.getBlockState()).isEmpty()) {
                            return;
                        }
                        mc.options.keyJump.setDown(false);
                    }
                }
            }
        }

        // Keybind driven item rotations/transformations.
        if (player != null && (event.getAction() == 1 || event.getAction() == 2) && !player.getMainHandItem().isEmpty()) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                assert mc.level != null;
                BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
                if (be instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
                    PlacementRange targetedRange = null;
                    if (mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlaceMatBlock pmb) {
                        if (pmb.getPlacementRanges(pmbe.getBlockState()).isEmpty()) {
                            return;
                        }
                        Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                        targetedRange = pmb.getTargetedPlacementRange(mc.level.getBlockState(blockHit.getBlockPos()), location);
                    }

                    if (PlaceMatKeyMappings.KEY_Y_ROT.matches(event.getKey(), event.getScanCode())) {
                        if (targetedRange != null && targetedRange.yawDisabled())
                            return;
                        float rotation = pmbe.getCurrentRotation();
                        rotation = (rotation + (Screen.hasControlDown() ? -15f : 15f)) % 360f;
                        if (rotation < 0)
                            rotation += 360f;
                        pmbe.setCurrentRotation(rotation);
                    } else if (PlaceMatKeyMappings.KEY_X_ROT.matches(event.getKey(), event.getScanCode())) {
                        if (targetedRange != null && targetedRange.pitchDisabled())
                            return;
                        float pitch = pmbe.getCurrentPitch();
                        pitch = (pitch + (Screen.hasControlDown() ? -15f : 15f)) % 360f;
                        if (pitch < 0)
                            pitch += 360f;
                        pmbe.setCurrentPitch(pitch);
                    } else if (PlaceMatKeyMappings.KEY_UP.matches(event.getKey(), event.getScanCode())) {
                        if (targetedRange != null && (targetedRange.elevationDisabled() || targetedRange.restricted()))
                            return;
                        float height = pmbe.getCurrentHeight();
                        height = Math.min(1.0f, height + 0.015625f);
                        pmbe.setCurrentHeight(height);
                    } else if (PlaceMatKeyMappings.KEY_DOWN.matches(event.getKey(), event.getScanCode())) {
                        if (targetedRange != null && (targetedRange.elevationDisabled() || targetedRange.restricted()))
                            return;
                        float height = pmbe.getCurrentHeight();
                        height = Math.max(0, height - 0.015625f);
                        pmbe.setCurrentHeight(height);
                    }
                }
            }
        }
    }

    /**
     * Handles converting key inputs into place mat transformations.
     * `onKeyInput` is needed to disable keys without buggy interactions.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && mc.options.keyJump.isDown()) {
            if (Screen.hasShiftDown() && !player.getMainHandItem().isEmpty()) {
                HitResult hit = mc.hitResult;
                if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                    assert mc.level != null;
                    BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
                    if (be instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
                        if (mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlaceMatBlock pmb) {
                            if (pmb.getPlacementRanges(pmbe.getBlockState()).isEmpty()) {
                                return;
                            }
                            Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                            PlacementRange targetedRange = pmb.getTargetedPlacementRange(mc.level.getBlockState(blockHit.getBlockPos()), location);
                            if (targetedRange != null && (targetedRange.elevationDisabled() || targetedRange.restricted()))
                                return;
                        }

                        float height = pmbe.getCurrentHeight();
                        if (Screen.hasControlDown()) {
                            height = Math.max(0, height - 0.015625f);
                        } else {
                            height = Math.min(1.0f, height + 0.015625f);
                        }
                        pmbe.setCurrentHeight(height);

                        mc.options.keyJump.setDown(false);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && Screen.hasShiftDown() && !player.getMainHandItem().isEmpty()) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                if (mc.level != null && mc.level.getBlockEntity(blockHit.getBlockPos()) instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
                    if (mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlaceMatBlock pmb) {
                        if (pmb.getPlacementRanges(pmbe.getBlockState()).isEmpty()) {
                            return;
                        }
                        Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                        PlacementRange targetedRange = pmb.getTargetedPlacementRange(mc.level.getBlockState(blockHit.getBlockPos()), location);
                        if (targetedRange != null && (targetedRange.elevationDisabled() || targetedRange.restricted()))
                            return;
                    }
                    event.getInput().jumping = false;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && Screen.hasShiftDown() && !mc.player.getMainHandItem().isEmpty()) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                assert mc.level != null;
                BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
                if (be instanceof PlaceMatBlockEntity pmbe && !PlaceMatBlock.preventsInteraction(pmbe.getBlockState())) {
                    PlaceMatBlock pmb;
                    PlacementRange targetedRange = null;
                    if (mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlaceMatBlock block) {
                        pmb = block;
                        if (pmb.getPlacementRanges(mc.level.getBlockState(blockHit.getBlockPos())).isEmpty()) {
                            return;
                        }
                        Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                        targetedRange = pmb.getTargetedPlacementRange(mc.level.getBlockState(blockHit.getBlockPos()), location);
                    }
                    if (Screen.hasControlDown()) {
                        if (targetedRange != null && targetedRange.pitchDisabled())
                            return;
                        float pitch = pmbe.getCurrentPitch();
                        pitch = (pitch + (float) event.getScrollDelta() * 15f) % 360f;
                        if (pitch < 0)
                            pitch += 360f;
                        pmbe.setCurrentPitch(pitch);
                    } else {
                        if (targetedRange != null && targetedRange.yawDisabled())
                            return;
                        float rotation = pmbe.getCurrentRotation();
                        rotation = (rotation + (float) event.getScrollDelta() * 15f) % 360f;
                        if (rotation < 0)
                            rotation += 360f;
                        pmbe.setCurrentRotation(rotation);
                    }
                    event.setCanceled(true);
                }
            }
        }
    }
}

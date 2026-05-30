package net.placemats.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.placemats.PlaceMatMain;
import net.placemats.client.renderer.blockentity.PlaceMatRenderer;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.event.PlaceMatInteractions;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.data.resource.DefinitionManager;
import net.placemats.network.PlaceMatsNetworkHandler;
import net.placemats.network.packet.PlaceMatPacket;

public class ClientHandler {
    private static boolean wasLookingAtPlacemat = false;

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
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof PlaceMatBlockEntity foodPlacer) {
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(pos.getX() - mc.gameRenderer.getMainCamera().getPosition().x,
                        pos.getY() - mc.gameRenderer.getMainCamera().getPosition().y,
                        pos.getZ() - mc.gameRenderer.getMainCamera().getPosition().z);

                MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
                Vec3 eyePos = player.getEyePosition(event.getPartialTick());
                Vec3 lookVec = player.getViewVector(event.getPartialTick());

                BlockState state = foodPlacer.getBlockState();
                Direction facing = state.hasProperty(PlaceMatBlock.FACING) ? state.getValue(PlaceMatBlock.FACING) : Direction.NORTH;
                poseStack.pushPose();
                poseStack.translate(0.5, 0, 0.5);
                if (facing != Direction.NORTH) {
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - facing.toYRot()));
                }
                poseStack.translate(-0.5, 0, -0.5);

                if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
                    Vec3 location = blockHit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

                    final PlaceMatBlock.PlacementRange[] targetedRange = { null };
                    if (foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                        pmb.addPlacementRanges(foodPlacer.getBlockState(), range -> {
                            PlaceMatRenderer.renderBounds(poseStack, buffer, range, 0.5f, 0.5f, 0.5f);
                        });
                        targetedRange[0] = pmb.getTargetedPlacementRange(foodPlacer.getBlockState(), location);
                        if (targetedRange[0] != null) {
                            if (targetedRange[0].restricted()) {
                                PlaceMatRenderer.renderBounds(poseStack, buffer, targetedRange[0], 1, 0, 1);
                            } else {
                                PlaceMatRenderer.renderBounds(poseStack, buffer, targetedRange[0], 0, 0, 1);
                            }
                        }
                    }

                    if (targetedRange[0] != null && foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                        DefinitionManager.PlaceMatDefinition def = DefinitionManager.getDefinition(held.getItem());
                        if (pmb.isDisableCustomModels() || targetedRange[0].disableCustomModels()) {
                            def = DefinitionManager.PlaceMatDefinition.DEFAULT(held.getItem());
                        }
                        float multiplier = pmb.getScaleMultiplier() * targetedRange[0].scaleMultiplier();

                        // Project ray onto the box's bottom plane.
                        double planeY = pos.getY() + targetedRange[0].box().minY;

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
                            Vec3 localIntersection = PlaceMatBlock.getLocalHitVec(foodPlacer.getBlockState(), relativeIntersection);
                            placementX = (float) localIntersection.x - (def.size().x * multiplier) / 2f;
                            placementZ = (float) localIntersection.z - (def.size().y * multiplier) / 2f;
                        } else {
                            // Fallback to hit location if ray is parallel to plane.
                            Vec3 localLocation = PlaceMatBlock.getLocalHitVec(foodPlacer.getBlockState(), location);
                            placementX = (float) localLocation.x - (def.size().x * multiplier) / 2f;
                            placementZ = (float) localLocation.z - (def.size().y * multiplier) / 2f;
                        }

                        Vec2 placementPos;
                        float effectiveHeight;
                        if (targetedRange[0].restricted()) {
                            float centerX = (float) (targetedRange[0].box().minX + targetedRange[0].box().maxX) / 2f;
                            float centerZ = (float) (targetedRange[0].box().minZ + targetedRange[0].box().maxZ) / 2f;
                            placementPos = new Vec2(centerX - (def.size().x * multiplier) / 2f, centerZ - (def.size().y * multiplier) / 2f);
                            if (targetedRange[0].snapToCenter()) {
                                float centerY = (float) (targetedRange[0].box().minY + targetedRange[0].box().maxY) / 2f;
                                float itemHalfHeight = (def.getItemHeight() * multiplier) / 2f;
                                effectiveHeight = centerY - (float) targetedRange[0].box().minY - itemHalfHeight;
                            } else {
                                effectiveHeight = 0;
                            }
                        } else {
                            float minX = (float) targetedRange[0].box().minX;
                            float maxX = (float) targetedRange[0].box().maxX - def.size().x * multiplier;
                            float minZ = (float) targetedRange[0].box().minZ;
                            float maxZ = (float) targetedRange[0].box().maxZ - def.size().y * multiplier;

                            float clampedX = Math.max(minX, Math.min(maxX, placementX));
                            float clampedZ = Math.max(minZ, Math.min(maxZ, placementZ));
                            placementPos = new Vec2(clampedX, clampedZ);
                            effectiveHeight = foodPlacer.calculateEffectiveHeight(held, placementPos, foodPlacer.getCurrentHeight(), targetedRange[0]);
                        }
                        boolean valid = foodPlacer.canPlace(held, placementPos, foodPlacer.getCurrentHeight(), targetedRange[0]);

                        PlaceMatRenderer.renderPreview(poseStack, buffer, placementPos, def.size(), def.scale(), foodPlacer.getCurrentRotation(), foodPlacer.getCurrentPitch(),
                                foodPlacer.getCurrentRoll(), effectiveHeight, valid, held, mc.getItemRenderer(), targetedRange[0], (PlaceMatBlock) foodPlacer.getBlockState().getBlock());
                    }
                } else if (held.isEmpty()) {
                    PlaceMatBlockEntity.PlacedItem targeted = foodPlacer.getTargetedItem(eyePos, lookVec, pos);
                    if (targeted != null) {
                        PlaceMatRenderer.renderItemOutline(poseStack, buffer, targeted, 1, 0.84f, 0);
                    }
                }

                poseStack.popPose();

                buffer.endBatch(RenderType.lines());
                buffer.endBatch(RenderType.cutout());
                buffer.endBatch();

                poseStack.popPose();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof PlaceMatBlockEntity foodPlacer) {
            BlockHitResult hit = null;
            if (event.getLevel().isClientSide && Minecraft.getInstance().hitResult instanceof BlockHitResult bhr) {
                hit = bhr;
            }
            if (PlaceMatInteractions.handleLeftClick(foodPlacer, event.getEntity(), hit)) {
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
        if (be instanceof PlaceMatBlockEntity foodPlacer) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            Vec3 location = event.getHitVec().getLocation().subtract(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());

            // Interaction with existing items.
            PlaceMatBlock.PlacementRange targetedRange = null;
            PlaceMatBlock pmb = null;
            if (foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock block) {
                pmb = block;
                BlockState state = foodPlacer.getBlockState();
                targetedRange = pmb.getTargetedPlacementRange(state, location);
            }

            if (foodPlacer.getTargetedItem(eyePos, lookVec, event.getPos(), targetedRange) != null) {
                if (held.isEmpty()) {
                    return;
                }
            }

            // Placement logic.
            if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
                if (pmb != null) {
                    TagKey<Item> whitelist = targetedRange != null ? targetedRange.whitelistTag() : null;
                    if (whitelist != null && !held.is(whitelist)) {
                        return;
                    }
                }

                if (event.getLevel().isClientSide) {
                    final PlaceMatBlock.PlacementRange[] targetedRangeArr = { targetedRange };
                    if (targetedRangeArr[0] != null) {
                        DefinitionManager.PlaceMatDefinition def = DefinitionManager.getDefinition(held.getItem());
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
                            Vec3 localIntersection = PlaceMatBlock.getLocalHitVec(foodPlacer.getBlockState(), relativeIntersection);
                            placementX = (float) localIntersection.x - (def.size().x * multiplier) / 2f;
                            placementZ = (float) localIntersection.z - (def.size().y * multiplier) / 2f;
                        } else {
                            Vec3 localLocation = PlaceMatBlock.getLocalHitVec(foodPlacer.getBlockState(), location);
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
                                .sendToServer(new PlaceMatPacket(event.getPos(), placementPos, location, foodPlacer.getCurrentRotation(), foodPlacer.getCurrentPitch(),
                                        foodPlacer.getCurrentRoll(), foodPlacer.getCurrentHeight(), count));
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
        if (player == null || mc.level == null)
            return;

        HitResult hit = mc.hitResult;
        boolean isLookingAtPlacemat = false;

        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockEntity blockEntity = mc.level.getBlockEntity(blockHit.getBlockPos());
            if (blockEntity instanceof PlaceMatBlockEntity foodPlacer) {
                isLookingAtPlacemat = true;

                Vec3 eyePos = player.getEyePosition(1.0f);
                Vec3 lookVec = player.getViewVector(1.0f);
                Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                PlaceMatBlock.PlacementRange targetedRange = null;
                if (blockEntity.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                    BlockState state = blockEntity.getBlockState();
                    targetedRange = pmb.getTargetedPlacementRange(state, location);
                }
                PlaceMatBlockEntity.PlacedItem targeted = foodPlacer.getTargetedItem(eyePos, lookVec, blockHit.getBlockPos(), targetedRange);
                ItemStack held = player.getMainHandItem();

                if (targeted != null) {
                    player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.interacting").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                } else if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
                    if (blockEntity.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                        if (foodPlacer.getPlacedItems().size() >= pmb.getContainerSize()) {
                            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.full", pmb.getContainerSize()).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED), true);
                        } else {
                            TagKey<Item> whitelist = targetedRange != null ? targetedRange.whitelistTag() : null;
                            if (whitelist != null && !held.is(whitelist)) {
                                player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.placing").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                            } else {
                                MutableComponent instructions = Component.empty();
                                boolean yawDisabled = targetedRange != null && targetedRange.yawDisabled();
                                boolean pitchDisabled = targetedRange != null && targetedRange.pitchDisabled();
                                boolean rollDisabled = targetedRange != null && targetedRange.rollDisabled();
                                boolean elevationDisabled = targetedRange != null && targetedRange.elevationDisabled();

                                if (!yawDisabled) {
                                    instructions.append(Component.translatable("place_mats.tooltip.placemat.yaw"));
                                }
                                if (!pitchDisabled) {
                                    instructions.append(Component.translatable("place_mats.tooltip.placemat.pitch"));
                                }
                                if (!rollDisabled) {
                                    instructions.append(Component.translatable("place_mats.tooltip.placemat.roll"));
                                }
                                if (!elevationDisabled) {
                                    instructions.append(Component.translatable("place_mats.tooltip.placemat.elevation"));
                                }
                                player.displayClientMessage(instructions.withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                            }
                        }
                    } else {
                        if (foodPlacer.getPlacedItems().size() >= 10) {
                            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.full", 10).withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                        } else {
                            player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.instructions").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                        }
                    }
                } else {
                    player.displayClientMessage(Component.translatable("place_mats.tooltip.placemat.placing").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE), true);
                }
            }
        }

        if (wasLookingAtPlacemat && !isLookingAtPlacemat) {
            player.displayClientMessage(Component.empty(), true);
        }

        wasLookingAtPlacemat = isLookingAtPlacemat;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && Screen.hasShiftDown() && !mc.player.getMainHandItem().isEmpty()) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                assert mc.level != null;
                BlockEntity be = mc.level.getBlockEntity(blockHit.getBlockPos());
                if (be instanceof PlaceMatBlockEntity foodPlacer) {
                    if (mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlaceMatBlock pmb) {
                        Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                        PlaceMatBlock.PlacementRange targetedRange = pmb.getTargetedPlacementRange(mc.level.getBlockState(blockHit.getBlockPos()), location);
                        if (targetedRange != null && (targetedRange.elevationDisabled() || targetedRange.restricted()))
                            return;
                    }
                    float height = foodPlacer.getCurrentHeight();
                    if (mc.options.keyJump.matches(event.getKey(), event.getScanCode())) {
                        if (Screen.hasControlDown()) {
                            height = Math.max(0, height - 0.015625f);
                        } else {
                            height = Math.min(1.0f, height + 0.015625f);
                        }
                        foodPlacer.setCurrentHeight(height);
                    }
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.player.getMainHandItem().isEmpty()) {
                HitResult hit = mc.hitResult;
                if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                    if (mc.level != null && mc.level.getBlockEntity(blockHit.getBlockPos()) instanceof PlaceMatBlockEntity placeMat) {
                        ItemStack held = mc.player.getMainHandItem();
                        if (!held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST)) {
                            if (placeMat.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                                Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                                PlaceMatBlock.PlacementRange targetedRange = pmb.getTargetedPlacementRange(placeMat.getBlockState(), location);
                                TagKey<Item> whitelist = targetedRange != null ? targetedRange.whitelistTag() : null;
                                if (whitelist != null && !held.is(whitelist)) {
                                    return;
                                }
                            }
                            mc.options.keyJump.setDown(false);
                            mc.options.keyShift.setDown(false);
                        }
                    }
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
                if (be instanceof PlaceMatBlockEntity foodPlacer) {
                    PlaceMatBlock pmb = null;
                    PlaceMatBlock.PlacementRange targetedRange = null;
                    if (mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof PlaceMatBlock block) {
                        pmb = block;
                        Vec3 location = blockHit.getLocation().subtract(blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
                        targetedRange = pmb.getTargetedPlacementRange(mc.level.getBlockState(blockHit.getBlockPos()), location);
                    }
                    if (Screen.hasAltDown() && Screen.hasShiftDown()) {
                        if (targetedRange != null && targetedRange.rollDisabled())
                            return;
                        float roll = foodPlacer.getCurrentRoll();
                        roll = (roll + (float) event.getScrollDelta() * 15f) % 360f;
                        if (roll < 0)
                            roll += 360f;
                        foodPlacer.setCurrentRoll(roll);
                    } else if (Screen.hasControlDown()) {
                        if (targetedRange != null && targetedRange.pitchDisabled())
                            return;
                        float pitch = foodPlacer.getCurrentPitch();
                        pitch = (pitch + (float) event.getScrollDelta() * 15f) % 360f;
                        if (pitch < 0)
                            pitch += 360f;
                        foodPlacer.setCurrentPitch(pitch);
                    } else {
                        if (targetedRange != null && targetedRange.yawDisabled())
                            return;
                        float rotation = foodPlacer.getCurrentRotation();
                        rotation = (rotation + (float) event.getScrollDelta() * 15f) % 360f;
                        if (rotation < 0)
                            rotation += 360f;
                        foodPlacer.setCurrentRotation(rotation);
                    }
                    event.setCanceled(true);
                }
            }
        }
    }
}

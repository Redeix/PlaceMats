package net.placemats.client.renderer.blockentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlacementRange;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;
import net.placemats.common.data.resource.DefinitionManager;
import net.placemats.compat.tfc.TFCCompat;

@SuppressWarnings({"deprecation", "unused"})
public class PlaceMatRenderer implements BlockEntityRenderer<PlaceMatBlockEntity> {
    private final ItemRenderer itemRenderer;

    public PlaceMatRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PlaceMatBlockEntity be, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof PlaceMatBlock pmb))
            return;

        DynamicTextureRenderer.renderDynamicBlock(be, poseStack, buffer, packedLight, packedOverlay, this.itemRenderer);

        Direction facing = (pmb.isRotateZones() && state.hasProperty(PlaceMatCardinalBlock.FACING)) ? state.getValue(PlaceMatCardinalBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        if (facing != Direction.NORTH) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        }
        poseStack.translate(-0.5, 0, -0.5);

        for (PlaceMatBlockEntity.PlacedItem placed : be.getPlacedItems()) {
            poseStack.pushPose();

            ItemStack stackToRender = placed.stack;
            PlacementRange range = pmb.getPlacementRanges(state).stream()
                    .filter(r -> Math.abs(placed.baseHeight - r.box().minY) < 0.001f &&
                            placed.pos.x >= r.box().minX && placed.pos.x <= r.box().maxX &&
                            placed.pos.y >= r.box().minZ && placed.pos.y <= r.box().maxZ)
                    .findFirst().orElse(null);

            boolean disableLayFlat = pmb.isDisableLayFlat() || (range != null && range.disableLayFlat());
            boolean disableCustomModels = pmb.isDisableCustomModels() || (range != null && range.disableCustomModels());
            float multiplier = be.getScaleMultiplier(range);
            float elevation = be.getEffectiveElevation(range);
            float[] rotation = be.getEffectiveRotation(range);

            DefinitionManager.PlaceMatDefinition def = be.getEffectiveDefinition(stackToRender, range);
            if (disableCustomModels) {
                def = DefinitionManager.PlaceMatDefinition.DEFAULT(stackToRender.getItem());
            }

            float itemHalfHeight = (def.getItemHeight() * multiplier) / 2.0f;
            float collisionCenterX = (float) placed.pos.x + (def.size().x * multiplier) / 2.0f;
            float collisionCenterY = (float) (placed.baseHeight + placed.getVisualHeight() + elevation) + itemHalfHeight;
            float collisionCenterZ = (float) placed.pos.y + (def.size().y * multiplier) / 2.0f;

            // Transform to 3D centerpoint of item collision shape.
            poseStack.translate(collisionCenterX, collisionCenterY, collisionCenterZ);

            float extraRoll = def.rot() != null ? def.rot().x : 0f;
            float extraPitch = def.rot() != null ? def.rot().y : 0f;
            float extraYaw = def.rot() != null ? def.rot().z : 0f;

            // Rotate around the 3D centerpoint of the collision shape.
            poseStack.mulPose(Axis.XP.rotationDegrees(placed.pitch + rotation[1] + extraPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(placed.roll + rotation[2] + extraRoll));
            poseStack.mulPose(Axis.YP.rotationDegrees(placed.rotation + rotation[0] + extraYaw));

            float effectiveScale = def.scale() * multiplier;
            poseStack.scale(effectiveScale, effectiveScale, effectiveScale);

            // Apply model position offset and move from 3D center down to model base.
            float offsetX = def.pos() != null ? def.pos().x : 0f;
            float offsetY = def.pos() != null ? def.pos().y : 0f;
            float offsetZ = def.pos() != null ? def.pos().z : 0f;
            float localHalfHeight = effectiveScale > 0 ? (itemHalfHeight / effectiveScale) : itemHalfHeight;
            poseStack.translate(offsetX, offsetY - localHalfHeight, offsetZ);

            BakedModel model;
            if (!disableCustomModels && def.modelRotten() != null && TFCCompat.INSTANCE.isRotten(stackToRender)) {
                model = itemRenderer.getItemModelShaper().getModelManager().getModel(def.modelRotten());
                if (model == itemRenderer.getItemModelShaper().getModelManager().getMissingModel()) {
                    model = itemRenderer.getModel(stackToRender, be.getLevel(), null, (int) be.getBlockPos().asLong());
                }
            } else if (!disableCustomModels && def.model() != null) {
                model = itemRenderer.getItemModelShaper().getModelManager().getModel(def.model());
                if (model == itemRenderer.getItemModelShaper().getModelManager().getMissingModel()) {
                    model = itemRenderer.getModel(stackToRender, be.getLevel(), null, (int) be.getBlockPos().asLong());
                }
            } else {
                model = itemRenderer.getModel(stackToRender, be.getLevel(), null, (int) be.getBlockPos().asLong());
            }
            if (!disableCustomModels) {
                model = getTransformedModel(model, def, stackToRender);
            }

            var fixedTransform = model.getTransforms().getTransform(ItemDisplayContext.FIXED);

            if (!disableLayFlat && def.flat()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                // Counteract model specific fixed translations.
                poseStack.translate(-fixedTransform.translation.x() / 16.0, -fixedTransform.translation.y() / 16.0, -fixedTransform.translation.z() / 16.0);
                // Move so it sits on the surface.
                poseStack.translate(0, 0, -0.0625 * fixedTransform.scale.z());
            } else {
                poseStack.translate(-fixedTransform.translation.x() / 16.0, -fixedTransform.translation.y() / 16.0, -fixedTransform.translation.z() / 16.0);
                if (model.isGui3d()) {
                    // 3D models are centered at 0.5. Move up by half their height.
                    poseStack.translate(0, 0.5 * fixedTransform.scale.y(), 0);
                }
            }

            itemRenderer.render(stackToRender, ItemDisplayContext.FIXED, false, poseStack, buffer, packedLight, packedOverlay, model);

            poseStack.popPose();
        }
        poseStack.popPose();
    }

    public static void renderBounds(PoseStack poseStack, MultiBufferSource buffer, PlacementRange range, float r, float g, float b) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, range.box(), r, g, b, 1.0f);
    }

    public static void renderItemOutline(PoseStack poseStack, MultiBufferSource buffer, PlacedItem placed, float r, float g, float b) {
        AABB box = placed.getVisualBox();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, r, g, b, 1.0f);
    }

    @SuppressWarnings("deprecation")
    public static void renderPreview(PoseStack poseStack, MultiBufferSource buffer, Vec2 pos, Vec2 size, float scale, float rotation, float pitch, float roll, float height, boolean valid,
            ItemStack stack, ItemRenderer itemRenderer, PlacementRange targetRange, PlaceMatBlock pmb) {
        poseStack.pushPose();

        boolean disableLayFlat = pmb.isDisableLayFlat() || targetRange.disableLayFlat();
        boolean disableCustomModels = pmb.isDisableCustomModels() || targetRange.disableCustomModels();
        float multiplier = pmb.getScaleMultiplier() * targetRange.scaleMultiplier();

        // Render ghost item.
        poseStack.pushPose();
        double yMin = targetRange.box().minY;
        float elevation = pmb.getDefaultElevation() + targetRange.defaultElevation();
        float yawOffset = pmb.getDefaultYaw() + targetRange.defaultYaw();
        float pitchOffset = pmb.getDefaultPitch() + targetRange.defaultPitch();
        float rollOffset = pmb.getDefaultRoll() + targetRange.defaultRoll();

        DefinitionManager.PlaceMatDefinition def = DefinitionManager.getEffectiveDefinition(stack, pmb.defaultBlockState(), targetRange);
        if (disableCustomModels) {
            def = DefinitionManager.PlaceMatDefinition.DEFAULT(stack.getItem());
        }

        float itemHalfHeight = (def.getItemHeight() * multiplier) / 2.0f;
        float collisionCenterX = (float) pos.x + (size.x * multiplier) / 2.0f;
        float collisionCenterY = (float) (yMin + height + elevation) + itemHalfHeight;
        float collisionCenterZ = (float) pos.y + (size.y * multiplier) / 2.0f;

        // Transform to 3D centerpoint of item collision shape.
        poseStack.translate(collisionCenterX, collisionCenterY, collisionCenterZ);

        float extraRoll = def.rot() != null ? def.rot().x : 0f;
        float extraPitch = def.rot() != null ? def.rot().y : 0f;
        float extraYaw = def.rot() != null ? def.rot().z : 0f;

        // Rotate around the 3D centerpoint of the collision shape.
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch + pitchOffset + extraPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll + rollOffset + extraRoll));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation + yawOffset + extraYaw));

        float effectiveScale = scale * multiplier;
        poseStack.scale(effectiveScale, effectiveScale, effectiveScale);

        // Apply model position offset and move from 3D center down to model base.
        float offsetX = def.pos() != null ? def.pos().x : 0f;
        float offsetY = def.pos() != null ? def.pos().y : 0f;
        float offsetZ = def.pos() != null ? def.pos().z : 0f;
        float localHalfHeight = effectiveScale > 0 ? (itemHalfHeight / effectiveScale) : itemHalfHeight;
        poseStack.translate(offsetX, offsetY - localHalfHeight, offsetZ);

        BakedModel model;
        if (!disableCustomModels && def.modelRotten() != null && TFCCompat.INSTANCE.isRotten(stack)) {
            model = itemRenderer.getItemModelShaper().getModelManager().getModel(def.modelRotten());
            if (model == itemRenderer.getItemModelShaper().getModelManager().getMissingModel()) {
                model = itemRenderer.getModel(stack, null, null, 0);
            }
        } else if (!disableCustomModels && def.model() != null) {
            model = itemRenderer.getItemModelShaper().getModelManager().getModel(def.model());
            if (model == itemRenderer.getItemModelShaper().getModelManager().getMissingModel()) {
                model = itemRenderer.getModel(stack, null, null, 0);
            }
        } else {
            model = itemRenderer.getModel(stack, null, null, 0);
        }
        if (!disableCustomModels) {
            model = getTransformedModel(model, def, stack);
        }

        var fixedTransform = model.getTransforms().getTransform(ItemDisplayContext.FIXED);

        if (!disableLayFlat && def.flat()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            // Counteract model specific fixed translation.
            poseStack.translate(-fixedTransform.translation.x() / 16.0, -fixedTransform.translation.y() / 16.0, -fixedTransform.translation.z() / 16.0);
            // Move so it sits on the surface.
            poseStack.translate(0, 0, -0.0625 * fixedTransform.scale.z());
        } else {
            poseStack.translate(-fixedTransform.translation.x() / 16.0, -fixedTransform.translation.y() / 16.0, -fixedTransform.translation.z() / 16.0);
            if (model.isGui3d()) {
                poseStack.translate(0, 0.5 * fixedTransform.scale.y(), 0);
            }
        }

        itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, model);
        poseStack.popPose();

        // Render bounding box.
        float y = (float) yMin + height + elevation;
        AABB box = new AABB(pos.x, y, pos.y, (double) pos.x + size.x * multiplier, (double) y + def.getItemHeight() * multiplier, (double) pos.y + size.y * multiplier);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        float r = valid ? 0 : 1;
        float g = valid ? 1 : 0;
        float b = 0;
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, r, g, b, 1.0f);

        // Render snap box locked to the bottom.
        AABB snapBox = new AABB(pos.x, yMin, pos.y, (double) pos.x + size.x * multiplier, yMin + 0.01, (double) pos.y + size.y * multiplier);
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, snapBox, r, g, b, 0.5f);

        poseStack.popPose();
    }

    public static TextureAtlasSprite getBlockAtlasSprite(ResourceLocation textureLocation) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLocation);
    }

    public static void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, AABB box, float borderR, float borderG, float borderB, float borderA, int packedLight, TextureAtlasSprite sprite) {
        renderOverlay(poseStack, buffer, box, borderR, borderG, borderB, borderA, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, sprite);
    }

    public static void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, AABB box, float borderR, float borderG, float borderB, float borderA, float faceR, float faceG, float faceB, float faceA, int packedLight, TextureAtlasSprite sprite) {
        if (borderA > 0.0f) {
            VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
            LevelRenderer.renderLineBox(poseStack, lineConsumer, box, borderR, borderG, borderB, borderA);
        }

        if (sprite != null && faceA > 0.0f) {
            VertexConsumer faceConsumer = buffer.getBuffer(RenderType.translucent());
            renderFaces(poseStack, faceConsumer, box, sprite, faceR, faceG, faceB, faceA, packedLight);
        }
    }

    public static void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, AABB box, float borderR, float borderG, float borderB, float borderA, int packedLight, ResourceLocation textureLocation) {
        TextureAtlasSprite sprite = textureLocation != null ? getBlockAtlasSprite(textureLocation) : null;
        renderOverlay(poseStack, buffer, box, borderR, borderG, borderB, borderA, packedLight, sprite);
    }

    public static void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, VoxelShape shape, float borderR, float borderG, float borderB, float borderA, int packedLight, TextureAtlasSprite sprite) {
        for (AABB box : shape.toAabbs()) {
            renderOverlay(poseStack, buffer, box, borderR, borderG, borderB, borderA, packedLight, sprite);
        }
    }

    public static void renderOverlay(PoseStack poseStack, MultiBufferSource buffer, VoxelShape shape, float borderR, float borderG, float borderB, float borderA, int packedLight, ResourceLocation textureLocation) {
        for (AABB box : shape.toAabbs()) {
            renderOverlay(poseStack, buffer, box, borderR, borderG, borderB, borderA, packedLight, textureLocation);
        }
    }

    public static void renderFaces(PoseStack poseStack, VertexConsumer consumer, AABB box, TextureAtlasSprite sprite, float r, float g, float b, float a, int packedLight) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Down
        consumer.vertex(pose, minX, minY, minZ).color(r, g, b, a).uv(sprite.getU(minX * 16.0), sprite.getV(minZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose, maxX, minY, minZ).color(r, g, b, a).uv(sprite.getU(maxX * 16.0), sprite.getV(minZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose, maxX, minY, maxZ).color(r, g, b, a).uv(sprite.getU(maxX * 16.0), sprite.getV(maxZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose, minX, minY, maxZ).color(r, g, b, a).uv(sprite.getU(minX * 16.0), sprite.getV(maxZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();

        // Up
        consumer.vertex(pose, minX, maxY, maxZ).color(r, g, b, a).uv(sprite.getU(minX * 16.0), sprite.getV(maxZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, maxX, maxY, maxZ).color(r, g, b, a).uv(sprite.getU(maxX * 16.0), sprite.getV(maxZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, maxX, maxY, minZ).color(r, g, b, a).uv(sprite.getU(maxX * 16.0), sprite.getV(minZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, minX, maxY, minZ).color(r, g, b, a).uv(sprite.getU(minX * 16.0), sprite.getV(minZ * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();

        // North
        consumer.vertex(pose, minX, maxY, minZ).color(r, g, b, a).uv(sprite.getU(minX * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, maxX, maxY, minZ).color(r, g, b, a).uv(sprite.getU(maxX * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, maxX, minY, minZ).color(r, g, b, a).uv(sprite.getU(maxX * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, minX, minY, minZ).color(r, g, b, a).uv(sprite.getU(minX * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();

        // South
        consumer.vertex(pose, maxX, maxY, maxZ).color(r, g, b, a).uv(sprite.getU((1.0 - maxX) * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, minX, maxY, maxZ).color(r, g, b, a).uv(sprite.getU((1.0 - minX) * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, minX, minY, maxZ).color(r, g, b, a).uv(sprite.getU((1.0 - minX) * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, maxX, minY, maxZ).color(r, g, b, a).uv(sprite.getU((1.0 - maxX) * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();

        // West
        consumer.vertex(pose, minX, maxY, maxZ).color(r, g, b, a).uv(sprite.getU((1.0 - maxZ) * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, minX, maxY, minZ).color(r, g, b, a).uv(sprite.getU((1.0 - minZ) * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, minX, minY, minZ).color(r, g, b, a).uv(sprite.getU((1.0 - minZ) * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, minX, minY, maxZ).color(r, g, b, a).uv(sprite.getU((1.0 - maxZ) * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();

        // East
        consumer.vertex(pose, maxX, maxY, minZ).color(r, g, b, a).uv(sprite.getU(minZ * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, maxX, maxY, maxZ).color(r, g, b, a).uv(sprite.getU(maxZ * 16.0), sprite.getV((1.0 - maxY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, maxX, minY, maxZ).color(r, g, b, a).uv(sprite.getU(maxZ * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, maxX, minY, minZ).color(r, g, b, a).uv(sprite.getU(minZ * 16.0), sprite.getV((1.0 - minY) * 16.0)).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
    }

    public static FluidStack getContainedFluid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FluidStack.EMPTY;

        ItemStack single = stack.getCount() == 1 ? stack : stack.copyWithCount(1);

        // TFC compat.
        try {
            FluidStack tfcFluid = TFCCompat.INSTANCE.getContainedFluid(single);
            if (!tfcFluid.isEmpty()) {
                return tfcFluid;
            }
        } catch (Exception ignored) {}

        // FluidUtil.getFluidContained
        try {
            Optional<FluidStack> contained = FluidUtil.getFluidContained(single);
            if (contained.isPresent() && !contained.get().isEmpty()) {
                return contained.get();
            }
        } catch (Exception ignored) {}

        // FLUID_HANDLER_ITEM capability.
        try {
            LazyOptional<IFluidHandlerItem> cap = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (cap.isPresent()) {
                FluidStack inTank = cap.map(h -> {
                    for (int i = 0; i < h.getTanks(); i++) {
                        FluidStack f = h.getFluidInTank(i);
                        if (!f.isEmpty()) return f;
                    }
                    return FluidStack.EMPTY;
                }).orElse(FluidStack.EMPTY);
                if (!inTank.isEmpty()) return inTank;
            }
        } catch (Exception ignored) {}

        // FLUID_HANDLER capability.
        try {
            var cap = single.getCapability(ForgeCapabilities.FLUID_HANDLER);
            if (cap.isPresent()) {
                FluidStack inTank = cap.map(h -> {
                    for (int i = 0; i < h.getTanks(); i++) {
                        FluidStack f = h.getFluidInTank(i);
                        if (!f.isEmpty()) return f;
                    }
                    return FluidStack.EMPTY;
                }).orElse(FluidStack.EMPTY);
                if (!inTank.isEmpty()) return inTank;
            }
        } catch (Exception ignored) {}

        // BucketItem.
        if (stack.getItem() instanceof BucketItem bucket && bucket.getFluid() != Fluids.EMPTY) {
            return new FluidStack(bucket.getFluid(), 1000);
        }

        // MilkBucketItem.
        if (stack.getItem() instanceof MilkBucketItem || stack.getItem() == Items.MILK_BUCKET) {
            try {
                return new FluidStack(ForgeMod.MILK.get(), 1000);
            } catch (Exception ignored) {}
        }

        return FluidStack.EMPTY;
    }

    public static BakedModel getTransformedModel(BakedModel model, DefinitionManager.PlaceMatDefinition def, ItemStack stack) {
        boolean hasTint = def.tintColors() != null && !def.tintColors().isEmpty();
        boolean hasFluid = def.fluidTextureIndex() != null;
        boolean ignoreTints = def.ignoreItemTints();
        if (!hasTint && !hasFluid && !ignoreTints) {
            return model;
        }

        FluidStack fluidStack = hasFluid ? getContainedFluid(stack) : FluidStack.EMPTY;
        TextureAtlasSprite fluidSprite = null;
        int fluidTintColor = -1;
        if (!fluidStack.isEmpty()) {
            Fluid fluid = fluidStack.getFluid();
            IClientFluidTypeExtensions clientExt = IClientFluidTypeExtensions.of(fluid);
            ResourceLocation still = clientExt.getStillTexture(fluidStack);
            if (still == null) {
                still = clientExt.getStillTexture();
            }
            if (still != null) {
                fluidSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
                fluidTintColor = clientExt.getTintColor(fluidStack);
                if (fluidTintColor == -1 || (fluidTintColor & 0xFF000000) == 0) {
                    int defTint = clientExt.getTintColor();
                    if (defTint != -1) {
                        fluidTintColor = defTint;
                    }
                }
                if (fluidTintColor != -1 && (fluidTintColor & 0xFF000000) == 0) {
                    fluidTintColor |= 0xFF000000;
                }
            }
        }

        final TextureAtlasSprite finalFluidSprite = fluidSprite;
        final int finalFluidTintColor = fluidTintColor;
        final String fluidTextureIndex = def.fluidTextureIndex();
        final Map<Integer, Integer> tintColors = def.tintColors();
        final boolean isFluidContainerEmpty = hasFluid && fluidStack.isEmpty();
        final boolean finalIgnoreTints = ignoreTints;

        return new BakedModelWrapper<>(model) {
            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
                List<BakedQuad> original = super.getQuads(state, side, rand);
                List<BakedQuad> result = new ArrayList<>(original.size());
                for (BakedQuad quad : original) {
                    BakedQuad transformed = transformQuad(quad, fluidTextureIndex, finalFluidSprite, finalFluidTintColor, tintColors, isFluidContainerEmpty, finalIgnoreTints);
                    if (transformed != null) {
                        result.add(transformed);
                    }
                }
                return result;
            }

            @Override
            public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
                List<BakedQuad> original = this.originalModel.getQuads(state, side, rand, extraData, renderType);
                List<BakedQuad> result = new ArrayList<>(original.size());
                for (BakedQuad quad : original) {
                    BakedQuad transformed = transformQuad(quad, fluidTextureIndex, finalFluidSprite, finalFluidTintColor, tintColors, isFluidContainerEmpty, finalIgnoreTints);
                    if (transformed != null) {
                        result.add(transformed);
                    }
                }
                return result;
            }

            @Override
            public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
                return List.of(this);
            }
        };
    }

    private static boolean matchesFluidIndex(BakedQuad quad, @Nullable String fluidTextureIndex) {
        if (fluidTextureIndex == null) return false;
        String cleanIndex = fluidTextureIndex.startsWith("#") ? fluidTextureIndex.substring(1) : fluidTextureIndex;
        try {
            int idx = Integer.parseInt(cleanIndex);
            if (quad.getTintIndex() == idx) {
                return true;
            }
        } catch (NumberFormatException ignored) {}
        String spriteName = quad.getSprite().contents().name().toString();
        return spriteName.equals(fluidTextureIndex) || spriteName.contains(fluidTextureIndex)
                || spriteName.equals(cleanIndex) || spriteName.contains(cleanIndex)
                || spriteName.endsWith("/" + cleanIndex) || spriteName.endsWith(":" + cleanIndex);
    }

    private static @Nullable BakedQuad transformQuad(
            BakedQuad quad,
            @Nullable String fluidTextureIndex,
            @Nullable TextureAtlasSprite fluidSprite,
            int fluidTintColor,
            @Nullable Map<Integer, Integer> tintColors,
            boolean isFluidContainerEmpty,
            boolean ignoreItemTints) {

        boolean isFluidMatch = matchesFluidIndex(quad, fluidTextureIndex);

        if (isFluidMatch) {
            if (fluidSprite != null) {
                return remapSprite(quad, fluidSprite, fluidTintColor);
            } else if (isFluidContainerEmpty) {
                // If the item has a fluidTexture definition but currently contains no fluid ignore the fluid quad.
                return null;
            }
        }

        if (tintColors != null && tintColors.containsKey(quad.getTintIndex())) {
            int color = tintColors.get(quad.getTintIndex());
            return applyQuadColor(quad, color);
        }

        if (ignoreItemTints && quad.isTinted()) {
            return stripQuadTint(quad);
        }

        return quad;
    }

    private static BakedQuad stripQuadTint(BakedQuad quad) {
        return new BakedQuad(quad.getVertices(), -1, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static BakedQuad remapSprite(BakedQuad quad, TextureAtlasSprite newSprite, int tintColor) {
        TextureAtlasSprite oldSprite = quad.getSprite();
        int[] v = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
        int stride = IQuadTransformer.STRIDE;

        float origU0 = oldSprite.getU0();
        float origV0 = oldSprite.getV0();
        float origW = oldSprite.getU1() - origU0;
        float origH = oldSprite.getV1() - origV0;

        float newU0 = newSprite.getU0();
        float newV0 = newSprite.getV0();
        float newW = newSprite.getU1() - newU0;
        float newH = newSprite.getV1() - newV0;

        for (int i = 0; i < 4; i++) {
            int offset = i * stride + IQuadTransformer.UV0;
            float u = Float.intBitsToFloat(v[offset]);
            float vCoord = Float.intBitsToFloat(v[offset + 1]);

            float normU = origW > 0 ? (u - origU0) / origW : 0;
            float normV = origH > 0 ? (vCoord - origV0) / origH : 0;

            float targetU = newU0 + normU * newW;
            float targetV = newV0 + normV * newH;

            v[offset] = Float.floatToRawIntBits(targetU);
            v[offset + 1] = Float.floatToRawIntBits(targetV);

            if (tintColor != -1) {
                int abgr = QuadTransformers.toABGR(tintColor);
                v[i * stride + IQuadTransformer.COLOR] = abgr;
            }
        }

        return new BakedQuad(v, -1, quad.getDirection(), newSprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static BakedQuad applyQuadColor(BakedQuad quad, int argbColor) {
        int[] v = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
        int stride = IQuadTransformer.STRIDE;
        int abgr = QuadTransformers.toABGR(argbColor);
        for (int i = 0; i < 4; i++) {
            v[i * stride + IQuadTransformer.COLOR] = abgr;
        }
        return new BakedQuad(v, -1, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }
}

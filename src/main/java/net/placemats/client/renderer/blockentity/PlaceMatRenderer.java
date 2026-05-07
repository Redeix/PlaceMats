package net.placemats.client.renderer.blockentity;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.placemats.compat.tfc.TFCCompat;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;
import net.placemats.common.data.resource.DefinitionManager;

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
        Direction facing = state.hasProperty(PlaceMatBlock.FACING) ? state.getValue(PlaceMatBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        if (facing != Direction.NORTH) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        }
        poseStack.translate(-0.5, 0, -0.5);

        for (PlaceMatBlockEntity.PlacedItem placed : be.getPlacedItems()) {
            poseStack.pushPose();

            ItemStack stackToRender = placed.stack;
            PlaceMatBlock.PlacementRange range = pmb.getPlacementRanges().stream()
                    .filter(r -> Math.abs(placed.baseHeight - r.box().minY) < 0.001f &&
                            placed.pos.x >= r.box().minX && placed.pos.x <= r.box().maxX &&
                            placed.pos.y >= r.box().minZ && placed.pos.y <= r.box().maxZ)
                    .findFirst().orElse(null);

            boolean disableLayFlat = pmb.isDisableLayFlat() || (range != null && range.disableLayFlat());
            boolean disableCustomModels = pmb.isDisableCustomModels() || (range != null && range.disableCustomModels());
            float multiplier = be.getScaleMultiplier(range);
            float elevation = be.getEffectiveElevation(range);
            float[] rotation = be.getEffectiveRotation(range);

            DefinitionManager.PlaceMatDefinition def = DefinitionManager.getDefinition(stackToRender.getItem());
            if (disableCustomModels) {
                def = DefinitionManager.PlaceMatDefinition.DEFAULT(stackToRender.getItem());
            }

            // Transform to item's position.
            poseStack.translate(placed.pos.x + (def.size().x * multiplier) / 2.0, placed.baseHeight + placed.getVisualHeight() + elevation, placed.pos.y + (def.size().y * multiplier) / 2.0);

            // Scale and rotate.
            poseStack.mulPose(Axis.XP.rotationDegrees(placed.pitch + rotation[1]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(placed.roll + rotation[2]));
            poseStack.mulPose(Axis.YP.rotationDegrees(placed.rotation + rotation[0]));

            float effectiveScale = def.scale() * multiplier;
            poseStack.scale(effectiveScale, effectiveScale, effectiveScale);

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

    public static void renderBounds(PoseStack poseStack, MultiBufferSource buffer, PlaceMatBlock.PlacementRange range, float r, float g, float b) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, range.box(), r, g, b, 1.0f);
    }

    public static void renderItemOutline(PoseStack poseStack, MultiBufferSource buffer, PlacedItem placed, float r, float g, float b) {
        AABB box = placed.getVisualBox();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, r, g, b, 1.0f);
    }

    public static void renderPreview(PoseStack poseStack, MultiBufferSource buffer, Vec2 pos, Vec2 size, float scale, float rotation, float pitch, float roll, float height, boolean valid,
            ItemStack stack, ItemRenderer itemRenderer, PlaceMatBlock.PlacementRange targetRange, PlaceMatBlock pmb) {
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

        poseStack.translate(pos.x + (size.x * multiplier) / 2.0, yMin + height + elevation, pos.y + (size.y * multiplier) / 2.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch + pitchOffset));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll + rollOffset));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation + yawOffset));

        float effectiveScale = scale * multiplier;
        poseStack.scale(effectiveScale, effectiveScale, effectiveScale);

        DefinitionManager.PlaceMatDefinition def = DefinitionManager.getDefinition(stack.getItem());
        if (disableCustomModels) {
            def = DefinitionManager.PlaceMatDefinition.DEFAULT(stack.getItem());
        }

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
}

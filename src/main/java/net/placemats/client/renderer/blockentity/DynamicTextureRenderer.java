package net.placemats.client.renderer.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.placemats.client.renderer.DynamicTexturedBakedModel;
import net.placemats.common.blockentity.IDynamicTexturedBlockEntity;

/**
 * Utility class providing rendering logic for dynamic textured and colored blocks.
 */
public final class DynamicTextureRenderer {

    private DynamicTextureRenderer() {}

    /**
     * Renders dynamic animated block model quads (ex. rainbow cycling) for block entities implementing {@link IDynamicTexturedBlockEntity}.
     */
    public static void renderDynamicBlock(
            BlockEntity be,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            ItemRenderer itemRenderer) {

        if (be instanceof IDynamicTexturedBlockEntity dtbe && (
            dtbe.hasRainbowColor()
                || dtbe.hasMultiColor()
        )) {
            BlockState state = be.getBlockState();
            BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            if (blockModel instanceof DynamicTexturedBakedModel dtbm) {
                BlockState materialState = dtbe.getDynamicMaterial();
                BakedModel baseModel = dtbm.getOriginalModel();
                renderDynamicQuads(
                        baseModel,
                        dtbm,
                        state,
                        materialState,
                        dtbe.getDynamicTextures(),
                        dtbe.getDynamicColors(),
                        be.getModelData(),
                        poseStack,
                        buffer,
                        packedLight,
                        packedOverlay,
                        itemRenderer
                );
            }
        }
    }

    /**
     * Renders dynamic remapped quads for a block model and texture/color configuration.
     */
    public static void renderDynamicQuads(
            BakedModel originalModel,
            DynamicTexturedBakedModel dynamicModel,
            BlockState state,
            Map<String, ResourceLocation> textures,
            Map<String, Integer> colors,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            ItemRenderer itemRenderer) {
        renderDynamicQuads(originalModel, dynamicModel, state, null, textures, colors, ModelData.EMPTY, poseStack, buffer, packedLight, packedOverlay, itemRenderer);
    }

    /**
     * Renders dynamic remapped quads for a block model, material state, and texture/color configuration.
     */
    public static void renderDynamicQuads(
            BakedModel originalModel,
            DynamicTexturedBakedModel dynamicModel,
            BlockState state,
            BlockState materialState,
            Map<String, ResourceLocation> textures,
            Map<String, Integer> colors,
            ModelData modelData,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay,
            ItemRenderer itemRenderer) {

        RandomSource rand = RandomSource.create(42L);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction side : Direction.values()) {
            quads.addAll(originalModel.getQuads(state, side, rand, modelData, null));
        }
        quads.addAll(originalModel.getQuads(state, null, rand, modelData, null));

        List<BakedQuad> remapped = dynamicModel.remapQuads(quads, materialState, textures, colors, modelData, rand, null);
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        itemRenderer.renderQuadList(poseStack, consumer, remapped, ItemStack.EMPTY, packedLight, packedOverlay);
    }
}

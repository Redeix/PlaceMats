package net.placemats.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.placemats.common.blockentity.IDynamicTexturedBlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * General BlockEntityRenderer for any block entity implementing {@link IDynamicTexturedBlockEntity}.
 */
@SuppressWarnings("unused")
public class DynamicBlockEntityRenderer<T extends BlockEntity & IDynamicTexturedBlockEntity> implements BlockEntityRenderer<T> {

    protected final ItemRenderer itemRenderer;

    public DynamicBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            @NotNull T blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {

        DynamicTextureRenderer.renderDynamicBlock(blockEntity, poseStack, buffer, packedLight, packedOverlay, this.itemRenderer);
    }
}

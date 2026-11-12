package net.placemats.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.placemats.common.data.PlaceMatTags;
import org.jetbrains.annotations.NotNull;

/**
 * Item model that cycles through texture layers 0-4.
 */
public class CyclicLayerBakedModel extends BakedModelWrapper<BakedModel> {

    private static final int TICKS_PER_FRAME = 20;
    private static final int ANIMATED_LAYER_COUNT = 5;
    private static final int STATIC_OVERLAY_LAYER = 5;

    private final Item item;

    public CyclicLayerBakedModel(BakedModel originalModel, Item item) {
        super(originalModel);
        this.item = item;
    }

    public BakedModel getOriginalModel() {
        return this.originalModel;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        List<BakedQuad> allQuads = super.getQuads(state, side, rand);
        return filterQuads(allQuads);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> allQuads = super.getQuads(state, side, rand, extraData, renderType);
        return filterQuads(allQuads);
    }

    @SuppressWarnings("deprecation")
    private List<BakedQuad> filterQuads(List<BakedQuad> allQuads) {
        if (!item.builtInRegistryHolder().is(PlaceMatTags.Items.TEXTURE_PROVIDERS)) {
            return allQuads;
        }

        Minecraft mc = Minecraft.getInstance();
        long time = mc.level != null ? mc.level.getGameTime() : System.currentTimeMillis() / 50L;
        int activeBaseLayer = (int) ((time / TICKS_PER_FRAME) % ANIMATED_LAYER_COUNT);

        List<BakedQuad> filtered = new ArrayList<>(allQuads.size());
        for (BakedQuad quad : allQuads) {
            int layer = quad.getTintIndex();
            if (layer == activeBaseLayer || layer == STATIC_OVERLAY_LAYER) {
                filtered.add(quad);
            }
        }
        return filtered;
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext cameraTransformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        BakedModel model = this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        return model == this.originalModel ? this : new CyclicLayerBakedModel(model, this.item);
    }

    @Override
    public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
        return Collections.singletonList(this);
    }
}

package net.placemats.client.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.placemats.client.gui.CabinetMakerTableScreen;
import net.placemats.common.block.IDynamicTexturedBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DynamicShelfBakedModel extends BakedModelWrapper<BakedModel> {

    public static final ModelProperty<Map<String, ResourceLocation>> DYNAMIC_TEXTURES_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Map<String, Integer>> DYNAMIC_COLORS_PROPERTY = new ModelProperty<>();

    @Getter private final IDynamicTexturedBlock dynamicBlock;
    private final ItemOverrides itemOverrides;

    public BakedModel getOriginalModel() {
        return this.originalModel;
    }

    public DynamicShelfBakedModel(BakedModel originalModel, IDynamicTexturedBlock dynamicBlock) {
        super(originalModel);
        this.dynamicBlock = dynamicBlock;
        this.itemOverrides = new DynamicShelfItemOverrides();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return super.getQuads(state, side, rand);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
        Map<String, ResourceLocation> customTextures = extraData.get(DYNAMIC_TEXTURES_PROPERTY);
        Map<String, Integer> customColors = extraData.get(DYNAMIC_COLORS_PROPERTY);
        if (customColors != null && customColors.values().stream().anyMatch(IDynamicTexturedBlock::isRainbowColor)) {
            return Collections.emptyList();
        }
        if ((customTextures == null || customTextures.isEmpty()) && (customColors == null || customColors.isEmpty())) {
            return quads;
        }
        return remapQuads(quads, customTextures, customColors);
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext cameraTransformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        BakedModel model = this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        return model == this.originalModel ? this : new DynamicShelfBakedModel(model, this.dynamicBlock);
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return itemOverrides;
    }

    private class DynamicShelfItemOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            Map<String, ResourceLocation> customTextures = IDynamicTexturedBlock.getDynamicTextures(stack);
            Map<String, Integer> customColors = IDynamicTexturedBlock.getDynamicColors(stack);
            if (customTextures.isEmpty() && customColors.isEmpty()) {
                return DynamicShelfBakedModel.this;
            }
            return new DynamicShelfItemBakedModel(DynamicShelfBakedModel.this, customTextures, customColors);
        }
    }

    public List<BakedQuad> remapQuads(List<BakedQuad> quads, Map<String, ResourceLocation> customTextures) {
        return remapQuads(quads, customTextures, Collections.emptyMap());
    }

    public List<BakedQuad> remapQuads(List<BakedQuad> quads, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
        if (quads.isEmpty() || ((customTextures == null || customTextures.isEmpty()) && (customColors == null || customColors.isEmpty()))) {
            return quads;
        }
        List<BakedQuad> remapped = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            remapped.add(remapQuad(quad, customTextures, customColors));
        }
        return remapped;
    }

    public BakedQuad remapQuad(BakedQuad quad, Map<String, ResourceLocation> customTextures) {
        return remapQuad(quad, customTextures, Collections.emptyMap());
    }

    public BakedQuad remapQuad(BakedQuad quad, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
        String matchedKey = findMatchingKey(quad);
        if (matchedKey != null) {
            ResourceLocation targetTex = customTextures != null ? customTextures.get(matchedKey) : null;
            Integer tintColor = customColors != null ? customColors.get(matchedKey) : null;
            int tint = tintColor != null ? tintColor : -1;
            if (IDynamicTexturedBlock.isRainbowColor(tint)) {
                tint = CabinetMakerTableScreen.getRainbowColor();
            }

            if (targetTex != null) {
                TextureAtlasSprite newSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(targetTex);
                if (newSprite != null) {
                    return remapSprite(quad, newSprite, tint);
                }
            } else if (tint != -1) {
                return remapSprite(quad, quad.getSprite(), tint);
            }
        }
        return quad;
    }

    @Nullable
    private String findMatchingKey(BakedQuad quad) {
        TextureAtlasSprite quadSprite = quad.getSprite();
        ResourceLocation spriteLoc = quadSprite.contents().name();

        List<String> keys = dynamicBlock.getTextureKeys();
        if (keys.size() == 1) {
            return keys.get(0);
        }

        for (String key : keys) {
            ResourceLocation defaultTex = dynamicBlock.getDefaultTexture(key);
            if (defaultTex.equals(spriteLoc)) {
                return key;
            }
            if (defaultTex.getPath().equals(spriteLoc.getPath()) || spriteLoc.getPath().endsWith(defaultTex.getPath())) {
                return key;
            }
        }

        return null;
    }

    public static BakedQuad remapSprite(BakedQuad quad, TextureAtlasSprite newSprite, int tintColor) {
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

    public static class DynamicShelfItemBakedModel extends BakedModelWrapper<BakedModel> {
        @Getter private final DynamicShelfBakedModel parent;
        @Getter private final Map<String, ResourceLocation> customTextures;
        @Getter private final Map<String, Integer> customColors;

        public DynamicShelfItemBakedModel(DynamicShelfBakedModel parent, Map<String, ResourceLocation> customTextures) {
            this(parent, customTextures, Collections.emptyMap());
        }

        public DynamicShelfItemBakedModel(DynamicShelfBakedModel parent, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
            super(parent.originalModel);
            this.parent = parent;
            this.customTextures = customTextures != null ? customTextures : Collections.emptyMap();
            this.customColors = customColors != null ? customColors : Collections.emptyMap();
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            List<BakedQuad> quads = super.getQuads(state, side, rand);
            return parent.remapQuads(quads, customTextures, customColors);
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
            List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, null);
            return parent.remapQuads(quads, customTextures, customColors);
        }

        @Override
        public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext cameraTransformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
            BakedModel model = this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
            return model == this.originalModel ? this : new DynamicShelfItemBakedModel(new DynamicShelfBakedModel(model, this.parent.getDynamicBlock()), this.customTextures, this.customColors);
        }

        @Override
        public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
            return Collections.singletonList(this);
        }
    }
}

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
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.placemats.client.gui.CabinetMakerTableScreen;
import net.placemats.common.block.IDynamicTexturedBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baked model wrapper that dynamically remaps textures and colors
 * for any block implementing {@link IDynamicTexturedBlock}.
 */
@SuppressWarnings("unused")
public class DynamicTexturedBakedModel extends BakedModelWrapper<BakedModel> {

    public static final ModelProperty<Map<String, ResourceLocation>> DYNAMIC_TEXTURES_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Map<String, Integer>> DYNAMIC_COLORS_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<BlockState> MATERIAL_STATE_PROPERTY = new ModelProperty<>();

    @Getter private final IDynamicTexturedBlock dynamicBlock;
    private final ItemOverrides itemOverrides;

    public BakedModel getOriginalModel() {
        return this.originalModel;
    }

    public DynamicTexturedBakedModel(BakedModel originalModel, IDynamicTexturedBlock dynamicBlock) {
        super(originalModel);
        this.dynamicBlock = dynamicBlock;
        this.itemOverrides = new DynamicTexturedItemOverrides();
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        BlockState material = modelData.get(MATERIAL_STATE_PROPERTY);
        ModelData resolvedData = modelData;
        if (material != null) {
            BakedModel materialModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(material);
            ModelData matData = materialModel.getModelData(level, pos, material, modelData);
            if (matData != ModelData.EMPTY) {
                ModelData.Builder builder = matData.derive();
                if (modelData.has(DYNAMIC_TEXTURES_PROPERTY) && !matData.has(DYNAMIC_TEXTURES_PROPERTY)) {
                    builder.with(DYNAMIC_TEXTURES_PROPERTY, modelData.get(DYNAMIC_TEXTURES_PROPERTY));
                }
                if (modelData.has(DYNAMIC_COLORS_PROPERTY) && !matData.has(DYNAMIC_COLORS_PROPERTY)) {
                    builder.with(DYNAMIC_COLORS_PROPERTY, modelData.get(DYNAMIC_COLORS_PROPERTY));
                }
                if (modelData.has(MATERIAL_STATE_PROPERTY) && !matData.has(MATERIAL_STATE_PROPERTY)) {
                    builder.with(MATERIAL_STATE_PROPERTY, modelData.get(MATERIAL_STATE_PROPERTY));
                }
                resolvedData = builder.build();
            }
        }
        return super.getModelData(level, pos, state, resolvedData);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.translucent());
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BlockState materialState = data.get(MATERIAL_STATE_PROPERTY);
        if (materialState != null) {
            BakedModel materialModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(materialState);
            return materialModel.getParticleIcon(data);
        }
        return super.getParticleIcon(data);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return super.getQuads(state, side, rand);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        if (renderType != null && renderType != RenderType.translucent()) {
            return Collections.emptyList();
        }

        Map<String, Integer> customColors = extraData.get(DYNAMIC_COLORS_PROPERTY);
        if (customColors != null && customColors.values().stream()
            .anyMatch(c -> IDynamicTexturedBlock.isRainbowColor(c) || IDynamicTexturedBlock.isMultiColor(c))) {
            return Collections.emptyList();
        }

        Map<String, ResourceLocation> customTextures = extraData.get(DYNAMIC_TEXTURES_PROPERTY);
        BlockState materialState = extraData.get(MATERIAL_STATE_PROPERTY);

        if ((customTextures == null || customTextures.isEmpty()) && (customColors == null || customColors.isEmpty()) && materialState == null) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }

        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, null);
        return remapQuads(quads, materialState, customTextures, customColors, extraData, rand, renderType);
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext cameraTransformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        BakedModel model = this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
        return model == this.originalModel ? this : new DynamicTexturedBakedModel(model, this.dynamicBlock);
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return itemOverrides;
    }

    private class DynamicTexturedItemOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            Map<String, ResourceLocation> customTextures = IDynamicTexturedBlock.getDynamicTextures(stack);
            Map<String, Integer> customColors = IDynamicTexturedBlock.getDynamicColors(stack);
            BlockState materialState = IDynamicTexturedBlock.getDynamicMaterial(stack);
            if (customTextures.isEmpty() && customColors.isEmpty() && materialState == null) {
                return DynamicTexturedBakedModel.this;
            }
            return new DynamicTexturedItemBakedModel(DynamicTexturedBakedModel.this, null, materialState, customTextures, customColors);
        }
    }

    public List<BakedQuad> remapQuads(List<BakedQuad> quads, Map<String, ResourceLocation> customTextures) {
        return remapQuads(quads, null, customTextures, Collections.emptyMap(), ModelData.EMPTY, null, null);
    }

    public List<BakedQuad> remapQuads(List<BakedQuad> quads, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
        return remapQuads(quads, null, customTextures, customColors, ModelData.EMPTY, null, null);
    }

    public List<BakedQuad> remapQuads(List<BakedQuad> quads, @Nullable BlockState materialState, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors, @NotNull ModelData extraData, @Nullable RandomSource rand, @Nullable RenderType renderType) {
        if (quads.isEmpty() || ((customTextures == null || customTextures.isEmpty()) && (customColors == null || customColors.isEmpty()) && materialState == null)) {
            return quads;
        }
        List<BakedQuad> remapped = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            remapQuad(quad, materialState, customTextures, customColors, extraData, rand, renderType, remapped);
        }
        return remapped;
    }

    public BakedQuad remapQuad(BakedQuad quad, Map<String, ResourceLocation> customTextures) {
        List<BakedQuad> out = new ArrayList<>(1);
        remapQuad(quad, null, customTextures, Collections.emptyMap(), ModelData.EMPTY, null, null, out);
        return out.isEmpty() ? quad : out.get(0);
    }

    public BakedQuad remapQuad(BakedQuad quad, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
        List<BakedQuad> out = new ArrayList<>(1);
        remapQuad(quad, null, customTextures, customColors, ModelData.EMPTY, null, null, out);
        return out.isEmpty() ? quad : out.get(0);
    }

    public BakedQuad remapQuad(BakedQuad quad, @Nullable BlockState materialState, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors, @NotNull ModelData extraData, @Nullable RandomSource rand, @Nullable RenderType renderType) {
        List<BakedQuad> out = new ArrayList<>(1);
        remapQuad(quad, materialState, customTextures, customColors, extraData, rand, renderType, out);
        return out.isEmpty() ? quad : out.get(0);
    }

    public void remapQuad(BakedQuad quad, @Nullable BlockState materialState, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors, @NotNull ModelData extraData, @Nullable RandomSource rand, @Nullable RenderType renderType, List<BakedQuad> out) {
        String matchedKey = findMatchingKey(quad);
        if (matchedKey == null) {
            out.add(quad);
            return;
        }

        ResourceLocation targetTex = customTextures != null ? customTextures.get(matchedKey) : null;
        Integer tintColor = customColors != null ? customColors.get(matchedKey) : null;
        int tint = tintColor != null ? tintColor : -1;
        if (IDynamicTexturedBlock.isRainbowColor(tint)) {
            tint = CabinetMakerTableScreen.getRainbowColor();
        }
        IDynamicTexturedBlock.KeywordColor keywordColor = IDynamicTexturedBlock.KeywordColor.fromColor(tint);
        long speed = 0;
        if (keywordColor != null && keywordColor.getSpeed() != null) {
            speed = keywordColor.getSpeed();
        }
        if (keywordColor != null && keywordColor.getMultiColors() != null && speed > 0) {
            tint = CabinetMakerTableScreen.getMultiColorAnimation(keywordColor.getMultiColors(), speed);
        }

        boolean useMaterialModel = false;
        List<BakedQuad> matQuads = null;
        if (materialState != null && "0".equals(matchedKey)) {
            BakedModel materialModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(materialState);
            Direction quadDir = quad.getDirection();
            RandomSource random = rand != null ? rand : RandomSource.create();
            matQuads = materialModel.getQuads(materialState, quadDir, random, extraData, null);
            if (matQuads.isEmpty()) {
                matQuads = materialModel.getQuads(materialState, null, random, extraData, null);
            }
            if (matQuads.isEmpty()) {
                for (RenderType rt : materialModel.getRenderTypes(materialState, random, extraData)) {
                    matQuads = materialModel.getQuads(materialState, quadDir, random, extraData, rt);
                    if (!matQuads.isEmpty()) break;
                    matQuads = materialModel.getQuads(materialState, null, random, extraData, rt);
                    if (!matQuads.isEmpty()) break;
                }
            }
            if (!matQuads.isEmpty() && isFullFaceCoverage(matQuads, quadDir)) {
                if (targetTex == null) {
                    useMaterialModel = true;
                } else {
                    for (BakedQuad mq : matQuads) {
                        ResourceLocation mqSpriteName = mq.getSprite().contents().name();
                        if (isMatchingMaterialSprite(mqSpriteName, targetTex)) {
                            useMaterialModel = true;
                            break;
                        }
                    }
                }
            }
        }

        if (useMaterialModel && !matQuads.isEmpty()) {
            remapQuadWithMaterialQuads(quad, matQuads, tint, out);
        } else if (targetTex != null) {
            TextureAtlasSprite newSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(targetTex);
            if (isValidSprite(newSprite)) {
                out.add(remapSprite(quad, newSprite, tint));
            } else {
                TextureAtlasSprite fallbackSprite = null;
                if (materialState != null) {
                    try {
                        BakedModel materialModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(materialState);
                        try {
                            TextureAtlasSprite particle = materialModel.getParticleIcon(extraData);
                            if (isValidSprite(particle)) {
                                fallbackSprite = particle;
                            }
                        } catch (Exception ignored) {
                            try {
                                TextureAtlasSprite particle = materialModel.getParticleIcon(ModelData.EMPTY);
                                if (isValidSprite(particle)) {
                                    fallbackSprite = particle;
                                }
                            } catch (Exception ignored2) {
                            }
                        }
                        if (fallbackSprite == null && matQuads != null && !matQuads.isEmpty()) {
                            TextureAtlasSprite mqSprite = matQuads.get(0).getSprite();
                            if (isValidSprite(mqSprite)) {
                                fallbackSprite = mqSprite;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (fallbackSprite != null) {
                    out.add(remapSprite(quad, fallbackSprite, tint));
                } else if (newSprite != null) {
                    out.add(remapSprite(quad, newSprite, tint));
                } else if (tint != -1) {
                    out.add(remapSprite(quad, quad.getSprite(), tint));
                } else {
                    out.add(quad);
                }
            }
        } else if (materialState != null && "0".equals(matchedKey)) {
            BakedModel materialModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(materialState);
            TextureAtlasSprite matParticle = null;
            try {
                matParticle = materialModel.getParticleIcon(extraData);
            } catch (Exception ignored) {
                try {
                    matParticle = materialModel.getParticleIcon(ModelData.EMPTY);
                } catch (Exception ignored2) {
                }
            }
            if (isValidSprite(matParticle)) {
                out.add(remapSprite(quad, matParticle, tint));
            } else if (tint != -1) {
                out.add(remapSprite(quad, quad.getSprite(), tint));
            } else {
                out.add(quad);
            }
        } else if (tint != -1) {
            out.add(remapSprite(quad, quad.getSprite(), tint));
        } else {
            out.add(quad);
        }
    }

    private void remapQuadWithMaterialQuads(BakedQuad quad, List<BakedQuad> matQuads, int tint, List<BakedQuad> out) {
        Direction dir = quad.getDirection();
        List<MatQuadBounds> boundsList = new ArrayList<>(matQuads.size());
        for (BakedQuad mq : matQuads) {
            boundsList.add(new MatQuadBounds(mq, dir));
        }

        if (matQuads.size() == 1) {
            out.add(remapSingleQuadFromBounds(quad, boundsList.get(0), tint));
            return;
        }

        int[] v = quad.getVertices();
        int stride = IQuadTransformer.STRIDE;
        float qMinU = Float.MAX_VALUE, qMaxU = -Float.MAX_VALUE;
        float qMinV = Float.MAX_VALUE, qMaxV = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            int posOffset = i * stride + IQuadTransformer.POSITION;
            float x = Float.intBitsToFloat(v[posOffset]);
            float y = Float.intBitsToFloat(v[posOffset + 1]);
            float z = Float.intBitsToFloat(v[posOffset + 2]);
            float[] fUV = getFaceUV(x, y, z, dir);
            qMinU = Math.min(qMinU, fUV[0]);
            qMaxU = Math.max(qMaxU, fUV[0]);
            qMinV = Math.min(qMinV, fUV[1]);
            qMaxV = Math.max(qMaxV, fUV[1]);
        }

        List<BakedQuad> pieces = new ArrayList<>();
        pieces.add(quad);
        if (qMinU < 0.49f && qMaxU > 0.51f) {
            List<BakedQuad> splitU = new ArrayList<>();
            for (BakedQuad p : pieces) {
                splitQuadByFaceCoord(p, dir, true, splitU);
            }
            pieces = splitU;
        }
        if (qMinV < 0.49f && qMaxV > 0.51f) {
            List<BakedQuad> splitV = new ArrayList<>();
            for (BakedQuad p : pieces) {
                splitQuadByFaceCoord(p, dir, false, splitV);
            }
            pieces = splitV;
        }

        for (BakedQuad piece : pieces) {
            int[] pv = piece.getVertices();
            float sumU = 0, sumV = 0;
            for (int i = 0; i < 4; i++) {
                int posOffset = i * stride + IQuadTransformer.POSITION;
                float x = Float.intBitsToFloat(pv[posOffset]);
                float y = Float.intBitsToFloat(pv[posOffset + 1]);
                float z = Float.intBitsToFloat(pv[posOffset + 2]);
                float[] fUV = getFaceUV(x, y, z, dir);
                sumU += fUV[0];
                sumV += fUV[1];
            }
            float cU = sumU / 4.0f;
            float cV = sumV / 4.0f;

            MatQuadBounds best = boundsList.get(0);
            for (MatQuadBounds b : boundsList) {
                if (b.contains(cU, cV)) {
                    best = b;
                    break;
                }
            }
            out.add(remapSingleQuadFromBounds(piece, best, tint));
        }
    }

    private static BakedQuad remapSingleQuadFromBounds(BakedQuad quad, MatQuadBounds bounds, int tint) {
        Direction dir = quad.getDirection();
        int[] v = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
        int stride = IQuadTransformer.STRIDE;

        for (int i = 0; i < 4; i++) {
            int posOffset = i * stride + IQuadTransformer.POSITION;
            float x = Float.intBitsToFloat(v[posOffset]);
            float y = Float.intBitsToFloat(v[posOffset + 1]);
            float z = Float.intBitsToFloat(v[posOffset + 2]);

            float[] fUV = getFaceUV(x, y, z, dir);
            float[] mapped = bounds.mapUV(fUV[0], fUV[1]);

            int uvOffset = i * stride + IQuadTransformer.UV0;
            v[uvOffset] = Float.floatToRawIntBits(mapped[0]);
            v[uvOffset + 1] = Float.floatToRawIntBits(mapped[1]);

            if (tint != -1) {
                int abgr = QuadTransformers.toABGR(tint);
                v[i * stride + IQuadTransformer.COLOR] = abgr;
            }
        }

        return new BakedQuad(v, -1, quad.getDirection(), bounds.matQuad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }

    private static void splitQuadByFaceCoord(BakedQuad quad, Direction dir, boolean isUAxis, List<BakedQuad> out) {
        int[] v = quad.getVertices();
        int stride = IQuadTransformer.STRIDE;
        float[] c = new float[4];
        for (int i = 0; i < 4; i++) {
            int posOffset = i * stride + IQuadTransformer.POSITION;
            float x = Float.intBitsToFloat(v[posOffset]);
            float y = Float.intBitsToFloat(v[posOffset + 1]);
            float z = Float.intBitsToFloat(v[posOffset + 2]);
            float[] fUV = getFaceUV(x, y, z, dir);
            c[i] = isUAxis ? fUV[0] : fUV[1];
        }

        List<Integer> splitEdgeStart = new ArrayList<>();
        List<Float> splitEdgeT = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float c1 = c[i];
            float c2 = c[j];
            if ((c1 < (float) 0.5 - 0.001f && c2 > (float) 0.5 + 0.001f) || (c1 > (float) 0.5 + 0.001f && c2 < (float) 0.5 - 0.001f)) {
                splitEdgeStart.add(i);
                splitEdgeT.add(((float) 0.5 - c1) / (c2 - c1));
            }
        }

        if (splitEdgeStart.size() != 2) {
            out.add(quad);
            return;
        }

        int e0 = splitEdgeStart.get(0);
        float t0 = splitEdgeT.get(0);
        int[] m0 = lerpVertex(v, stride, e0, (e0 + 1) % 4, t0);

        int e1 = splitEdgeStart.get(1);
        float t1 = splitEdgeT.get(1);
        int[] m1 = lerpVertex(v, stride, e1, (e1 + 1) % 4, t1);

        if ((e0 == 0 && e1 == 2) || (e0 == 2 && e1 == 0)) {
            // Edges 0-1 and 2-3 are split by m0 and m1.
            int[] vA = buildQuadVertices(v, stride, 0, m0, m1, 3);
            int[] vB = buildQuadVertices(v, stride, m0, 1, 2, m1);
            out.add(new BakedQuad(vA, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            out.add(new BakedQuad(vB, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
        } else if ((e0 == 1 && e1 == 3) || (e0 == 3 && e1 == 1)) {
            // Edges 1-2 and 3-0 are split by m0 and m1.
            int[] vA = buildQuadVertices(v, stride, 0, 1, m0, m1);
            int[] vB = buildQuadVertices(v, stride, m1, m0, 2, 3);
            out.add(new BakedQuad(vA, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            out.add(new BakedQuad(vB, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
        } else {
            out.add(quad);
        }
    }

    private static int[] buildQuadVertices(int[] v, int stride, Object... corners) {
        int[] result = new int[stride * 4];
        for (int i = 0; i < 4; i++) {
            Object corner = corners[i];
            if (corner instanceof Integer idx) {
                System.arraycopy(v, idx * stride, result, i * stride, stride);
            } else if (corner instanceof int[] vert) {
                System.arraycopy(vert, 0, result, i * stride, stride);
            }
        }
        return result;
    }

    private static int[] lerpVertex(int[] v, int stride, int idxA, int idxB, float t) {
        int[] result = new int[stride];
        int offA = idxA * stride;
        int offB = idxB * stride;

        for (int p = 0; p < 3; p++) {
            float valA = Float.intBitsToFloat(v[offA + IQuadTransformer.POSITION + p]);
            float valB = Float.intBitsToFloat(v[offB + IQuadTransformer.POSITION + p]);
            result[IQuadTransformer.POSITION + p] = Float.floatToRawIntBits(valA + t * (valB - valA));
        }

        result[IQuadTransformer.COLOR] = v[offA + IQuadTransformer.COLOR];

        for (int u = 0; u < 2; u++) {
            float valA = Float.intBitsToFloat(v[offA + IQuadTransformer.UV0 + u]);
            float valB = Float.intBitsToFloat(v[offB + IQuadTransformer.UV0 + u]);
            result[IQuadTransformer.UV0 + u] = Float.floatToRawIntBits(valA + t * (valB - valA));
        }

        result[IQuadTransformer.UV2] = v[offA + IQuadTransformer.UV2];
        result[IQuadTransformer.NORMAL] = v[offA + IQuadTransformer.NORMAL];

        return result;
    }

    public static float[] getFaceUV(float x, float y, float z, @Nullable Direction dir) {
        if (dir == null) {
            return new float[] { x, z };
        }
        return switch (dir) {
            case UP -> new float[] { x, z };
            case DOWN -> new float[] { x, 1.0f - z };
            case NORTH -> new float[] { 1.0f - x, 1.0f - y };
            case SOUTH -> new float[] { x, 1.0f - y };
            case WEST -> new float[] { z, 1.0f - y };
            case EAST -> new float[] { 1.0f - z, 1.0f - y };
        };
    }

    private static class MatQuadBounds {
        final BakedQuad matQuad;
        final float minU, maxU, minV, maxV;
        final float spriteMinU, spriteMaxU, spriteMinV, spriteMaxV;

        MatQuadBounds(BakedQuad mq, @Nullable Direction dir) {
            this.matQuad = mq;
            int[] v = mq.getVertices();
            int stride = IQuadTransformer.STRIDE;
            float fMinU = Float.MAX_VALUE, fMaxU = -Float.MAX_VALUE;
            float fMinV = Float.MAX_VALUE, fMaxV = -Float.MAX_VALUE;
            float sMinU = Float.MAX_VALUE, sMaxU = -Float.MAX_VALUE;
            float sMinV = Float.MAX_VALUE, sMaxV = -Float.MAX_VALUE;

            Direction effectiveDir = dir != null ? dir : mq.getDirection();
            for (int i = 0; i < 4; i++) {
                int posOffset = i * stride + IQuadTransformer.POSITION;
                float x = Float.intBitsToFloat(v[posOffset]);
                float y = Float.intBitsToFloat(v[posOffset + 1]);
                float z = Float.intBitsToFloat(v[posOffset + 2]);

                float[] faceUV = getFaceUV(x, y, z, effectiveDir);
                fMinU = Math.min(fMinU, faceUV[0]);
                fMaxU = Math.max(fMaxU, faceUV[0]);
                fMinV = Math.min(fMinV, faceUV[1]);
                fMaxV = Math.max(fMaxV, faceUV[1]);

                int uvOffset = i * stride + IQuadTransformer.UV0;
                float u = Float.intBitsToFloat(v[uvOffset]);
                float vCoord = Float.intBitsToFloat(v[uvOffset + 1]);
                sMinU = Math.min(sMinU, u);
                sMaxU = Math.max(sMaxU, u);
                sMinV = Math.min(sMinV, vCoord);
                sMaxV = Math.max(sMaxV, vCoord);
            }

            this.minU = Math.min(fMinU, fMaxU);
            this.maxU = Math.max(fMinU, fMaxU);
            this.minV = Math.min(fMinV, fMaxV);
            this.maxV = Math.max(fMinV, fMaxV);
            this.spriteMinU = sMinU;
            this.spriteMaxU = sMaxU;
            this.spriteMinV = sMinV;
            this.spriteMaxV = sMaxV;
        }

        boolean contains(float u, float v) {
            float eps = 0.05f;
            return u >= minU - eps && u <= maxU + eps && v >= minV - eps && v <= maxV + eps;
        }

        float[] mapUV(float faceU, float faceV) {
            float du = maxU - minU;
            float dv = maxV - minV;
            float fracU = du > 0.0001f ? (faceU - minU) / du : 0f;
            float fracV = dv > 0.0001f ? (faceV - minV) / dv : 0f;
            fracU = Math.max(0f, Math.min(1f, fracU));
            fracV = Math.max(0f, Math.min(1f, fracV));
            float targetU = spriteMinU + fracU * (spriteMaxU - spriteMinU);
            float targetV = spriteMinV + fracV * (spriteMaxV - spriteMinV);
            return new float[] { targetU, targetV };
        }
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
            if (defaultTex.equals(spriteLoc) || defaultTex.getPath().equals(spriteLoc.getPath()) || spriteLoc.getPath().endsWith(defaultTex.getPath())) {
                return key;
            }
        }

        return null;
    }

    private static boolean isFullFaceCoverage(List<BakedQuad> quads, @Nullable Direction dir) {
        if (quads == null || quads.isEmpty()) {
            return false;
        }

        int stride = IQuadTransformer.STRIDE;
        float totalMinU = Float.MAX_VALUE, totalMaxU = -Float.MAX_VALUE;
        float totalMinV = Float.MAX_VALUE, totalMaxV = -Float.MAX_VALUE;

        for (BakedQuad q : quads) {
            int[] v = q.getVertices();
            if (v.length < stride * 4) {
                return false;
            }
            Direction effectiveDir = dir != null ? dir : q.getDirection();
            float qMinU = Float.MAX_VALUE, qMaxU = -Float.MAX_VALUE;
            float qMinV = Float.MAX_VALUE, qMaxV = -Float.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                int posOffset = i * stride + IQuadTransformer.POSITION;
                float x = Float.intBitsToFloat(v[posOffset]);
                float y = Float.intBitsToFloat(v[posOffset + 1]);
                float z = Float.intBitsToFloat(v[posOffset + 2]);

                float[] faceUV = getFaceUV(x, y, z, effectiveDir);
                qMinU = Math.min(qMinU, faceUV[0]);
                qMaxU = Math.max(qMaxU, faceUV[0]);
                qMinV = Math.min(qMinV, faceUV[1]);
                qMaxV = Math.max(qMaxV, faceUV[1]);
            }

            float qWidth = qMaxU - qMinU;
            float qHeight = qMaxV - qMinV;
            if (qWidth < 0.20f || qHeight < 0.20f) {
                return false;
            }

            totalMinU = Math.min(totalMinU, qMinU);
            totalMaxU = Math.max(totalMaxU, qMaxU);
            totalMinV = Math.min(totalMinV, qMinV);
            totalMaxV = Math.max(totalMaxV, qMaxV);
        }

        float totalWidth = totalMaxU - totalMinU;
        float totalHeight = totalMaxV - totalMinV;

        return totalWidth >= 0.75f && totalHeight >= 0.75f && totalMinU <= 0.20f && totalMinV <= 0.20f && totalMaxU >= 0.80f && totalMaxV >= 0.80f;
    }

    private static boolean isMatchingMaterialSprite(ResourceLocation spriteName, ResourceLocation targetTex) {
        if (spriteName.equals(targetTex) || spriteName.getPath().equals(targetTex.getPath())) {
            return true;
        }
        if (spriteName.getNamespace().equals(targetTex.getNamespace())) {
            String sPath = spriteName.getPath();
            String tPath = targetTex.getPath();
            if (sPath.startsWith(tPath)) {
                String remainder = sPath.substring(tPath.length());
                if (remainder.isEmpty()
                    || remainder.startsWith("_ctm")
                    || remainder.startsWith("-ctm")
                    || remainder.startsWith("_connected")
                    || remainder.startsWith("/ctm")
                    || remainder.startsWith("/")) {
                    return true;
                }
            }
            String sFile = sPath.substring(sPath.lastIndexOf('/') + 1);
            String tFile = tPath.substring(tPath.lastIndexOf('/') + 1);
            if (sFile.equals(tFile)) {
                return true;
            }
            if (sFile.startsWith(tFile)) {
                String remainder = sFile.substring(tFile.length());
                return remainder.startsWith("_ctm")
                    || remainder.startsWith("-ctm")
                    || remainder.startsWith("_connected")
                    || remainder.startsWith("/ctm");
            }
        }
        return false;
    }

    public static boolean isValidSprite(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            return false;
        }
        ResourceLocation name = sprite.contents().name();
        return !"missingno".equals(name.getPath()) && !"minecraft:missingno".equals(name.toString());
    }

    public static boolean isMissingSprite(@Nullable TextureAtlasSprite sprite) {
        return !isValidSprite(sprite);
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

    public static class DynamicTexturedItemBakedModel extends BakedModelWrapper<BakedModel> {
        @Getter private final DynamicTexturedBakedModel parent;
        @Getter private final BlockState blockState;
        @Getter private final BlockState materialState;
        @Getter private final Map<String, ResourceLocation> customTextures;
        @Getter private final Map<String, Integer> customColors;

        public DynamicTexturedItemBakedModel(DynamicTexturedBakedModel parent, Map<String, ResourceLocation> customTextures) {
            this(parent, null, null, customTextures, Collections.emptyMap());
        }

        public DynamicTexturedItemBakedModel(DynamicTexturedBakedModel parent, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
            this(parent, null, null, customTextures, customColors);
        }

        public DynamicTexturedItemBakedModel(DynamicTexturedBakedModel parent, @Nullable BlockState blockState, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
            this(parent, blockState, null, customTextures, customColors);
        }

        public DynamicTexturedItemBakedModel(DynamicTexturedBakedModel parent, @Nullable BlockState blockState, @Nullable BlockState materialState, Map<String, ResourceLocation> customTextures, Map<String, Integer> customColors) {
            super(parent.originalModel);
            this.parent = parent;
            this.blockState = blockState;
            this.materialState = materialState;
            this.customTextures = customTextures != null ? customTextures : Collections.emptyMap();
            this.customColors = customColors != null ? customColors : Collections.emptyMap();
        }

        @Override
        public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
            return Collections.singletonList(Sheets.translucentItemSheet());
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            BlockState targetState = state != null ? state : this.blockState;
            List<BakedQuad> quads = super.getQuads(targetState, side, rand);
            return parent.remapQuads(quads, this.materialState, customTextures, customColors, ModelData.EMPTY, rand, null);
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
            if (renderType != null && renderType != Sheets.translucentItemSheet() && renderType != RenderType.translucent()) {
                return Collections.emptyList();
            }
            BlockState targetState = state != null ? state : this.blockState;
            BlockState mat = this.materialState != null ? this.materialState : extraData.get(MATERIAL_STATE_PROPERTY);
            List<BakedQuad> quads = super.getQuads(targetState, side, rand, extraData, null);
            return parent.remapQuads(quads, mat, customTextures, customColors, extraData, rand, renderType);
        }

        @Override
        public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext cameraTransformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
            BakedModel model = this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
            return model == this.originalModel ? this : new DynamicTexturedItemBakedModel(new DynamicTexturedBakedModel(model, this.parent.getDynamicBlock()), this.blockState, this.materialState, this.customTextures, this.customColors);
        }

        @Override
        public @NotNull List<BakedModel> getRenderPasses(@NotNull ItemStack itemStack, boolean fabulous) {
            return Collections.singletonList(this);
        }
    }
}

package net.placemats.client.gui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.placemats.client.renderer.CyclicLayerBakedModel;
import net.placemats.common.data.PlaceMatTags;

/**
 * Extracts 16x16 textures from a given ItemStack's block or item model.
 * For the {@link net.placemats.common.block.CabinetMakerTableBlock}
 */
@SuppressWarnings("deprecation")
public class BlockTextureExtractor {

    private static final RandomSource RANDOM = RandomSource.create();
    private static final int MAX_BASE_LAYERS = 5;

    public static List<ResourceLocation> extract16x16Textures(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST)) {
            return Collections.emptyList();
        }

        List<ResourceLocation> textures = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        // BlockItem model quads.
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockState state = block.defaultBlockState();
            try {
                BakedModel blockModel = mc.getBlockRenderer().getBlockModel(state);
                extractFromModel(blockModel, state, textures);
            } catch (Exception ignored) {
            }
        }

        // Item model quads.
        try {
            BakedModel itemModel = mc.getItemRenderer().getModel(stack, null, null, 0);
            extractFromModel(itemModel, null, textures);
        } catch (Exception ignored) {
        }

        return textures;
    }

    private static void extractFromModel(BakedModel model, BlockState state, List<ResourceLocation> out) {
        if (model == null) {
            return;
        }

        // Unwrap CyclicLayerBakedModel so inactive animation frames aren't hidden.
        while (model instanceof CyclicLayerBakedModel cyclic) {
            model = cyclic.getOriginalModel();
        }

        List<BakedQuad> allQuads = new ArrayList<>();

        // Directional quads.
        for (Direction side : Direction.values()) {
            try {
                allQuads.addAll(model.getQuads(state, side, RANDOM, ModelData.EMPTY, null));
            } catch (Exception ignored) {
                try {
                    allQuads.addAll(model.getQuads(state, side, RANDOM));
                } catch (Exception ignored2) {
                }
            }
        }

        // Unculled quads.
        try {
            allQuads.addAll(model.getQuads(state, null, RANDOM, ModelData.EMPTY, null));
        } catch (Exception ignored) {
            try {
                allQuads.addAll(model.getQuads(state, null, RANDOM));
            } catch (Exception ignored2) {
            }
        }

        // Render type-specific quads (Create casings, multi-layer blocks).
        if (state != null) {
            try {
                for (net.minecraft.client.renderer.RenderType rt : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
                    for (Direction side : Direction.values()) {
                        try {
                            allQuads.addAll(model.getQuads(state, side, RANDOM, ModelData.EMPTY, rt));
                        } catch (Exception ignored) {
                        }
                    }
                    try {
                        allQuads.addAll(model.getQuads(state, null, RANDOM, ModelData.EMPTY, rt));
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (allQuads.isEmpty()) {
            RenderType[] commonTypes = new RenderType[] {
                RenderType.solid(),
                RenderType.cutout(),
                RenderType.cutoutMipped(),
                RenderType.translucent()
            };
            for (RenderType rt : commonTypes) {
                for (Direction side : Direction.values()) {
                    try {
                        allQuads.addAll(model.getQuads(state, side, RANDOM, ModelData.EMPTY, rt));
                    } catch (Exception ignored) {
                    }
                }
                try {
                    allQuads.addAll(model.getQuads(state, null, RANDOM, ModelData.EMPTY, rt));
                } catch (Exception ignored) {
                }
            }
        }

        Map<Integer, ResourceLocation> layeredTextures = new TreeMap<>();
        Set<ResourceLocation> unlayeredTextures = new LinkedHashSet<>();

        for (BakedQuad quad : allQuads) {
            TextureAtlasSprite sprite = quad.getSprite();
            if (!isValidSprite(sprite)) {
                continue;
            }

            ResourceLocation name = sprite.contents().name();
            int layer = quad.getTintIndex();

            if (layer >= 0 && layer < MAX_BASE_LAYERS) {
                layeredTextures.putIfAbsent(layer, name);
            } else if (layer < 0) {
                unlayeredTextures.add(name);
            }
        }

        if (!layeredTextures.isEmpty()) {
            for (ResourceLocation tex : layeredTextures.values()) {
                if (!out.contains(tex)) {
                    out.add(tex);
                }
            }
            return;
        }

        for (ResourceLocation tex : unlayeredTextures) {
            if (!out.contains(tex)) {
                out.add(tex);
            }
        }

        // Particles.
        try {
            TextureAtlasSprite particle = model.getParticleIcon(ModelData.EMPTY);
            if (isValidSprite(particle)) {
                ResourceLocation particleName = particle.contents().name();
                if (!out.contains(particleName)) {
                    out.add(particleName);
                }
            }
        } catch (Exception ignored) {
            try {
                TextureAtlasSprite particle = model.getParticleIcon();
                if (isValidSprite(particle)) {
                    ResourceLocation particleName = particle.contents().name();
                    if (!out.contains(particleName)) {
                        out.add(particleName);
                    }
                }
            } catch (Exception ignored2) {
            }
        }
    }

    private static boolean isValidSprite(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return false;
        }
        ResourceLocation name = sprite.contents().name();
        // Oops, no Mew for you.
        if (name.getPath().equals("missingno") || name.toString().equals("minecraft:missingno")) {
            return false;
        }

        int width = sprite.contents().width();
        int height = sprite.contents().height();

        return width > 0 && height > 0;
    }
}

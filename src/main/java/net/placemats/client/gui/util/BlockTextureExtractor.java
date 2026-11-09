package net.placemats.client.gui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
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
import net.placemats.common.data.PlaceMatTags;

/**
 * Extracts 16x16 textures from a given ItemStack's block or item model.
 * For the {@link net.placemats.common.block.CabinetMakerTableBlock}
 */
@SuppressWarnings("deprecation")
public class BlockTextureExtractor {

    private static final RandomSource RANDOM = RandomSource.create();

    public static List<ResourceLocation> extract16x16Textures(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST)) {
            return Collections.emptyList();
        }

        Set<ResourceLocation> textures = new LinkedHashSet<>();
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

        return new ArrayList<>(textures);
    }

    private static void extractFromModel(BakedModel model, BlockState state, Set<ResourceLocation> out) {
        if (model == null) {
            return;
        }

        // Directional quads.
        for (Direction side : Direction.values()) {
            try {
                List<BakedQuad> quads = model.getQuads(state, side, RANDOM);
                for (BakedQuad quad : quads) {
                    addValidSprite(quad.getSprite(), out);
                }
            } catch (Exception ignored) {
            }
        }

        // Unculled quads.
        try {
            List<BakedQuad> unculled = model.getQuads(state, null, RANDOM);
            for (BakedQuad quad : unculled) {
                addValidSprite(quad.getSprite(), out);
            }
        } catch (Exception ignored) {
        }

        // Particles.
        try {
            TextureAtlasSprite particle = model.getParticleIcon();
            addValidSprite(particle, out);
        } catch (Exception ignored) {
        }
    }

    private static void addValidSprite(TextureAtlasSprite sprite, Set<ResourceLocation> out) {
        if (sprite == null) {
            return;
        }
        ResourceLocation name = sprite.contents().name();
        // Oops, no Mew for you.
        if (name.getPath().equals("missingno") || name.toString().equals("minecraft:missingno")) {
            return;
        }

        int width = sprite.contents().width();
        int height = sprite.contents().height();

        // Standard 16x16 textures.
        if (width == 16 && height == 16) {
            out.add(name);
        }
    }
}

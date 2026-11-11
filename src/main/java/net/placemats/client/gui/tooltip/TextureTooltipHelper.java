package net.placemats.client.gui.tooltip;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.placemats.client.gui.util.BlockTextureExtractor;
import net.placemats.common.data.PlaceMatTags;

/**
 * Helper class for extracting textures from items for {@link ClientTextureTooltip}.
 */
@SuppressWarnings("unused")
public class TextureTooltipHelper {

    public static final int MAX_TOOLTIP_TEXTURES = 5;
    private static final Map<Item, List<ResourceLocation>> TEXTURE_CACHE = new ConcurrentHashMap<>();

    public static List<ResourceLocation> getTextures(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }
        return TEXTURE_CACHE.computeIfAbsent(stack.getItem(), item -> BlockTextureExtractor.extract16x16Textures(stack));
    }

    public static List<ResourceLocation> getTooltipTextures(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(PlaceMatTags.Items.TEXTURE_PROVIDERS)) {
            return Collections.emptyList();
        }
        List<ResourceLocation> allTextures = getTextures(stack);
        if (allTextures.isEmpty()) {
            return Collections.emptyList();
        }
        return allTextures.size() > MAX_TOOLTIP_TEXTURES
                ? allTextures.subList(0, MAX_TOOLTIP_TEXTURES)
                : allTextures;
    }

    public static Optional<TextureTooltip> createTooltipComponent(ItemStack stack) {
        List<ResourceLocation> textures = getTooltipTextures(stack);
        if (textures.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TextureTooltip(textures));
    }

    public static void clearCache() {
        TEXTURE_CACHE.clear();
    }
}

package net.placemats.common.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.placemats.common.blockentity.IDynamicTexturedBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for blocks that support custom dynamic textures.
 */
@SuppressWarnings("unused")
public interface IDynamicTexturedBlock {

    String NBT_DYNAMIC_TEXTURES = "DynamicTextures";
    String NBT_DYNAMIC_COLORS = "DynamicColors";

    /**
     * Enum for colors by keywords for Easter eggs and faster coloring.
     * 8-character limit for strings.
     */
    @Getter
    enum KeywordColor {
        // Easter Eggs
        RAINBOW(0x00726564, List.of(
            "rainbow", "redeix", "pyritie", "tomtom", "gustavo",
            "ashlen", "xikaro", "zippity", "yan", "airrice",
            "dmitry", "gamestar", "nebby", "sakura", "tfg"
        )),
        // Base Colors
        WHITE(0xFFF9FFFE, List.of("white")),
        ORANGE(0xFFF9801D, List.of("orange")),
        MAGENTA(0xFFC74EBD, List.of("magenta")),
        LIGHT_BLUE(0xFF3AB3DA, List.of("lblue")),
        YELLOW(0xFFFED83D, List.of("yellow")),
        LIME(0xFF80C71F, List.of("lime", "lgreen")),
        PINK(0xFFF38BAA, List.of("pink")),
        GRAY(0xFF474F52, List.of("gray", "grey")),
        LIGHT_GRAY(0xFF9D9D97, List.of("lgray", "lgrey")),
        CYAN(0xFF169C9C, List.of("cyan")),
        PURPLE(0xFF8932B8, List.of("purple")),
        BLUE(0xFF3C44AA, List.of("blue")),
        BROWN(0xFF835432, List.of("brown")),
        GREEN(0xFF5E7C16, List.of("green")),
        RED(0xFFB02E26, List.of("red")),
        BLACK(0xFF1D1D21, List.of("black")),
        // Wood Colors
        OAK(0xFFAD8D54, List.of("oak")),
        SPRUCE(0xFF6E512D, List.of("spruce")),
        BIRCH(0xFFC5B478, List.of("birch")),
        JUNGLE(0xFFA87753, List.of("jungle")),
        ACACIA(0xFFAB5C31, List.of("acacia")),
        DARK_OAK(0xFF5C3828, List.of("darkoak", "dark oak", "dark_oak", "doak")),
        BAMBOO(0xFFB4A948, List.of("bamboo")),
        MANGROVE(0xFF6D292D, List.of("mangrove")),
        CHERRY(0xFFDA9B95, List.of("cherry")),
        CRIMSON(0xFF7E3752, List.of("crimson")),
        WARPED(0xFF308B8A, List.of("warped"));

        private final int colorCode;
        private final List<String> keywords;

        KeywordColor(int colorCode, List<String> keywords) {
            this.colorCode = colorCode;
            this.keywords = keywords;
        }

        public boolean matches(Integer color) {
            return color == this.colorCode;
        }

        public static @Nullable KeywordColor fromColor(Integer color) {
            return Arrays.stream(values())
                .filter(c -> c.colorCode == color)
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Checks if the color is for the rainbow-cycling Easter egg.
     */
    static boolean isRainbowColor(Integer color) {
        return KeywordColor.RAINBOW.matches(color);
    }

    /**
     * @return Number of texture key slots this block supports (1 to 5).
     */
    int getTextureKeyCount();

    /**
     * @return List of texture key identifiers (ex. ["0", "1"]).
     */
    List<String> getTextureKeys();

    /**
     * @param key Texture key identifier.
     * @return Default texture location for this key.
     */
    ResourceLocation getDefaultTexture(String key);

    /**
     * @param keyIndex Key slot index.
     * @return Label for this key slot.
     */
    Component getTextureKeyName(int keyIndex);

    /**
     * Extracts dynamic texture overrides from an ItemStack's NBT.
     */
    static Map<String, ResourceLocation> getDynamicTextures(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptyMap();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_DYNAMIC_TEXTURES, Tag.TAG_COMPOUND)) {
            return Collections.emptyMap();
        }
        CompoundTag dtTag = tag.getCompound(NBT_DYNAMIC_TEXTURES);
        Map<String, ResourceLocation> map = new HashMap<>();
        for (String key : dtTag.getAllKeys()) {
            ResourceLocation rl = ResourceLocation.tryParse(dtTag.getString(key));
            if (rl != null) {
                map.put(key, rl);
            }
        }
        return map;
    }

    /**
     * Saves dynamic texture overrides to an ItemStack's NBT.
     */
    static void setDynamicTextures(ItemStack stack, Map<String, ResourceLocation> textures) {
        if (stack.isEmpty()) {
            return;
        }
        if (textures.isEmpty()) {
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                stack.getTag().remove(NBT_DYNAMIC_TEXTURES);
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        CompoundTag dtTag = new CompoundTag();
        for (Map.Entry<String, ResourceLocation> entry : textures.entrySet()) {
            dtTag.putString(entry.getKey(), entry.getValue().toString());
        }
        stack.addTagElement(NBT_DYNAMIC_TEXTURES, dtTag);
    }

    /**
     * Extracts dynamic color overrides from an ItemStack's NBT.
     */
    static Map<String, Integer> getDynamicColors(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptyMap();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_DYNAMIC_COLORS, Tag.TAG_COMPOUND)) {
            return Collections.emptyMap();
        }
        CompoundTag dcTag = tag.getCompound(NBT_DYNAMIC_COLORS);
        Map<String, Integer> map = new HashMap<>();
        for (String key : dcTag.getAllKeys()) {
            map.put(key, dcTag.getInt(key));
        }
        return map;
    }

    /**
     * Saves dynamic color overrides to an ItemStack's NBT.
     */
    static void setDynamicColors(ItemStack stack, Map<String, Integer> colors) {
        if (stack.isEmpty()) {
            return;
        }
        if (colors.isEmpty()) {
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                stack.getTag().remove(NBT_DYNAMIC_COLORS);
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        CompoundTag dcTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : colors.entrySet()) {
            dcTag.putInt(entry.getKey(), entry.getValue());
        }
        stack.addTagElement(NBT_DYNAMIC_COLORS, dcTag);
    }

    /**
     * Checks if an ItemStack has dynamic texture overrides.
     */
    static boolean hasDynamicTextures(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_DYNAMIC_TEXTURES, Tag.TAG_COMPOUND) && !tag.getCompound(NBT_DYNAMIC_TEXTURES).isEmpty();
    }

    /**
     * Checks if an ItemStack has dynamic color overrides.
     */
    static boolean hasDynamicColors(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_DYNAMIC_COLORS, Tag.TAG_COMPOUND) && !tag.getCompound(NBT_DYNAMIC_COLORS).isEmpty();
    }

    /**
     * Applies dynamic textures and colors from an ItemStack to a target BlockEntity.
     */
    static void applyDynamicDataToBlockEntity(@Nullable BlockEntity be, ItemStack stack) {
        if (be instanceof IDynamicTexturedBlockEntity dtbe) {
            if (hasDynamicTextures(stack)) {
                dtbe.setDynamicTextures(getDynamicTextures(stack));
            }
            if (hasDynamicColors(stack)) {
                dtbe.setDynamicColors(getDynamicColors(stack));
            }
        }
    }

    /**
     * Copies dynamic textures and colors from a BlockEntity into an ItemStack.
     */
    static ItemStack applyDynamicDataToItemStack(ItemStack stack, @Nullable BlockEntity be) {
        if (be instanceof IDynamicTexturedBlockEntity dtbe) {
            if (dtbe.hasDynamicTextures()) {
                setDynamicTextures(stack, dtbe.getDynamicTextures());
            }
            if (dtbe.hasDynamicColors()) {
                setDynamicColors(stack, dtbe.getDynamicColors());
            }
        }
        return stack;
    }

    /**
     * Applies dynamic textures and colors from a BlockEntity to the dropped item.
     */
    static void applyDynamicDataToDrops(List<ItemStack> drops, @Nullable BlockEntity be, Block block) {
        if (be instanceof IDynamicTexturedBlockEntity dtbe && (dtbe.hasDynamicTextures() || dtbe.hasDynamicColors())) {
            for (ItemStack drop : drops) {
                if (drop.getItem() == block.asItem()) {
                    applyDynamicDataToItemStack(drop, be);
                }
            }
        }
    }

    /**
     * Convenience method to call in Block#setPlacedBy.
     */
    default void onDynamicBlockPlacedBy(Level level, BlockPos pos, ItemStack stack) {
        applyDynamicDataToBlockEntity(level.getBlockEntity(pos), stack);
    }

    /**
     * Convenience method to call in Block#getCloneItemStack.
     */
    default ItemStack getDynamicCloneItemStack(ItemStack baseStack, BlockGetter level, BlockPos pos) {
        return applyDynamicDataToItemStack(baseStack, level.getBlockEntity(pos));
    }

    /**
     * Convenience method to call in Block#getDrops.
     */
    default void applyDynamicDrops(List<ItemStack> drops, LootParams.Builder params, Block block) {
        applyDynamicDataToDrops(drops, params.getOptionalParameter(LootContextParams.BLOCK_ENTITY), block);
    }
}

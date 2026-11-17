package net.placemats.common.block;

import java.util.*;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.registries.ForgeRegistries;
import net.placemats.PlaceMatMain;
import net.placemats.common.blockentity.IDynamicTexturedBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for blocks that support custom dynamic textures.
 */
@SuppressWarnings({"unused", "deprecation"})
public interface IDynamicTexturedBlock {

    String NBT_DYNAMIC_TEXTURES = "DynamicTextures";
    String NBT_DYNAMIC_COLORS = "DynamicColors";
    String NBT_DYNAMIC_EMISSIVES = "DynamicEmissives";
    String NBT_DYNAMIC_MATERIAL = "DynamicMaterial";
    String NBT_DYNAMIC_MATERIALS = "DynamicMaterials";

    /**
     * Enum for colors by keywords for Easter eggs and faster coloring.
     * 8-character limit for strings.
     */
    @Getter
    enum KeywordColor {
        // Easter Eggs
        RAINBOW(0x00726564, List.of("rainbow", "pride")),
        TRANS(0x0030ddff, List.of("trans"), new int[] {
            0x5BCEFA, // Blue
            0xF5A9B8, // Pink
            0xFFFFFF, // White
            0xF5A9B8  // Pink
        }, 8000L),
        ICE_CREAM(0x00fff2cf, List.of("icecream", "polly"), new int[] {
            0xfff2cf, // vanilla
            0x2b1b14, // chocolate
            0xf25a97 // strawberry
        }, 8000L),
        TFG(0x00d12ea6, List.of(
            "redeix", "pyritie", "tomtom", "gustavo", "ashlen",
            "xikaro", "zippity", "yan", "airrice", "dmitry",
            "gamestar", "nebby", "sakura", "tfg"), new int[] {
            0x360431,
            0x86036D,
            0xCD0479,
            0xFD6C7F,
            0xCD0479,
            0x86036D
        }, 8000L),
        // Misc
        CHORUS(0xFF956595, List.of("chorus", "shulk")),
        BRICK(0xFF995542, List.of("brick")),
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
        MANGROVE(0xFF753833, List.of("mangrove")),
        CHERRY(0xFFDA9B95, List.of("cherry")),
        CRIMSON(0xFF7E3752, List.of("crimson")),
        WARPED(0xFF308B8A, List.of("warped")),
        ASH(0xFFa35949, List.of("ash")),
        ASPEN(0xFF464631, List.of("aspen")),
        CHESTNUT(0xFF6c3c2b, List.of("chestnut")),
        FIR(0xFFd4ae83, List.of("fir", "dfir")),
        HICKORY(0xFF4e3118, List.of("hickory")),
        KAPOK(0xFFa47c96, List.of("kapok")),
        MAPLE(0xFF9b6b38, List.of("maple")),
        PALM(0xFFac6932, List.of("palm")),
        PINE(0xFFc6b393, List.of("pine")),
        ROSEWOOD(0xFF8d2227, List.of("rosewood", "rose")),
        SEQUOIA(0xFF7e4937, List.of("sequoia")),
        SYCAMORE(0xFFbc8839, List.of("sycamore")),
        CEDAR(0xFFb6aba4, List.of("cedar", "wcedar")),
        WILLOW(0xFF343e09, List.of("willow")),
        // Metals
        COPPER(0xFFbf6a4b, List.of("copper")),
        EXPOSED_COPPER(0xFFa67561, List.of("ecopper")),
        WEATHERED_COPPER(0xFF65a775, List.of("wcopper")),
        OXIDIZED_COPPER(0xFF4ea98e, List.of("ocopper")),
        IRON(0xFFdddddd, List.of("iron")),
        GOLD(0xFFfbe94e, List.of("gold")),
        // Gems
        EMERALD(0xFF17da61, List.of("emerald")),
        DIAMOND(0xFF64f2e0, List.of("diamond")),
        AMETHYST(0xFFa476ee, List.of("amethyst"));

        @Getter
        private final int colorCode;
        @Getter
        private final List<String> keywords;
        @Getter
        private final int @Nullable [] multiColors;
        @Getter
        private final @Nullable Long speed;

        KeywordColor(int colorCode, List<String> keywords) {
            this(colorCode, keywords, null, null);
        }

        KeywordColor(int colorCode, List<String> keywords, int @Nullable [] multiColors, @Nullable Long speed) {
            this.colorCode = colorCode;
            this.keywords = keywords;
            this.multiColors = multiColors;
            this.speed = speed;
        }

        public boolean matches(@Nullable Integer color) {
            return color != null && color == this.colorCode;
        }

        public static @Nullable KeywordColor fromColor(@Nullable Integer color) {
            if (color == null) {
                return null;
            }
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
     * Checks if the color is for any multicolor-cycling Easter egg.
     */
    static boolean isMultiColor(@Nullable Integer color) {
        KeywordColor keywordColor = KeywordColor.fromColor(color);
        return keywordColor != null && keywordColor.multiColors != null;
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
     * @return Label for this key slot.
     */
    Component getTextureKeyName(int keyIndex);

    /**
     * @return Vertical Y-offset (in GUI pixels) applied when rendering the 3D preview in the cabinetmaker's table screen.
     * Negative values shift the preview upward, positive values shift downward.
     */
    default float getPreviewYOffset() {
        return 0.0f;
    }

    /**
     * @return true if the preview in the cabinetmaker's table screen should render the item model instead of the blockstate model list.
     */
    default boolean shouldPreviewItemModel() {
        return false;
    }

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
     * Extracts dynamic emissive overrides from an ItemStack's NBT.
     */
    static Map<String, Integer> getDynamicEmissives(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptyMap();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_DYNAMIC_EMISSIVES, Tag.TAG_COMPOUND)) {
            return Collections.emptyMap();
        }
        CompoundTag deTag = tag.getCompound(NBT_DYNAMIC_EMISSIVES);
        Map<String, Integer> map = new HashMap<>();
        for (String key : deTag.getAllKeys()) {
            map.put(key, deTag.getInt(key));
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
     * Saves dynamic emissive overrides to an ItemStack's NBT.
     */
    static void setDynamicEmissives(ItemStack stack, Map<String, Integer> emissives) {
        if (stack.isEmpty()) {
            return;
        }
        if (emissives.isEmpty()) {
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                stack.getTag().remove(NBT_DYNAMIC_EMISSIVES);
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        CompoundTag deTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : emissives.entrySet()) {
            deTag.putInt(entry.getKey(), entry.getValue());
        }
        stack.addTagElement(NBT_DYNAMIC_EMISSIVES, deTag);
    }

    /**
     * Gets a BlockState from dynamic textures by matching texture path names against registered blocks.
     */
    @Nullable
    static BlockState resolveBlockStateFromTextures(@Nullable Map<String, ResourceLocation> textures) {
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        ResourceLocation tex0 = textures.get("0");
        if (tex0 == null) {
            tex0 = textures.values().iterator().next();
        }
        return resolveBlockStateFromTexture(tex0);
    }

    /**
     * Resolves all possible BlockStates from dynamic textures by matching texture path names against registered blocks.
     */
    static Map<String, BlockState> resolveBlockStatesFromTextures(@Nullable Map<String, ResourceLocation> textures) {
        if (textures == null || textures.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, BlockState> result = new HashMap<>();
        for (Map.Entry<String, ResourceLocation> entry : textures.entrySet()) {
            BlockState state = resolveBlockStateFromTexture(entry.getValue());
            if (state != null && !state.isAir()) {
                result.put(entry.getKey(), state);
            }
        }
        return result;
    }

    /**
     * Resolves a BlockState from a single texture by matching texture path names against registered blocks.
     */
    @Nullable
    static BlockState resolveBlockStateFromTexture(@Nullable ResourceLocation texture) {
        if (texture == null) {
            return null;
        }
        String namespace = texture.getNamespace();
        if (namespace.equals(PlaceMatMain.MOD_ID)) {
            return null;
        }
        String blockName = texture.getPath();
        int lastSlash = blockName.lastIndexOf('/');
        if (lastSlash != -1) {
            blockName = blockName.substring(lastSlash + 1);
        }
        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(namespace, blockName);
        if (ForgeRegistries.BLOCKS.containsKey(blockId)) {
            var block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null || block instanceof IDynamicTexturedBlock) {
                return null;
            }
            return block.defaultBlockState();
        }
        return null;
    }

    /**
     * Resolves material BlockStates for all texture slots from an ItemStack's NBT.
     */
    static Map<String, BlockState> getDynamicMaterials(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptyMap();
        }
        CompoundTag tag = stack.getTag();
        Map<String, BlockState> result = new HashMap<>();
        if (tag != null && tag.contains(NBT_DYNAMIC_MATERIALS, Tag.TAG_COMPOUND)) {
            CompoundTag materialsTag = tag.getCompound(NBT_DYNAMIC_MATERIALS);
            for (String key : materialsTag.getAllKeys()) {
                if (materialsTag.contains(key, Tag.TAG_COMPOUND)) {
                    BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), materialsTag.getCompound(key));
                    if (!state.isAir()) {
                        result.put(key, state);
                    }
                }
            }
        }
        if (!result.containsKey("0") && tag != null && tag.contains(NBT_DYNAMIC_MATERIAL, Tag.TAG_COMPOUND)) {
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(NBT_DYNAMIC_MATERIAL));
            if (!state.isAir()) {
                result.put("0", state);
            }
        }
        Map<String, ResourceLocation> textures = getDynamicTextures(stack);
        for (Map.Entry<String, ResourceLocation> entry : textures.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                BlockState resolved = resolveBlockStateFromTexture(entry.getValue());
                if (resolved != null && !resolved.isAir()) {
                    result.put(entry.getKey(), resolved);
                }
            }
        }
        return result;
    }

    /**
     * Extracts dynamic material override from an ItemStack's NBT.
     */
    @Nullable
    static BlockState getDynamicMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_DYNAMIC_MATERIALS, Tag.TAG_COMPOUND)) {
            CompoundTag materialsTag = tag.getCompound(NBT_DYNAMIC_MATERIALS);
            if (materialsTag.contains("0", Tag.TAG_COMPOUND)) {
                return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), materialsTag.getCompound("0"));
            }
        }
        if (tag != null && tag.contains(NBT_DYNAMIC_MATERIAL, Tag.TAG_COMPOUND)) {
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(NBT_DYNAMIC_MATERIAL));
        }
        return resolveBlockStateFromTextures(getDynamicTextures(stack));
    }

    @Nullable
    static BlockState getMaterialBlockState(ItemStack stack) {
        return getDynamicMaterial(stack);
    }

    /**
     * Saves dynamic materials mapping to an ItemStack's NBT.
     */
    static void setDynamicMaterials(ItemStack stack, @Nullable Map<String, BlockState> materials) {
        if (stack.isEmpty()) {
            return;
        }
        if (materials == null || materials.isEmpty()) {
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                stack.getTag().remove(NBT_DYNAMIC_MATERIALS);
                stack.getTag().remove(NBT_DYNAMIC_MATERIAL);
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        CompoundTag materialsTag = new CompoundTag();
        materials.forEach((k, v) -> materialsTag.put(k, NbtUtils.writeBlockState(v)));
        stack.addTagElement(NBT_DYNAMIC_MATERIALS, materialsTag);
        if (materials.containsKey("0") && materials.get("0") != null) {
            stack.addTagElement(NBT_DYNAMIC_MATERIAL, NbtUtils.writeBlockState(materials.get("0")));
        } else if (stack.hasTag()) {
            assert stack.getTag() != null;
            stack.getTag().remove(NBT_DYNAMIC_MATERIAL);
        }
    }

    /**
     * Saves dynamic material override to an ItemStack's NBT.
     */
    static void setDynamicMaterial(ItemStack stack, @Nullable BlockState material) {
        if (stack.isEmpty()) {
            return;
        }
        if (material == null) {
            if (stack.hasTag()) {
                assert stack.getTag() != null;
                stack.getTag().remove(NBT_DYNAMIC_MATERIAL);
                if (stack.getTag().contains(NBT_DYNAMIC_MATERIALS, Tag.TAG_COMPOUND)) {
                    stack.getTag().getCompound(NBT_DYNAMIC_MATERIALS).remove("0");
                    if (stack.getTag().getCompound(NBT_DYNAMIC_MATERIALS).isEmpty()) {
                        stack.getTag().remove(NBT_DYNAMIC_MATERIALS);
                    }
                }
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
            return;
        }
        stack.addTagElement(NBT_DYNAMIC_MATERIAL, NbtUtils.writeBlockState(material));
        Map<String, BlockState> current = new HashMap<>(getDynamicMaterials(stack));
        current.put("0", material);
        setDynamicMaterials(stack, current);
    }

    static void setMaterialBlockState(ItemStack stack, @Nullable BlockState material) {
        setDynamicMaterial(stack, material);
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
     * Checks if an ItemStack has dynamic emissive overrides.
     */
    static boolean hasDynamicEmissives(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(NBT_DYNAMIC_EMISSIVES, Tag.TAG_COMPOUND) && !tag.getCompound(NBT_DYNAMIC_EMISSIVES).isEmpty();
    }

    /**
     * Checks if an ItemStack has dynamic material override.
     */
    static boolean hasDynamicMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && (
            (tag.contains(NBT_DYNAMIC_MATERIALS, Tag.TAG_COMPOUND) && !tag.getCompound(NBT_DYNAMIC_MATERIALS).isEmpty())
            || (tag.contains(NBT_DYNAMIC_MATERIAL, Tag.TAG_COMPOUND) && !tag.getCompound(NBT_DYNAMIC_MATERIAL).isEmpty())
        );
    }

    /**
     * Checks if an ItemStack has dynamic materials mapping.
     */
    static boolean hasDynamicMaterials(ItemStack stack) {
        return hasDynamicMaterial(stack);
    }

    /**
     * Applies dynamic textures, colors, emissives, and material from an ItemStack to a target BlockEntity.
     */
    static void applyDynamicDataToBlockEntity(@Nullable BlockEntity be, ItemStack stack) {
        if (be instanceof IDynamicTexturedBlockEntity dtbe) {
            if (hasDynamicTextures(stack)) {
                dtbe.setDynamicTextures(getDynamicTextures(stack));
            }
            if (hasDynamicColors(stack)) {
                dtbe.setDynamicColors(getDynamicColors(stack));
            }
            if (hasDynamicEmissives(stack)) {
                dtbe.setDynamicEmissives(getDynamicEmissives(stack));
            }
            if (hasDynamicMaterials(stack)) {
                dtbe.setDynamicMaterials(getDynamicMaterials(stack));
            } else if (hasDynamicMaterial(stack)) {
                dtbe.setDynamicMaterial(getDynamicMaterial(stack));
            }
        }
    }

    /**
     * Applies dynamic textures, colors, emissives, and material from a BlockEntity to an ItemStack.
     */
    static ItemStack applyDynamicDataToItemStack(ItemStack stack, @Nullable BlockEntity be) {
        if (be instanceof IDynamicTexturedBlockEntity dtbe) {
            if (dtbe.hasDynamicTextures()) {
                setDynamicTextures(stack, dtbe.getDynamicTextures());
            }
            if (dtbe.hasDynamicColors()) {
                setDynamicColors(stack, dtbe.getDynamicColors());
            }
            if (dtbe.hasDynamicEmissives()) {
                setDynamicEmissives(stack, dtbe.getDynamicEmissives());
            }
            if (dtbe.hasDynamicMaterials()) {
                setDynamicMaterials(stack, dtbe.getDynamicMaterials());
            } else if (dtbe.hasDynamicMaterial()) {
                setDynamicMaterial(stack, dtbe.getDynamicMaterial());
            }
        }
        return stack;
    }

    /**
     * Applies dynamic textures, colors, emissives, and material from a BlockEntity to the dropped item.
     */
    static void applyDynamicDataToDrops(List<ItemStack> drops, @Nullable BlockEntity be, Block block) {
        if (be instanceof IDynamicTexturedBlockEntity dtbe && (dtbe.hasDynamicTextures() || dtbe.hasDynamicColors() || dtbe.hasDynamicEmissives() || dtbe.hasDynamicMaterial() || dtbe.hasDynamicMaterials())) {
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

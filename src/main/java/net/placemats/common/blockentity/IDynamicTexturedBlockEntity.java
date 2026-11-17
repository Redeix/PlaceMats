package net.placemats.common.blockentity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.placemats.client.renderer.DynamicTexturedBakedModel;
import net.placemats.common.block.IDynamicTexturedBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for block entities that store and provide dynamic texture and color overrides.
 */
@SuppressWarnings({"unused", "deprecation"})
public interface IDynamicTexturedBlockEntity {

    /**
     * @return Map of texture slot keys to ResourceLocation overrides.
     */
    Map<String, ResourceLocation> getDynamicTextures();

    /**
     * Sets texture slot overrides and marks the block entity synced.
     *
     * @param textures Map of texture slot keys to ResourceLocations.
     */
    void setDynamicTextures(Map<String, ResourceLocation> textures);

    /**
     * @return Map of texture slot keys to ARGB integer color overrides.
     */
    Map<String, Integer> getDynamicColors();

    /**
     * Sets color slot overrides and marks the block entity synced.
     *
     * @param colors Map of texture slot keys to ARGB integer color overrides.
     */
    void setDynamicColors(Map<String, Integer> colors);

    /**
     * @return Map of texture slot keys to emissive overrides (0-15).
     */
    default Map<String, Integer> getDynamicEmissives() {
        return Collections.emptyMap();
    }

    /**
     * Sets emissive slot overrides and marks the block entity synced.
     *
     * @param emissives Map of texture slot keys to emissive overrides (0-15).
     */
    default void setDynamicEmissives(Map<String, Integer> emissives) {}

    /**
     * @return True if any dynamic emissive override is present.
     */
    default boolean hasDynamicEmissives() {
        return !getDynamicEmissives().isEmpty();
    }

    /**
     * @return Material BlockState overrides for each layer.
     *
     * Currently, not really working besides the first layer.
     */
    default Map<String, BlockState> getDynamicMaterials() {
        BlockState single = getDynamicMaterial();
        return single != null ? Collections.singletonMap("0", single) : Collections.emptyMap();
    }

    /**
     * Sets material BlockState overrides and marks the block entity synced.
     *
     * @param materials Map of texture slot keys to material BlockState overrides.
     */
    default void setDynamicMaterials(Map<String, BlockState> materials) {
        if (materials != null && materials.containsKey("0")) {
            setDynamicMaterial(materials.get("0"));
        } else {
            setDynamicMaterial(null);
        }
    }

    /**
     * @return True if any material BlockState override is present.
     */
    default boolean hasDynamicMaterials() {
        return !getDynamicMaterials().isEmpty() || hasDynamicMaterial();
    }

    /**
     * @return Material BlockState override for the primary slot.
     */
    @Nullable
    default BlockState getDynamicMaterial() {
        return getDynamicMaterials().get("0");
    }

    /**
     * Sets material BlockState override for the primary slot and marks the block entity synced.
     *
     * @param material Material BlockState override.
     */
    default void setDynamicMaterial(@Nullable BlockState material) {}

    /**
     * @return True if a material BlockState override is present.
     */
    default boolean hasDynamicMaterial() {
        return getDynamicMaterial() != null;
    }

    /**
     * @return True if any texture slot override is present.
     */
    default boolean hasDynamicTextures() {
        return !getDynamicTextures().isEmpty();
    }

    /**
     * @return True if any color slot override is present.
     */
    default boolean hasDynamicColors() {
        return !getDynamicColors().isEmpty();
    }

    /**
     * @return True if any active color is set to the animated rainbow keyword.
     */
    default boolean hasRainbowColor() {
        for (Integer color : getDynamicColors().values()) {
            if (IDynamicTexturedBlock.isRainbowColor(color)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if any active color key has multi-color animations.
     */
    default boolean hasMultiColor() {
        for (Integer color : getDynamicColors().values()) {
            if (IDynamicTexturedBlock.isMultiColor(color)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return True if any active color has translucency.
     */
    default boolean hasTranslucentColor() {
        for (Integer color : getDynamicColors().values()) {
            if (color != null && !IDynamicTexturedBlock.isRainbowColor(color) && !IDynamicTexturedBlock.isMultiColor(color)) {
                int alpha = (color >>> 24) & 0xFF;
                if (alpha > 0 && alpha < 255) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Builds ModelData containing dynamic textures, colors, emissives, and material properties.
     */
    default ModelData getDynamicModelData(ModelData baseData) {
        if (hasDynamicTextures() || hasDynamicColors() || hasDynamicEmissives() || hasDynamicMaterial()) {
            ModelData.Builder builder = ModelData.builder();
            if (hasDynamicTextures()) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_TEXTURES_PROPERTY, new HashMap<>(getDynamicTextures()));
            }
            if (hasDynamicColors()) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_COLORS_PROPERTY, new HashMap<>(getDynamicColors()));
            }
            if (hasDynamicEmissives()) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_EMISSIVES_PROPERTY, new HashMap<>(getDynamicEmissives()));
            }
            if (hasDynamicMaterial()) {
                builder.with(DynamicTexturedBakedModel.MATERIAL_STATE_PROPERTY, getDynamicMaterial());
            }
            return builder.build();
        }
        return baseData != null ? baseData : ModelData.EMPTY;
    }

    /**
     * Builds ModelData containing dynamic textures, colors, and emissives properties.
     */
    default ModelData getDynamicModelData() {
        return getDynamicModelData(ModelData.EMPTY);
    }

    /**
     * Serializes dynamic textures, colors, and emissives to NBT.
     */
    default void saveDynamicData(CompoundTag tag) {
        saveDynamicData(tag, getDynamicTextures(), getDynamicColors(), getDynamicEmissives(), getDynamicMaterials());
    }

    /**
     * Deserializes dynamic textures, colors, emissives, and material from NBT.
     */
    default void loadDynamicData(CompoundTag tag) {
        Map<String, ResourceLocation> textures = new HashMap<>();
        Map<String, Integer> colors = new HashMap<>();
        Map<String, Integer> emissives = new HashMap<>();
        loadDynamicData(tag, textures, colors, emissives);
        setDynamicTextures(textures);
        setDynamicColors(colors);
        setDynamicEmissives(emissives);
        Map<String, BlockState> materials = loadDynamicMaterials(tag);
        if (!materials.isEmpty()) {
            setDynamicMaterials(materials);
        } else {
            setDynamicMaterial(loadDynamicMaterial(tag));
        }
    }

    /**
     * Serializes texture and color maps into the compound tag.
     */
    static void saveDynamicData(CompoundTag tag, Map<String, ResourceLocation> textures, Map<String, Integer> colors) {
        saveDynamicData(tag, textures, colors, Collections.emptyMap(), (BlockState) null);
    }

    /**
     * Serializes texture, color maps, and material into the compound tag.
     */
    static void saveDynamicData(CompoundTag tag, Map<String, ResourceLocation> textures, Map<String, Integer> colors, @Nullable BlockState material) {
        Map<String, BlockState> materials = material != null ? Collections.singletonMap("0", material) : null;
        saveDynamicData(tag, textures, colors, Collections.emptyMap(), materials);
    }

    /**
     * Serializes texture, color, and emissive maps and material into the compound tag.
     */
    static void saveDynamicData(CompoundTag tag, Map<String, ResourceLocation> textures, Map<String, Integer> colors, Map<String, Integer> emissives, @Nullable BlockState material) {
        Map<String, BlockState> materials = material != null ? Collections.singletonMap("0", material) : null;
        saveDynamicData(tag, textures, colors, emissives, materials);
    }

    /**
     * Serializes texture, color, emissive maps, and materials mapping into the compound tag.
     */
    static void saveDynamicData(CompoundTag tag, Map<String, ResourceLocation> textures, Map<String, Integer> colors, Map<String, Integer> emissives, @Nullable Map<String, BlockState> materials) {
        if (textures != null && !textures.isEmpty()) {
            CompoundTag dtTag = new CompoundTag();
            textures.forEach((k, v) -> {
                if (v != null) {
                    dtTag.putString(k, v.toString());
                }
            });
            tag.put(IDynamicTexturedBlock.NBT_DYNAMIC_TEXTURES, dtTag);
        }
        if (colors != null && !colors.isEmpty()) {
            CompoundTag dcTag = new CompoundTag();
            colors.forEach((k, v) -> {
                if (v != null) {
                    dcTag.putInt(k, v);
                }
            });
            tag.put(IDynamicTexturedBlock.NBT_DYNAMIC_COLORS, dcTag);
        }
        if (emissives != null && !emissives.isEmpty()) {
            CompoundTag deTag = new CompoundTag();
            emissives.forEach((k, v) -> {
                if (v != null) {
                    deTag.putInt(k, v);
                }
            });
            tag.put(IDynamicTexturedBlock.NBT_DYNAMIC_EMISSIVES, deTag);
        }
        if (materials != null && !materials.isEmpty()) {
            CompoundTag dmTag = new CompoundTag();
            materials.forEach((k, v) -> {
                if (v != null) {
                    dmTag.put(k, NbtUtils.writeBlockState(v));
                }
            });
            tag.put(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIALS, dmTag);
            if (materials.containsKey("0") && materials.get("0") != null) {
                tag.put(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIAL, NbtUtils.writeBlockState(materials.get("0")));
            }
        }
    }

    /**
     * Deserializes texture and color maps from the compound tag.
     */
    static void loadDynamicData(CompoundTag tag, Map<String, ResourceLocation> targetTextures, Map<String, Integer> targetColors) {
        loadDynamicData(tag, targetTextures, targetColors, null);
    }

    /**
     * Deserializes texture, color, and emissive maps from the compound tag.
     */
    static void loadDynamicData(CompoundTag tag, Map<String, ResourceLocation> targetTextures, Map<String, Integer> targetColors, @Nullable Map<String, Integer> targetEmissives) {
        if (targetTextures != null) {
            targetTextures.clear();
            if (tag.contains(IDynamicTexturedBlock.NBT_DYNAMIC_TEXTURES, Tag.TAG_COMPOUND)) {
                CompoundTag dtTag = tag.getCompound(IDynamicTexturedBlock.NBT_DYNAMIC_TEXTURES);
                for (String key : dtTag.getAllKeys()) {
                    ResourceLocation rl = ResourceLocation.tryParse(dtTag.getString(key));
                    if (rl != null) {
                        targetTextures.put(key, rl);
                    }
                }
            }
        }
        if (targetColors != null) {
            targetColors.clear();
            if (tag.contains(IDynamicTexturedBlock.NBT_DYNAMIC_COLORS, Tag.TAG_COMPOUND)) {
                CompoundTag dcTag = tag.getCompound(IDynamicTexturedBlock.NBT_DYNAMIC_COLORS);
                for (String key : dcTag.getAllKeys()) {
                    targetColors.put(key, dcTag.getInt(key));
                }
            }
        }
        if (targetEmissives != null) {
            targetEmissives.clear();
            if (tag.contains(IDynamicTexturedBlock.NBT_DYNAMIC_EMISSIVES, Tag.TAG_COMPOUND)) {
                CompoundTag deTag = tag.getCompound(IDynamicTexturedBlock.NBT_DYNAMIC_EMISSIVES);
                for (String key : deTag.getAllKeys()) {
                    targetEmissives.put(key, deTag.getInt(key));
                }
            }
        }
    }

    /**
     * Deserializes material BlockState from the compound tag.
     */
    @Nullable
    static BlockState loadDynamicMaterial(CompoundTag tag) {
        if (tag.contains(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIAL, Tag.TAG_COMPOUND)) {
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIAL));
        }
        return null;
    }

    /**
     * Deserializes dynamic materials mapping from the compound tag.
     */
    static Map<String, BlockState> loadDynamicMaterials(CompoundTag tag) {
        Map<String, BlockState> materials = new HashMap<>();
        if (tag.contains(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIALS, Tag.TAG_COMPOUND)) {
            CompoundTag dmTag = tag.getCompound(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIALS);
            for (String key : dmTag.getAllKeys()) {
                if (dmTag.contains(key, Tag.TAG_COMPOUND)) {
                    BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), dmTag.getCompound(key));
                    if (!state.isAir()) {
                        materials.put(key, state);
                    }
                }
            }
        }
        if (!materials.containsKey("0") && tag.contains(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIAL, Tag.TAG_COMPOUND)) {
            BlockState single = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(IDynamicTexturedBlock.NBT_DYNAMIC_MATERIAL));
            if (!single.isAir()) {
                materials.put("0", single);
            }
        }
        return materials;
    }

    /**
     * Deserializes texture and color maps using consumer setters.
     */
    static void loadDynamicData(CompoundTag tag, Consumer<Map<String, ResourceLocation>> texturesSetter, Consumer<Map<String, Integer>> colorsSetter) {
        Map<String, ResourceLocation> textures = new HashMap<>();
        Map<String, Integer> colors = new HashMap<>();
        loadDynamicData(tag, textures, colors);
        if (texturesSetter != null) {
            texturesSetter.accept(textures);
        }
        if (colorsSetter != null) {
            colorsSetter.accept(colors);
        }
    }

    /**
     * Creates a ModelData instance from dynamic textures and colors maps.
     */
    static ModelData createDynamicModelData(Map<String, ResourceLocation> textures, Map<String, Integer> colors) {
        return createDynamicModelData(textures, colors, Collections.emptyMap(), (BlockState) null);
    }

    /**
     * Creates a ModelData instance from dynamic textures, colors maps, and material BlockState.
     */
    static ModelData createDynamicModelData(Map<String, ResourceLocation> textures, Map<String, Integer> colors, @Nullable BlockState material) {
        return createDynamicModelData(textures, colors, Collections.emptyMap(), material);
    }

    /**
     * Creates a ModelData instance from dynamic textures, colors, and emissives maps, and material BlockState.
     */
    static ModelData createDynamicModelData(Map<String, ResourceLocation> textures, Map<String, Integer> colors, Map<String, Integer> emissives, @Nullable BlockState material) {
        boolean hasTex = textures != null && !textures.isEmpty();
        boolean hasCol = colors != null && !colors.isEmpty();
        boolean hasEmissive = emissives != null && !emissives.isEmpty();
        boolean hasMat = material != null;
        if (hasTex || hasCol || hasEmissive || hasMat) {
            ModelData.Builder builder = ModelData.builder();
            if (hasTex) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_TEXTURES_PROPERTY, new HashMap<>(textures));
            }
            if (hasCol) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_COLORS_PROPERTY, new HashMap<>(colors));
            }
            if (hasEmissive) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_EMISSIVES_PROPERTY, new HashMap<>(emissives));
            }
            if (hasMat) {
                builder.with(DynamicTexturedBakedModel.MATERIAL_STATE_PROPERTY, material);
            }
            return builder.build();
        }
        return ModelData.EMPTY;
    }

    /**
     * Creates a ModelData instance from dynamic textures, colors, emissives maps, and materials mapping.
     */
    static ModelData createDynamicModelData(Map<String, ResourceLocation> textures, Map<String, Integer> colors, Map<String, Integer> emissives, @Nullable Map<String, BlockState> materials) {
        BlockState material = materials != null ? materials.get("0") : null;
        return createDynamicModelData(textures, colors, emissives, material);
    }
}

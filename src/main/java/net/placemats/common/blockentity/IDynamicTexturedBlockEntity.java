package net.placemats.common.blockentity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;
import net.placemats.client.renderer.DynamicTexturedBakedModel;
import net.placemats.common.block.IDynamicTexturedBlock;

/**
 * Interface for block entities that store and provide dynamic texture and color overrides.
 */
@SuppressWarnings("unused")
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
     * Builds ModelData containing dynamic textures and colors properties.
     */
    default ModelData getDynamicModelData(ModelData baseData) {
        if (hasDynamicTextures() || hasDynamicColors()) {
            ModelData.Builder builder = ModelData.builder();
            if (hasDynamicTextures()) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_TEXTURES_PROPERTY, new HashMap<>(getDynamicTextures()));
            }
            if (hasDynamicColors()) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_COLORS_PROPERTY, new HashMap<>(getDynamicColors()));
            }
            return builder.build();
        }
        return baseData != null ? baseData : ModelData.EMPTY;
    }

    /**
     * Builds ModelData containing dynamic textures and colors properties.
     */
    default ModelData getDynamicModelData() {
        return getDynamicModelData(ModelData.EMPTY);
    }

    /**
     * Serializes dynamic textures and colors to NBT.
     */
    default void saveDynamicData(CompoundTag tag) {
        saveDynamicData(tag, getDynamicTextures(), getDynamicColors());
    }

    /**
     * Deserializes dynamic textures and colors from NBT.
     */
    default void loadDynamicData(CompoundTag tag) {
        Map<String, ResourceLocation> textures = new HashMap<>();
        Map<String, Integer> colors = new HashMap<>();
        loadDynamicData(tag, textures, colors);
        setDynamicTextures(textures);
        setDynamicColors(colors);
    }

    /**
     * Serializes texture and color maps into the compound tag.
     */
    static void saveDynamicData(CompoundTag tag, Map<String, ResourceLocation> textures, Map<String, Integer> colors) {
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
    }

    /**
     * Deserializes texture and color maps from the compound tag.
     */
    static void loadDynamicData(CompoundTag tag, Map<String, ResourceLocation> targetTextures, Map<String, Integer> targetColors) {
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
        boolean hasTex = textures != null && !textures.isEmpty();
        boolean hasCol = colors != null && !colors.isEmpty();
        if (hasTex || hasCol) {
            ModelData.Builder builder = ModelData.builder();
            if (hasTex) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_TEXTURES_PROPERTY, new HashMap<>(textures));
            }
            if (hasCol) {
                builder.with(DynamicTexturedBakedModel.DYNAMIC_COLORS_PROPERTY, new HashMap<>(colors));
            }
            return builder.build();
        }
        return ModelData.EMPTY;
    }
}

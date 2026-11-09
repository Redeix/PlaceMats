package net.placemats.common.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for blocks that support custom dynamic textures per key slot.
 */
public interface IDynamicTexturedBlock {

    String NBT_DYNAMIC_TEXTURES = "DynamicTextures";
    String NBT_DYNAMIC_COLORS = "DynamicColors";

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
}

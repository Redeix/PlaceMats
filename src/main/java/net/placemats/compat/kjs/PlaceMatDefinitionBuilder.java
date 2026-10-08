package net.placemats.compat.kjs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.registries.ForgeRegistries;

import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.typings.Info;

import net.placemats.common.data.resource.DefinitionManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@SuppressWarnings("unused")
public class PlaceMatDefinitionBuilder {
    public final ResourceLocation id;
    private String item;
    private float itemWidth;
    private float itemDepth;
    private ResourceLocation model;
    private ResourceLocation modelRotten;
    private Float itemScale;
    private Boolean layFlat = true;
    private Float itemHeight;
    private boolean stackable = true;
    private boolean allowsStackingOnTop = true;
    private Boolean ignoreItemTints;
    private String fluidTextureIndex;
    private final Map<Integer, Integer> tintColors = new LinkedHashMap<>();
    private Vector3f pos;
    private Vector3f rot;
    private final JsonObject textures = new JsonObject();
    private final JsonObject texturesRotten = new JsonObject();

    public PlaceMatDefinitionBuilder(ResourceLocation id) {
        this.id = id;
    }

    @Info("Sets the item for this definition.")
    public PlaceMatDefinitionBuilder item(Object item) {
        this.item = item.toString();
        return this;
    }

    @Info("Sets the physical width of the item box. (float ratio of 16x16. 1px is 0.0625f)")
    public PlaceMatDefinitionBuilder width(float width) {
        this.itemWidth = width;
        return this;
    }

    @Info("Sets the physical depth of the item box. (float ratio of 16x16. 1px is 0.0625f)")
    public PlaceMatDefinitionBuilder depth(float depth) {
        this.itemDepth = depth;
        return this;
    }

    @Info("Sets the physical width and depth of the item box.")
    public PlaceMatDefinitionBuilder size(float width, float depth) {
        this.itemWidth = width;
        this.itemDepth = depth;
        return this;
    }

    @Info("Sets the physical width, depth, and height of the item box.")
    public PlaceMatDefinitionBuilder size(float width, float depth, float height) {
        this.itemWidth = width;
        this.itemDepth = depth;
        this.itemHeight = height;
        return this;
    }

    @Info("Sets the manual model position offset (x, y, z) where 1f is 16px.")
    public PlaceMatDefinitionBuilder pos(float x, float y, float z) {
        this.pos = new org.joml.Vector3f(x, y, z);
        return this;
    }

    @Info("Sets the manual model rotation offset (roll, pitch, yaw) in degrees.")
    public PlaceMatDefinitionBuilder rot(float roll, float pitch, float yaw) {
        this.rot = new org.joml.Vector3f(roll, pitch, yaw);
        return this;
    }

    @Info("Sets the manual model rotation offset (roll, pitch, yaw) in degrees.")
    public PlaceMatDefinitionBuilder rotation(float roll, float pitch, float yaw) {
        return rot(roll, pitch, yaw);
    }

    @Info("Sets the custom model for the item. (ResourceLocation)")
    public PlaceMatDefinitionBuilder model(Object model) {
        this.model = model == null ? null : ResourceLocation.parse(model.toString());
        return this;
    }

    @Info("Sets the custom model for when the item is rotten. (ResourceLocation)")
    public PlaceMatDefinitionBuilder modelRotten(Object modelRotten) {
        this.modelRotten = modelRotten == null ? null : ResourceLocation.parse(modelRotten.toString());
        return this;
    }

    @Info("Sets the render scale of the item. (float multiplier)")
    public PlaceMatDefinitionBuilder scale(float scale) {
        this.itemScale = scale;
        return this;
    }

    @Info("Sets whether the item should default lay flat. (boolean)")
    public PlaceMatDefinitionBuilder layFlat(boolean layFlat) {
        this.layFlat = layFlat;
        return this;
    }

    @Info("Sets the physical height of the item box. (float ratio of 16x16. 1px is 0.0625f)")
    public PlaceMatDefinitionBuilder height(float height) {
        this.itemHeight = height;
        return this;
    }

    @Info("Disables stacking for this item. (boolean)")
    public PlaceMatDefinitionBuilder disableStacking() {
        this.stackable = false;
        return this;
    }

    @Info("Prevents other items from being stacked on top of this item. (boolean)")
    public PlaceMatDefinitionBuilder preventStackingOnTop() {
        this.allowsStackingOnTop = false;
        return this;
    }

    @Info("Sets whether outside/base item tints should be ignored for this model.")
    public PlaceMatDefinitionBuilder ignoreItemTints(boolean ignore) {
        this.ignoreItemTints = ignore;
        return this;
    }

    @Info("Sets whether outside/base item tints should be ignored for this model.")
    public PlaceMatDefinitionBuilder ignoreItemTints() {
        return ignoreItemTints(true);
    }

    @Info("Sets whether outside/base item tints should be ignored for this model.")
    public PlaceMatDefinitionBuilder overrideTints(boolean override) {
        return ignoreItemTints(override);
    }

    @Info("Sets whether outside/base item tints should be ignored for this model.")
    public PlaceMatDefinitionBuilder overrideTints() {
        return ignoreItemTints(true);
    }

    @Info("Sets the fluid texture index to be dynamically textured with contained fluid.")
    public PlaceMatDefinitionBuilder fluidTexture(int textureIndex) {
        this.fluidTextureIndex = String.valueOf(textureIndex);
        return this;
    }

    @Info("Sets the fluid texture index to be dynamically textured with contained fluid.")
    public PlaceMatDefinitionBuilder fluidTexture(String textureIndex) {
        this.fluidTextureIndex = textureIndex;
        return this;
    }

    @Info("Sets the fluid texture index to be dynamically textured with contained fluid.")
    public PlaceMatDefinitionBuilder fluidTextureIndex(int textureIndex) {
        return fluidTexture(textureIndex);
    }

    @Info("Sets the fluid texture index to be dynamically textured with contained fluid.")
    public PlaceMatDefinitionBuilder fluidTextureIndex(String textureIndex) {
        return fluidTexture(textureIndex);
    }

    @Info("Sets custom tint color for a specific tint index.")
    public PlaceMatDefinitionBuilder tint(int tintIndex, int color) {
        if ((color & 0xFF000000) == 0) {
            color |= 0xFF000000;
        }
        this.tintColors.put(tintIndex, color);
        return this;
    }

    @Info("Sets custom tint color for a specific tint index.")
    public PlaceMatDefinitionBuilder tint(int tintIndex, int r, int g, int b) {
        return tint(tintIndex, (255 << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    @Info("Sets custom tint color for a specific tint index.")
    public PlaceMatDefinitionBuilder tint(int tintIndex, int a, int r, int g, int b) {
        return tint(tintIndex, ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    @Info("Sets custom tint color from a hex string (e.g. '#fc8c03' or 'fc8c03').")
    public PlaceMatDefinitionBuilder tint(int tintIndex, String hexColor) {
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }
        long val = Long.parseLong(hexColor, 16);
        if (hexColor.length() <= 6) {
            val |= 0xFF000000L;
        }
        return tint(tintIndex, (int) val);
    }

    @Info("Sets custom tint color for a specific tint index.")
    public PlaceMatDefinitionBuilder color(int tintIndex, int color) {
        return tint(tintIndex, color);
    }

    @Info("Sets custom tint color for a specific tint index.")
    public PlaceMatDefinitionBuilder color(int tintIndex, int r, int g, int b) {
        return tint(tintIndex, r, g, b);
    }

    @Info("Sets custom tint color for a specific tint index.")
    public PlaceMatDefinitionBuilder color(int tintIndex, int a, int r, int g, int b) {
        return tint(tintIndex, a, r, g, b);
    }

    @Info("Sets custom tint color from a hex string.")
    public PlaceMatDefinitionBuilder color(int tintIndex, String hexColor) {
        return tint(tintIndex, hexColor);
    }

    @Info("Sets multiple tint colors.")
    public PlaceMatDefinitionBuilder tints(java.util.Map<Integer, Integer> tints) {
        tints.forEach(this::tint);
        return this;
    }

    @Info("Sets multiple tint colors.")
    public PlaceMatDefinitionBuilder colors(java.util.Map<Integer, Integer> colors) {
        return tints(colors);
    }

    @Info("Overrides a texture layer for the model.")
    public PlaceMatDefinitionBuilder modelTexture(String layer, String texture) {
        this.textures.addProperty(layer, texture);
        return this;
    }

    @Info("Overrides a texture layer for the rotten model.")
    public PlaceMatDefinitionBuilder modelRottenTexture(String layer, String texture) {
        this.texturesRotten.addProperty(layer, texture);
        return this;
    }

    public void apply(java.util.Map<Item, DefinitionManager.PlaceMatDefinition> definitions,
            java.util.Map<net.minecraft.tags.TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions) {
        if (item == null)
            return;
        DefinitionManager.PlaceMatDefinition def = getDef();

        // If textures are overridden, the model ResourceLocation in the definition needs to point to the generated one.
        if (textures.size() > 0) {
            def = new DefinitionManager.PlaceMatDefinition(def.size(), ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_item/" + id.getPath()), def.modelRotten(), def.scale(),
                    def.flat(), def.itemHeight(), def.stackable(), def.allowsStackingOnTop(), def.ignoreItemTints(), def.fluidTextureIndex(), def.tintColors(), pos, rot, java.util.List.of());
        }
        if (texturesRotten.size() > 0) {
            def = new DefinitionManager.PlaceMatDefinition(def.size(), def.model(), ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_item/" + id.getPath() + "_rotten"), def.scale(),
                    def.flat(), def.itemHeight(), def.stackable(), def.allowsStackingOnTop(), def.ignoreItemTints(), def.fluidTextureIndex(), def.tintColors(), pos, rot, java.util.List.of());
        }

        if (item.startsWith("#")) {
            var tagKey = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).createTagKey(ResourceLocation.parse(item.substring(1)));
            tagDefinitions.put(tagKey, def);
        } else {
            Item i = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(item));
            if (i != null) {
                definitions.put(i, def);
            }
        }
    }

    private DefinitionManager.@NotNull PlaceMatDefinition getDef()
    {
        boolean flat = !Boolean.FALSE.equals(layFlat);
        boolean ignoreTints = Boolean.TRUE.equals(ignoreItemTints);
        return new DefinitionManager.PlaceMatDefinition(
                new Vec2(itemWidth * (flat ? 0.75f : 1.0f), itemDepth * (flat ? 0.75f : 1.0f)),
                model, modelRotten, itemScale != null ? itemScale : (flat ? 0.375f : 0.5f),
                flat, itemHeight, stackable, allowsStackingOnTop, ignoreTints, fluidTextureIndex, tintColors.isEmpty() ? Map.of() : Map.copyOf(tintColors), pos, rot, java.util.List.of());
    }

    public void generateDataJsons(DataJsonGenerator generator) {
        generator.json(ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_definitions/" + id.getPath()), createJson());
    }

    public void generateAssetJsons(AssetJsonGenerator generator) {
        if (textures.size() > 0) {
            JsonObject modelJson = new JsonObject();
            modelJson.addProperty("parent", model != null ? model.toString() : "minecraft:item/generated");

            JsonObject mappedTextures = new JsonObject();
            textures.entrySet().forEach(entry -> mappedTextures.add(entry.getKey(), entry.getValue()));

            modelJson.add("textures", mappedTextures);
            generator.json(ResourceLocation.fromNamespaceAndPath("place_mats", "models/place_mat_item/" + id.getPath()), modelJson);
        }

        if (texturesRotten.size() > 0) {
            JsonObject modelJson = new JsonObject();
            modelJson.addProperty("parent", modelRotten != null ? modelRotten.toString() : "minecraft:item/generated");

            JsonObject mappedTextures = new JsonObject();
            texturesRotten.entrySet().forEach(entry -> mappedTextures.add(entry.getKey(), entry.getValue()));

            modelJson.add("textures", mappedTextures);
            generator.json(ResourceLocation.fromNamespaceAndPath("place_mats", "models/place_mat_item/" + id.getPath() + "_rotten"), modelJson);
        }

        generator.json(ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_definitions/" + id.getPath()), createJson());
    }

    private JsonObject createJson() {
        JsonObject json = new JsonObject();
        if (item != null) {
            if (item.startsWith("#")) {
                json.addProperty("tag", item.substring(1));
            } else {
                json.addProperty("item", item);
            }
        }
        if (itemWidth != 0.25f || itemDepth != 0.25f || itemHeight != null) {
            JsonArray arr = new JsonArray();
            arr.add(itemWidth);
            arr.add(itemDepth);
            if (itemHeight != null) {
                arr.add(itemHeight);
            }
            json.add("size", arr);
        } else {
            JsonArray arr = new JsonArray();
            arr.add(itemWidth);
            arr.add(itemDepth);
            json.add("size", arr);
        }

        ResourceLocation modelLoc = model;
        if (textures.size() > 0) {
            modelLoc = ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_item/" + id.getPath());
        }

        ResourceLocation modelRottenLoc = modelRotten;
        if (texturesRotten.size() > 0) {
            modelRottenLoc = ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_item/" + id.getPath() + "_rotten");
        }

        if (modelLoc != null)
            json.addProperty("model", modelLoc.toString());
        if (modelRottenLoc != null)
            json.addProperty("model_rotten", modelRottenLoc.toString());
        if (itemScale != null)
            json.addProperty("scale", itemScale);
        if (layFlat != null)
            json.addProperty("lay_flat", layFlat);
        if (!stackable)
            json.addProperty("stackable", false);
        if (!allowsStackingOnTop)
            json.addProperty("allows_stacking_on_top", false);
        if (pos != null) {
            JsonArray arr = new JsonArray();
            arr.add(pos.x);
            arr.add(pos.y);
            arr.add(pos.z);
            json.add("pos", arr);
        }
        if (rot != null) {
            JsonArray arr = new JsonArray();
            arr.add(rot.x);
            arr.add(rot.y);
            arr.add(rot.z);
            json.add("rot", arr);
        }
        if (ignoreItemTints != null)
            json.addProperty("ignore_item_tints", ignoreItemTints);
        if (fluidTextureIndex != null)
            json.addProperty("fluid_texture_index", fluidTextureIndex);
        if (!tintColors.isEmpty()) {
            JsonObject tints = new JsonObject();
            tintColors.forEach((idx, col) -> tints.addProperty(String.valueOf(idx), col));
            json.add("tint_colors", tints);
        }
        return json;
    }
}

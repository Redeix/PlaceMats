package net.placemats.compat.kjs;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.registries.ForgeRegistries;

import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.typings.Info;

import net.placemats.common.data.resource.DefinitionManager;

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
        boolean flat = !Boolean.FALSE.equals(layFlat);
        DefinitionManager.PlaceMatDefinition def = new DefinitionManager.PlaceMatDefinition(
                new Vec2(itemWidth * (flat ? 0.75f : 1.0f), itemDepth * (flat ? 0.75f : 1.0f)),
                model, modelRotten, itemScale != null ? itemScale : (flat ? 0.375f : 0.5f),
                flat, itemHeight, stackable, allowsStackingOnTop);

        // If textures are overridden, the model ResourceLocation in the definition needs to point to the generated one.
        if (textures.size() > 0) {
            def = new DefinitionManager.PlaceMatDefinition(def.size(), ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_item/" + id.getPath()), def.modelRotten(), def.scale(),
                    def.flat(), def.itemHeight(), def.stackable(), def.allowsStackingOnTop());
        }
        if (texturesRotten.size() > 0) {
            def = new DefinitionManager.PlaceMatDefinition(def.size(), def.model(), ResourceLocation.fromNamespaceAndPath("place_mats", "place_mat_item/" + id.getPath() + "_rotten"), def.scale(),
                    def.flat(), def.itemHeight(), def.stackable(), def.allowsStackingOnTop());
        }

        if (item.startsWith("#")) {
            var tagKey = ForgeRegistries.ITEMS.tags().createTagKey(ResourceLocation.parse(item.substring(1)));
            tagDefinitions.put(tagKey, def);
        } else {
            Item i = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(item));
            if (i != null) {
                definitions.put(i, def);
            }
        }
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
        json.addProperty("item_width", itemWidth);
        json.addProperty("item_depth", itemDepth);

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
            json.addProperty("item_scale", itemScale);
        if (layFlat != null)
            json.addProperty("lay_flat", layFlat);
        if (itemHeight != null)
            json.addProperty("item_height", itemHeight);
        if (!stackable)
            json.addProperty("stackable", false);
        if (!allowsStackingOnTop)
            json.addProperty("allows_stacking_on_top", false);
        return json;
    }
}

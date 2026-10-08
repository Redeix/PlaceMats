package net.placemats.common.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.placemats.common.datagen.definitions.*;
import net.placemats.common.datagen.definitions.compat.MinecraftDefinitions;
import net.placemats.common.datagen.definitions.compat.TFCDefinitions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generates model definitions for place mats using enum registries.
 */
public class PlaceMatDefinitionProvider implements DataProvider {
    private final PackOutput output;
    private final List<IPlaceMatDefinition[]> definitionGroups = new ArrayList<>();

    public PlaceMatDefinitionProvider(PackOutput output) {
        this.output = output;
    }

    public PlaceMatDefinitionProvider add(IPlaceMatDefinition[] definitions) {
        definitionGroups.add(definitions);
        return this;
    }

    // Enum registries.
    public PlaceMatDefinitionProvider addAll() {
        add(TFCDefinitions.values());
        add(MinecraftDefinitions.values());
        return this;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (IPlaceMatDefinition[] group : definitionGroups) {
            for (IPlaceMatDefinition def : group) {
                Definition d = def.getDefinition();

                collectAndSaveModels(d, futures, cache);

                JsonObject json = new JsonObject();

                String id = d.id;
                String namespace = "minecraft";
                String path = id;

                if (id != null && id.startsWith("#")) {
                    json.addProperty("tag", id.substring(1));
                    path = id.substring(1);
                } else {
                    json.addProperty("item", id);
                }

                if (path != null && path.contains(":")) {
                    String[] split = path.split(":", 2);
                    namespace = split[0];
                    path = split[1];
                }

                if (d.width != null && d.depth != null) {
                    JsonArray sizeArr = new JsonArray();
                    sizeArr.add(d.width);
                    sizeArr.add(d.depth);
                    if (d.height != null) {
                        sizeArr.add(d.height);
                    }
                    json.add("size", sizeArr);
                } else {
                    if (d.width != null) json.addProperty("item_width", d.width);
                    if (d.depth != null) json.addProperty("item_depth", d.depth);
                    if (d.height != null) json.addProperty("item_height", d.height);
                }

                if (d.model != null) json.addProperty("model", d.model);
                if (d.modelRotten != null) json.addProperty("model_rotten", d.modelRotten);
                if (d.scale != null) json.addProperty("scale", d.scale);
                if (d.flat != null) json.addProperty("lay_flat", d.flat);
                if (d.stackable != null) json.addProperty("stackable", d.stackable);
                if (d.allowsStackingOnTop != null) json.addProperty("allows_stacking_on_top", d.allowsStackingOnTop);
                if (d.ignoreItemTints != null) json.addProperty("ignore_item_tints", d.ignoreItemTints);
                if (d.fluidTextureIndex != null) json.addProperty("fluid_texture_index", d.fluidTextureIndex);
                if (!d.tintColors.isEmpty()) {
                    JsonObject tints = new JsonObject();
                    d.tintColors.forEach((idx, col) -> tints.addProperty(String.valueOf(idx), col));
                    json.add("tint_colors", tints);
                }
                if (d.pos != null) {
                    JsonArray arr = new JsonArray();
                    arr.add(d.pos.x);
                    arr.add(d.pos.y);
                    arr.add(d.pos.z);
                    json.add("pos", arr);
                }
                if (d.rot != null) {
                    JsonArray arr = new JsonArray();
                    arr.add(d.rot.x);
                    arr.add(d.rot.y);
                    arr.add(d.rot.z);
                    json.add("rot", arr);
                }

                if (!d.getConditions().isEmpty()) {
                    json.add("conditions", serializeConditions(d.getConditions()));
                }

                assert id != null;
                String fileName = (id.startsWith("#") ? "tag/" : "") + path + ".json";

                Path assetsPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                        .resolve(namespace).resolve("place_mat_definitions").resolve(fileName);
                Path dataPath = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                        .resolve(namespace).resolve("place_mat_definitions").resolve(fileName);

                futures.add(DataProvider.saveStable(cache, json, assetsPath));
                futures.add(DataProvider.saveStable(cache, json, dataPath));
            }
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private void collectAndSaveModels(Definition d, List<CompletableFuture<?>> futures, CachedOutput cache) {
        if (d.modelData != null) {
            if (d.model == null && d.id != null) {
                d.model = inferModelPath(d.id, false);
            }
            if (d.model != null) {
                futures.add(saveModel(cache, d.model, d.modelData));
            }
        }
        if (d.modelRottenData != null) {
            if (d.modelRotten == null && d.id != null) {
                d.modelRotten = inferModelPath(d.id, true);
            }
            if (d.modelRotten != null) {
                futures.add(saveModel(cache, d.modelRotten, d.modelRottenData));
            }
        }
        for (ConditionDefinition cd : d.getConditions()) {
            collectAndSaveModels(cd.definition(), futures, cache);
        }
    }

    private JsonArray serializeConditions(List<ConditionDefinition> conditions) {
        JsonArray array = new JsonArray();
        for (ConditionDefinition cd : conditions) {
            JsonObject condObj = cd.condition().toJson();
            Definition def = cd.definition();
            if (def.width != null && def.depth != null) {
                JsonArray sizeArr = new JsonArray();
                sizeArr.add(def.width);
                sizeArr.add(def.depth);
                if (def.height != null) {
                    sizeArr.add(def.height);
                }
                condObj.add("size", sizeArr);
            } else {
                if (def.width != null) condObj.addProperty("item_width", def.width);
                if (def.depth != null) condObj.addProperty("item_depth", def.depth);
                if (def.height != null) condObj.addProperty("item_height", def.height);
            }
            if (def.model != null) condObj.addProperty("model", def.model);
            if (def.scale != null) condObj.addProperty("scale", def.scale);
            if (def.flat != null) condObj.addProperty("lay_flat", def.flat);
            if (def.stackable != null) condObj.addProperty("stackable", def.stackable);
            if (def.allowsStackingOnTop != null) condObj.addProperty("allows_stacking_on_top", def.allowsStackingOnTop);
            if (def.ignoreItemTints != null) condObj.addProperty("ignore_item_tints", def.ignoreItemTints);
            if (def.fluidTextureIndex != null) condObj.addProperty("fluid_texture_index", def.fluidTextureIndex);
            if (!def.tintColors.isEmpty()) {
                JsonObject tints = new JsonObject();
                def.tintColors.forEach((idx, col) -> tints.addProperty(String.valueOf(idx), col));
                condObj.add("tint_colors", tints);
            }
            if (def.pos != null) {
                JsonArray arr = new JsonArray();
                arr.add(def.pos.x);
                arr.add(def.pos.y);
                arr.add(def.pos.z);
                condObj.add("pos", arr);
            }
            if (def.rot != null) {
                JsonArray arr = new JsonArray();
                arr.add(def.rot.x);
                arr.add(def.rot.y);
                arr.add(def.rot.z);
                condObj.add("rot", arr);
            }
            if (!def.getConditions().isEmpty()) {
                condObj.add("conditions", serializeConditions(def.getConditions()));
            }
            array.add(condObj);
        }
        return array;
    }

    @Override
    public @NotNull String getName() {
        return "Place Mat Definitions";
    }

    private String inferModelPath(String id, boolean rotten) {
        String path = id.startsWith("#") ? "tag/" + id.substring(1) : id;
        if (path.contains(":")) {
            String[] split = path.split(":", 2);
            path = split[0] + "/" + split[1];
        }
        return "place_mats:render/" + path + (rotten ? "_rotten" : "");
    }

    private CompletableFuture<?> saveModel(CachedOutput cache, String modelLoc, ModelData data) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", data.parent());
        JsonObject textures = new JsonObject();
        data.textures().forEach(textures::addProperty);
        json.add("textures", textures);

        String namespace = "minecraft";
        String path = modelLoc;
        if (modelLoc.contains(":")) {
            String[] split = modelLoc.split(":", 2);
            namespace = split[0];
            path = split[1];
        }

        Path modelPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(namespace).resolve("models").resolve(path + ".json");

        return DataProvider.saveStable(cache, json, modelPath);
    }
}

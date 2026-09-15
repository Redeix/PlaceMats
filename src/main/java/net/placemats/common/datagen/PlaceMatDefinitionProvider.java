package net.placemats.common.datagen;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.placemats.common.datagen.definitions.*;
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
        return this;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (IPlaceMatDefinition[] group : definitionGroups) {
            for (IPlaceMatDefinition def : group) {
                Definition d = def.getDefinition();

                if (d.modelData != null) {
                    if (d.model == null) {
                        d.model = inferModelPath(d.id, false);
                    }
                    futures.add(saveModel(cache, d.model, d.modelData));
                }
                if (d.modelRottenData != null) {
                    if (d.modelRotten == null) {
                        d.modelRotten = inferModelPath(d.id, true);
                    }
                    futures.add(saveModel(cache, d.modelRotten, d.modelRottenData));
                }

                JsonObject json = new JsonObject();

                String id = d.id;
                String namespace = "minecraft";
                String path = id;

                if (id.startsWith("#")) {
                    json.addProperty("tag", id.substring(1));
                    path = id.substring(1);
                } else {
                    json.addProperty("item", id);
                }

                if (path.contains(":")) {
                    String[] split = path.split(":", 2);
                    namespace = split[0];
                    path = split[1];
                }

                json.addProperty("item_width", d.width);
                json.addProperty("item_depth", d.depth);

                if (d.model != null) json.addProperty("model", d.model);
                if (d.modelRotten != null) json.addProperty("model_rotten", d.modelRotten);
                if (d.height != null) json.addProperty("item_height", d.height);
                if (d.scale != null) json.addProperty("item_scale", d.scale);
                if (d.flat != null) json.addProperty("lay_flat", d.flat);
                if (d.stackable != null) json.addProperty("stackable", d.stackable);
                if (d.allowsStackingOnTop != null) json.addProperty("allows_stacking_on_top", d.allowsStackingOnTop);

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

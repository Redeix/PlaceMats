package net.placemats.common.data.resource;

import java.util.HashMap;
import java.util.Map;

import net.placemats.PlaceMatMain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import net.placemats.network.PlaceMatsNetworkHandler;
import net.placemats.network.packet.SyncPlaceMatDefinitionsPacket;
import net.placemats.compat.kjs.KJSCompat;

public class DefinitionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<Item, PlaceMatDefinition> CLIENT_DEFINITIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<Item, PlaceMatDefinition> SERVER_DEFINITIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<TagKey<Item>, PlaceMatDefinition> CLIENT_TAG_DEFINITIONS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<TagKey<Item>, PlaceMatDefinition> SERVER_TAG_DEFINITIONS = new java.util.concurrent.ConcurrentHashMap<>();

    private final boolean isClientSide;
    private static DefinitionManager CLIENT_INSTANCE;
    private static DefinitionManager SERVER_INSTANCE;

    private static Map<Item, PlaceMatDefinition> getDefinitionsMapInternal() {
        if (CLIENT_INSTANCE != null && net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()) {
            return CLIENT_DEFINITIONS;
        }
        return SERVER_DEFINITIONS;
    }

    private static Map<TagKey<Item>, PlaceMatDefinition> getTagDefinitionsMapInternal() {
        if (CLIENT_INSTANCE != null && net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()) {
            return CLIENT_TAG_DEFINITIONS;
        }
        return SERVER_TAG_DEFINITIONS;
    }

    public DefinitionManager() {
        this(net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient());
    }

    public DefinitionManager(boolean isClientSide) {
        super(GSON, "place_mat_definitions");
        this.isClientSide = isClientSide;
        if (isClientSide) {
            CLIENT_INSTANCE = this;
        } else {
            SERVER_INSTANCE = this;
        }
    }

    public static PlaceMatDefinition getDefinition(Item item) {
        PlaceMatDefinition def = getDefinitionsMapInternal().get(item);
        if (def == null) {
            // Check tag definitions.
            var tagMap = getTagDefinitionsMapInternal();
            for (var entry : tagMap.entrySet()) {
                if (item.builtInRegistryHolder().is(entry.getKey())) {
                    def = entry.getValue();
                    break;
                }
            }
        }
        if (def == null) {
            def = PlaceMatDefinition.DEFAULT(item);
        }
        return def;
    }

    public static Iterable<PlaceMatDefinition> getDefinitions() {
        return getDefinitionsMapInternal().values();
    }

    public static Map<Item, PlaceMatDefinition> getDefinitionsMap() {
        return getDefinitionsMapInternal();
    }

    public static void clear() {
        getDefinitionsMapInternal().clear();
        getTagDefinitionsMapInternal().clear();
    }

    public static void addDefinition(Item item, PlaceMatDefinition definition) {
        getDefinitionsMapInternal().put(item, definition);
    }

    public static void addTagDefinition(TagKey<Item> tag, PlaceMatDefinition definition) {
        getTagDefinitionsMapInternal().put(tag, definition);
    }

    public static Map<TagKey<Item>, PlaceMatDefinition> getTagDefinitionsMap() {
        return getTagDefinitionsMapInternal();
    }

    public static void registerModels(ResourceManager resourceManager, java.util.function.Consumer<ResourceLocation> consumer) {
        postKjsEvent();
        getDefinitionsMapInternal().values().forEach(def -> {
            if (def.model() != null) {
                consumer.accept(def.model());
            }
            if (def.modelRotten() != null) {
                consumer.accept(def.modelRotten());
            }
        });
        getTagDefinitionsMapInternal().values().forEach(def -> {
            if (def.model() != null) {
                consumer.accept(def.model());
            }
            if (def.modelRotten() != null) {
                consumer.accept(def.modelRotten());
            }
        });

        // Search for definitions in all namespaces.
        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resourcesAssets = new HashMap<>();
        for (String namespace : resourceManager.getNamespaces()) {
            resourcesAssets.putAll(resourceManager.listResources("place_mat_definitions", (loc) -> loc.getNamespace().equals(namespace) && loc.getPath().endsWith(".json")));
        }

        if (!resourcesAssets.isEmpty()) {
            for (var entry : resourcesAssets.entrySet()) {
                processResource(entry.getKey(), entry.getValue(), consumer);
            }
        }
    }

    private static void processResource(ResourceLocation location, net.minecraft.server.packs.resources.Resource resource, java.util.function.Consumer<ResourceLocation> consumer) {
        try (var stream = resource.open()) {
            JsonObject json = GSON.fromJson(new java.io.InputStreamReader(stream), JsonObject.class);
            if (json.has("model")) {
                ResourceLocation loc = ResourceLocation.parse(json.get("model").getAsString());
                consumer.accept(loc);
            }
            if (json.has("model_rotten")) {
                ResourceLocation loc = ResourceLocation.parse(json.get("model_rotten").getAsString());
                consumer.accept(loc);
            }
        } catch (Exception e) {
            PlaceMatMain.LOGGER.error("Error pre-loading place mat definition models {}: {}", location, e.getMessage());
        }
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> object, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<Item, PlaceMatDefinition> definitions = isClientSide ? CLIENT_DEFINITIONS : SERVER_DEFINITIONS;
        Map<TagKey<Item>, PlaceMatDefinition> tagDefinitions = isClientSide ? CLIENT_TAG_DEFINITIONS : SERVER_TAG_DEFINITIONS;

        definitions.clear();
        tagDefinitions.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                float itemWidth = json.get("item_width").getAsFloat();
                float itemDepth = json.get("item_depth").getAsFloat();
                ResourceLocation model = json.has("model") ? ResourceLocation.parse(json.get("model").getAsString()) : null;
                ResourceLocation modelRotten = json.has("model_rotten") ? ResourceLocation.parse(json.get("model_rotten").getAsString()) : null;
                Float itemHeight = json.has("item_height") ? json.get("item_height").getAsFloat() : null;

                PlaceMatDefinition def = createDefinition(json, itemWidth, itemDepth, model, modelRotten, itemHeight);

                if (json.has("item")) {
                    ResourceLocation itemLoc = ResourceLocation.parse(json.get("item").getAsString());
                    Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
                    if (item != null) {
                        definitions.put(item, def);
                    }
                } else if (json.has("tag")) {
                    ResourceLocation tagLoc = ResourceLocation.parse(json.get("tag").getAsString());
                    TagKey<Item> tagKey = ForgeRegistries.ITEMS.tags().createTagKey(tagLoc);
                    tagDefinitions.put(tagKey, def);
                }
            } catch (Exception e) {
                PlaceMatMain.LOGGER.error("Error loading place mat definition {}: {}", entry.getKey(), e.getMessage());
            }
        }

        postKjsEvent(isClientSide);
    }

    public static void postKjsEvent() {
        postKjsEvent(net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient());
    }

    public static void postKjsEvent(boolean isClient) {
        Map<Item, PlaceMatDefinition> definitions = isClient ? CLIENT_DEFINITIONS : SERVER_DEFINITIONS;
        Map<TagKey<Item>, PlaceMatDefinition> tagDefinitions = isClient ? CLIENT_TAG_DEFINITIONS : SERVER_TAG_DEFINITIONS;

        KJSCompat.INSTANCE.postEvent(definitions, tagDefinitions);

        // Sync to all clients if we are on the server side.
        if (!isClient && net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            PlaceMatsNetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPlaceMatDefinitionsPacket(definitions, tagDefinitions));
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        PlaceMatsNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlaceMatDefinitionsPacket(getDefinitionsMapInternal(), getTagDefinitionsMapInternal()));
    }

    private static PlaceMatDefinition createDefinition(JsonObject json, float width, float depth, ResourceLocation model, ResourceLocation modelRotten, Float itemHeight) {
        boolean flat = !json.has("lay_flat") || json.get("lay_flat").getAsBoolean();
        float scale = json.has("item_scale") ? json.get("item_scale").getAsFloat() : (flat ? 0.375f : 0.5f);
        boolean stackable = !json.has("stackable") || json.get("stackable").getAsBoolean();
        boolean allowsStackingOnTop = !json.has("allows_stacking_on_top") || json.get("allows_stacking_on_top").getAsBoolean();
        return new PlaceMatDefinition(new Vec2(width * (flat ? 0.75f : 1.0f), depth * (flat ? 0.75f : 1.0f)), model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop);
    }

    public record PlaceMatDefinition(Vec2 size, ResourceLocation model, ResourceLocation modelRotten, float scale, boolean flat, @Nullable Float itemHeight, boolean stackable,
            boolean allowsStackingOnTop) {
        public static PlaceMatDefinition DEFAULT(Item item) {
            boolean isBlock = item instanceof net.minecraft.world.item.BlockItem;
            // Generic render food as items.
            boolean isFood = item.isEdible();
            boolean flat = !isBlock || isFood;
            float scale = flat ? 0.375f : 0.5f;
            float size = flat ? 0.1875f : 0.25f;
            return new PlaceMatDefinition(new Vec2(size, size), null, null, scale, flat, null, true, true);
        }

        public float getItemHeight() {
            if (itemHeight != null) {
                return itemHeight;
            }
            if (flat) {
                return 1 / 16f;
            }
            return Math.max(size.x, size.y);
        }
    }
}

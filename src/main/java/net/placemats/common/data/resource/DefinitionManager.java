package net.placemats.common.data.resource;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlacementRange;
import net.placemats.common.datagen.definitions.Condition;
import net.placemats.common.datagen.definitions.ConditionContext;
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
import org.joml.Vector3f;
import com.google.gson.JsonArray;

import net.placemats.network.PlaceMatsNetworkHandler;
import net.placemats.network.packet.SyncPlaceMatDefinitionsPacket;
import net.placemats.compat.kjs.KJSCompat;

@SuppressWarnings({"unused", "deprecation", "FieldCanBeLocal"})
public class DefinitionManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<Item, PlaceMatDefinition> CLIENT_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<Item, PlaceMatDefinition> SERVER_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<TagKey<Item>, PlaceMatDefinition> CLIENT_TAG_DEFINITIONS = new ConcurrentHashMap<>();
    private static final Map<TagKey<Item>, PlaceMatDefinition> SERVER_TAG_DEFINITIONS = new ConcurrentHashMap<>();

    private final boolean isClientSide;
    private static DefinitionManager CLIENT_INSTANCE;
    private static DefinitionManager SERVER_INSTANCE;

    private static Map<Item, PlaceMatDefinition> getDefinitionsMapInternal() {
        if (CLIENT_INSTANCE != null && EffectiveSide.get().isClient()) {
            return CLIENT_DEFINITIONS;
        }
        return SERVER_DEFINITIONS;
    }

    private static Map<TagKey<Item>, PlaceMatDefinition> getTagDefinitionsMapInternal() {
        if (CLIENT_INSTANCE != null && EffectiveSide.get().isClient()) {
            return CLIENT_TAG_DEFINITIONS;
        }
        return SERVER_TAG_DEFINITIONS;
    }

    public DefinitionManager() {
        this(EffectiveSide.get().isClient());
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

    public static PlaceMatDefinition getEffectiveDefinition(ItemStack stack, @Nullable BlockState state, @Nullable PlacementRange range) {
        PlaceMatDefinition def = getDefinition(stack.getItem());
        ConditionContext context = ConditionContext.of(stack, state, range);
        return def.resolve(context);
    }

    public static PlaceMatDefinition getEffectiveDefinition(ItemStack stack, @Nullable BlockState state, @Nullable PlacementRange range, int rangeIndex) {
        PlaceMatDefinition def = getDefinition(stack.getItem());
        ConditionContext context = ConditionContext.of(stack, state, range, rangeIndex);
        return def.resolve(context);
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

    public static void registerModels(ResourceManager resourceManager, Consumer<ResourceLocation> consumer) {
        postKjsEvent();
        getDefinitionsMapInternal().values().forEach(def -> collectDefinitionModels(def, consumer));
        getTagDefinitionsMapInternal().values().forEach(def -> collectDefinitionModels(def, consumer));

        // Search for definitions in all namespaces.
        Map<ResourceLocation, Resource> resourcesAssets = new HashMap<>();
        for (String namespace : resourceManager.getNamespaces()) {
            resourcesAssets.putAll(resourceManager.listResources("place_mat_definitions", (loc) -> loc.getNamespace().equals(namespace) && loc.getPath().endsWith(".json")));
        }

        if (!resourcesAssets.isEmpty()) {
            for (var entry : resourcesAssets.entrySet()) {
                processResource(entry.getKey(), entry.getValue(), consumer);
            }
        }
    }

    private static void collectDefinitionModels(PlaceMatDefinition def, Consumer<ResourceLocation> consumer) {
        if (def.model() != null) {
            consumer.accept(def.model());
        }
        if (def.modelRotten() != null) {
            consumer.accept(def.modelRotten());
        }
        for (PlaceMatConditionDefinition condDef : def.conditions()) {
            collectConditionModels(condDef, consumer);
        }
    }

    private static void collectConditionModels(PlaceMatConditionDefinition condDef, Consumer<ResourceLocation> consumer) {
        if (condDef.model() != null) {
            consumer.accept(condDef.model());
        }
        for (PlaceMatConditionDefinition child : condDef.conditions()) {
            collectConditionModels(child, consumer);
        }
    }

    private static void processResource(ResourceLocation location, Resource resource, Consumer<ResourceLocation> consumer) {
        try (var stream = resource.open()) {
            JsonObject json = GSON.fromJson(new InputStreamReader(stream), JsonObject.class);
            collectJsonModels(json, consumer);
        } catch (Exception e) {
            PlaceMatMain.LOGGER.error("Error pre-loading place mat definition models {}: {}", location, e.getMessage());
        }
    }

    private static void collectJsonModels(JsonObject json, Consumer<ResourceLocation> consumer) {
        if (json.has("model")) {
            ResourceLocation loc = ResourceLocation.parse(json.get("model").getAsString());
            consumer.accept(loc);
        }
        if (json.has("model_rotten")) {
            ResourceLocation loc = ResourceLocation.parse(json.get("model_rotten").getAsString());
            consumer.accept(loc);
        }
        if (json.has("conditions") && json.get("conditions").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("conditions")) {
                if (el.isJsonObject()) {
                    collectJsonModels(el.getAsJsonObject(), consumer);
                }
            }
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
                Float width = null;
                Float depth = null;
                Float height = null;
                if (json.has("size") && json.get("size").isJsonArray()) {
                    JsonArray arr = json.getAsJsonArray("size");
                    if (!arr.isEmpty()) width = arr.get(0).getAsFloat();
                    if (arr.size() >= 2) depth = arr.get(1).getAsFloat();
                    if (arr.size() >= 3) height = arr.get(2).getAsFloat();
                }
                if (width == null && json.has("item_width")) width = json.get("item_width").getAsFloat();
                if (depth == null && json.has("item_depth")) depth = json.get("item_depth").getAsFloat();
                if (height == null && json.has("item_height")) height = json.get("item_height").getAsFloat();
                if (height == null && json.has("height")) height = json.get("height").getAsFloat();

                float itemWidth = width != null ? width : 0.25f;
                float itemDepth = depth != null ? depth : 0.25f;
                ResourceLocation model = json.has("model") ? ResourceLocation.parse(json.get("model").getAsString()) : null;
                ResourceLocation modelRotten = json.has("model_rotten") ? ResourceLocation.parse(json.get("model_rotten").getAsString()) : null;

                PlaceMatDefinition def = createDefinition(json, itemWidth, itemDepth, model, modelRotten, height);

                if (json.has("item")) {
                    ResourceLocation itemLoc = ResourceLocation.parse(json.get("item").getAsString());
                    Item item = ForgeRegistries.ITEMS.getValue(itemLoc);
                    if (item != null) {
                        definitions.put(item, def);
                    }
                } else if (json.has("tag")) {
                    ResourceLocation tagLoc = ResourceLocation.parse(json.get("tag").getAsString());
                    TagKey<Item> tagKey = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).createTagKey(tagLoc);
                    tagDefinitions.put(tagKey, def);
                }
            } catch (Exception e) {
                PlaceMatMain.LOGGER.error("Error loading place mat definition {}: {}", entry.getKey(), e.getMessage());
            }
        }

        postKjsEvent(isClientSide);
    }

    public static void postKjsEvent() {
        postKjsEvent(EffectiveSide.get().isClient());
    }

    public static void postKjsEvent(boolean isClient) {
        Map<Item, PlaceMatDefinition> definitions = isClient ? CLIENT_DEFINITIONS : SERVER_DEFINITIONS;
        Map<TagKey<Item>, PlaceMatDefinition> tagDefinitions = isClient ? CLIENT_TAG_DEFINITIONS : SERVER_TAG_DEFINITIONS;

        KJSCompat.INSTANCE.postEvent(definitions, tagDefinitions);

        // Sync to all clients if we are on the server side.
        if (!isClient && ServerLifecycleHooks.getCurrentServer() != null) {
            PlaceMatsNetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncPlaceMatDefinitionsPacket(definitions, tagDefinitions));
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        PlaceMatsNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlaceMatDefinitionsPacket(getDefinitionsMapInternal(), getTagDefinitionsMapInternal()));
    }

    private static PlaceMatDefinition createDefinition(JsonObject json, float width, float depth, ResourceLocation model, ResourceLocation modelRotten, Float itemHeight) {
        boolean flat = !json.has("lay_flat") || json.get("lay_flat").getAsBoolean();
        float scale;
        if (json.has("scale")) {
            scale = json.get("scale").getAsFloat();
        } else if (json.has("item_scale")) {
            scale = json.get("item_scale").getAsFloat();
        } else {
            scale = flat ? 0.375f : 0.5f;
        }
        boolean stackable = !json.has("stackable") || json.get("stackable").getAsBoolean();
        boolean allowsStackingOnTop = !json.has("allows_stacking_on_top") || json.get("allows_stacking_on_top").getAsBoolean();
        boolean ignoreItemTints = parseIgnoreItemTints(json);
        String fluidTextureIndex = parseFluidTextureIndex(json);
        Map<Integer, Integer> tintColors = parseTintColors(json);
        Vector3f pos = parseVector3f(json, "pos", "offset", "position");
        Vector3f rot = parseVector3f(json, "rot", "rotation");
        List<PlaceMatConditionDefinition> conditions = parseConditions(json);
        return new PlaceMatDefinition(new Vec2(width * (flat ? 0.75f : 1.0f), depth * (flat ? 0.75f : 1.0f)), model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop, ignoreItemTints, fluidTextureIndex, tintColors, pos, rot, conditions);
    }

    public static Vector3f parseVector3f(JsonObject json, String primaryKey, String... alternateKeys) {
        JsonElement elem = json.get(primaryKey);
        if (elem == null) {
            for (String alt : alternateKeys) {
                if (json.has(alt)) {
                    elem = json.get(alt);
                    break;
                }
            }
        }
        if (elem != null) {
            if (elem.isJsonArray() && elem.getAsJsonArray().size() >= 3) {
                JsonArray arr = elem.getAsJsonArray();
                return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
            } else if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                float x = obj.has("x") ? obj.get("x").getAsFloat() : (obj.has("roll") ? obj.get("roll").getAsFloat() : 0f);
                float y = obj.has("y") ? obj.get("y").getAsFloat() : (obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0f);
                float z = obj.has("z") ? obj.get("z").getAsFloat() : (obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0f);
                return new Vector3f(x, y, z);
            }
        }
        return null;
    }

    public static boolean parseIgnoreItemTints(JsonObject json) {
        if (json.has("ignore_item_tints")) {
            return json.get("ignore_item_tints").getAsBoolean();
        } else if (json.has("override_tints")) {
            return json.get("override_tints").getAsBoolean();
        } else if (json.has("ignore_tints")) {
            return json.get("ignore_tints").getAsBoolean();
        }
        return false;
    }

    public static String parseFluidTextureIndex(JsonObject json) {
        if (json.has("fluid_texture_index")) {
            return json.get("fluid_texture_index").getAsString();
        } else if (json.has("fluid_texture")) {
            return json.get("fluid_texture").getAsString();
        } else if (json.has("fluid_texture_key")) {
            return json.get("fluid_texture_key").getAsString();
        }
        return null;
    }

    public static Map<Integer, Integer> parseTintColors(JsonObject json) {
        Map<Integer, Integer> map = new HashMap<>();
        JsonObject obj = null;
        if (json.has("tint_colors") && json.get("tint_colors").isJsonObject()) {
            obj = json.getAsJsonObject("tint_colors");
        } else if (json.has("colors") && json.get("colors").isJsonObject()) {
            obj = json.getAsJsonObject("colors");
        } else if (json.has("tints") && json.get("tints").isJsonObject()) {
            obj = json.getAsJsonObject("tints");
        }
        if (obj != null) {
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                try {
                    int idx = Integer.parseInt(entry.getKey());
                    int color = getColor(entry);
                    map.put(idx, color);
                } catch (Exception ignored) {}
            }
        }
        return map.isEmpty() ? Map.of() : Collections.unmodifiableMap(map);
    }

    public static int getColor(Map.Entry<String, JsonElement> entry)
    {
        JsonElement val = entry.getValue();
        int color;
        if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
            String s = val.getAsString();
            if (s.startsWith("#")) s = s.substring(1);
            long num = Long.parseLong(s, 16);
            if (s.length() <= 6) num |= 0xFF000000L;
            color = (int) num;
        } else {
            color = val.getAsInt();
        }
        return color;
    }

    private static List<PlaceMatConditionDefinition> parseConditions(JsonObject json) {
        if (!json.has("conditions") || !json.get("conditions").isJsonArray()) {
            return List.of();
        }
        List<PlaceMatConditionDefinition> list = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("conditions")) {
            if (el.isJsonObject()) {
                JsonObject condObj = el.getAsJsonObject();
                Condition condition = Condition.fromJson(condObj);
                Vec2 size = null;
                Float itemHeight = null;
                if (condObj.has("size") && condObj.get("size").isJsonArray()) {
                    JsonArray arr = condObj.getAsJsonArray("size");
                    float w = !arr.isEmpty() ? arr.get(0).getAsFloat() : 0.25f;
                    float d = arr.size() >= 2 ? arr.get(1).getAsFloat() : 0.25f;
                    if (arr.size() >= 3) {
                        itemHeight = arr.get(2).getAsFloat();
                    }
                    boolean flat = !condObj.has("lay_flat") || condObj.get("lay_flat").getAsBoolean();
                    size = new Vec2(w * (flat ? 0.75f : 1.0f), d * (flat ? 0.75f : 1.0f));
                } else if (condObj.has("item_width") && condObj.has("item_depth")) {
                    float w = condObj.get("item_width").getAsFloat();
                    float d = condObj.get("item_depth").getAsFloat();
                    boolean flat = !condObj.has("lay_flat") || condObj.get("lay_flat").getAsBoolean();
                    size = new Vec2(w * (flat ? 0.75f : 1.0f), d * (flat ? 0.75f : 1.0f));
                }
                ResourceLocation model = condObj.has("model") ? ResourceLocation.parse(condObj.get("model").getAsString()) : null;
                Float scale = null;
                if (condObj.has("scale")) {
                    scale = condObj.get("scale").getAsFloat();
                } else if (condObj.has("item_scale")) {
                    scale = condObj.get("item_scale").getAsFloat();
                }
                Boolean flat = condObj.has("lay_flat") ? condObj.get("lay_flat").getAsBoolean() : null;
                if (itemHeight == null) {
                    if (condObj.has("item_height")) {
                        itemHeight = condObj.get("item_height").getAsFloat();
                    } else if (condObj.has("height")) {
                        itemHeight = condObj.get("height").getAsFloat();
                    }
                }
                Boolean stackable = condObj.has("stackable") ? condObj.get("stackable").getAsBoolean() : null;
                Boolean allowsStackingOnTop = condObj.has("allows_stacking_on_top") ? condObj.get("allows_stacking_on_top").getAsBoolean() : null;
                Boolean ignoreItemTints = condObj.has("ignore_item_tints") ? Boolean.valueOf(condObj.get("ignore_item_tints").getAsBoolean()) : (condObj.has("override_tints") ? condObj.get("override_tints").getAsBoolean() : null);
                String fluidTextureIndex = parseFluidTextureIndex(condObj);
                Map<Integer, Integer> tintColors = parseTintColors(condObj);
                Vector3f pos = parseVector3f(condObj, "pos", "offset", "position");
                Vector3f rot = parseVector3f(condObj, "rot", "rotation");
                List<PlaceMatConditionDefinition> nested = parseConditions(condObj);
                list.add(new PlaceMatConditionDefinition(condition, size, model, scale, flat, itemHeight, stackable, allowsStackingOnTop, ignoreItemTints, fluidTextureIndex, tintColors.isEmpty() ? null : tintColors, pos, rot, nested));
            }
        }
        return list;
    }

    public record PlaceMatConditionDefinition(
            Condition condition,
            @Nullable Vec2 size,
            @Nullable ResourceLocation model,
            @Nullable Float scale,
            @Nullable Boolean flat,
            @Nullable Float itemHeight,
            @Nullable Boolean stackable,
            @Nullable Boolean allowsStackingOnTop,
            @Nullable Boolean ignoreItemTints,
            @Nullable String fluidTextureIndex,
            @Nullable Map<Integer, Integer> tintColors,
            @Nullable Vector3f pos,
            @Nullable Vector3f rot,
            List<PlaceMatConditionDefinition> conditions) {

        public PlaceMatConditionDefinition(
                Condition condition,
                @Nullable Vec2 size,
                @Nullable ResourceLocation model,
                @Nullable Float scale,
                @Nullable Boolean flat,
                @Nullable Float itemHeight,
                @Nullable Boolean stackable,
                @Nullable Boolean allowsStackingOnTop) {
            this(condition, size, model, scale, flat, itemHeight, stackable, allowsStackingOnTop, null, null, null, null, null, List.of());
        }

        public PlaceMatConditionDefinition(
                Condition condition,
                @Nullable Vec2 size,
                @Nullable ResourceLocation model,
                @Nullable Float scale,
                @Nullable Boolean flat,
                @Nullable Float itemHeight,
                @Nullable Boolean stackable,
                @Nullable Boolean allowsStackingOnTop,
                List<PlaceMatConditionDefinition> conditions) {
            this(condition, size, model, scale, flat, itemHeight, stackable, allowsStackingOnTop, null, null, null, null, null, conditions);
        }

        public PlaceMatConditionDefinition(
                Condition condition,
                @Nullable Vec2 size,
                @Nullable ResourceLocation model,
                @Nullable Float scale,
                @Nullable Boolean flat,
                @Nullable Float itemHeight,
                @Nullable Boolean stackable,
                @Nullable Boolean allowsStackingOnTop,
                @Nullable String fluidTextureIndex,
                @Nullable Map<Integer, Integer> tintColors,
                List<PlaceMatConditionDefinition> conditions) {
            this(condition, size, model, scale, flat, itemHeight, stackable, allowsStackingOnTop, null, fluidTextureIndex, tintColors, null, null, conditions);
        }
    }

    public record PlaceMatDefinition(
            Vec2 size,
            ResourceLocation model,
            ResourceLocation modelRotten,
            float scale,
            boolean flat,
            @Nullable Float itemHeight,
            boolean stackable,
            boolean allowsStackingOnTop,
            boolean ignoreItemTints,
            @Nullable String fluidTextureIndex,
            Map<Integer, Integer> tintColors,
            @Nullable Vector3f pos,
            @Nullable Vector3f rot,
            List<PlaceMatConditionDefinition> conditions) {

        public PlaceMatDefinition(Vec2 size, ResourceLocation model, ResourceLocation modelRotten, float scale, boolean flat, @Nullable Float itemHeight, boolean stackable, boolean allowsStackingOnTop) {
            this(size, model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop, false, null, Map.of(), null, null, List.of());
        }

        public PlaceMatDefinition(Vec2 size, ResourceLocation model, ResourceLocation modelRotten, float scale, boolean flat, @Nullable Float itemHeight, boolean stackable, boolean allowsStackingOnTop, List<PlaceMatConditionDefinition> conditions) {
            this(size, model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop, false, null, Map.of(), null, null, conditions);
        }

        public PlaceMatDefinition(Vec2 size, ResourceLocation model, ResourceLocation modelRotten, float scale, boolean flat, @Nullable Float itemHeight, boolean stackable, boolean allowsStackingOnTop, @Nullable String fluidTextureIndex, Map<Integer, Integer> tintColors, List<PlaceMatConditionDefinition> conditions) {
            this(size, model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop, false, fluidTextureIndex, tintColors, null, null, conditions);
        }

        public PlaceMatDefinition(Vec2 size, ResourceLocation model, ResourceLocation modelRotten, float scale, boolean flat, @Nullable Float itemHeight, boolean stackable, boolean allowsStackingOnTop, @Nullable String fluidTextureIndex, Map<Integer, Integer> tintColors, @Nullable Vector3f pos, @Nullable Vector3f rot, List<PlaceMatConditionDefinition> conditions) {
            this(size, model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop, false, fluidTextureIndex, tintColors, pos, rot, conditions);
        }

        public static PlaceMatDefinition DEFAULT(Item item) {
            boolean isBlock = item instanceof BlockItem;
            // Generic render food as items.
            boolean isFood = item.isEdible();
            boolean flat = !isBlock || isFood;
            float scale = flat ? 0.375f : 0.5f;
            float size = flat ? 0.1875f : 0.25f;
            return new PlaceMatDefinition(new Vec2(size, size), null, null, scale, flat, null, true, true, false, null, Map.of(), null, null, List.of());
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

        public PlaceMatDefinition resolve(ConditionContext context) {
            if (conditions.isEmpty()) {
                return this;
            }

            record MatchedOverride(int depth, int priority, int order, PlaceMatConditionDefinition def) {}

            List<MatchedOverride> matched = new ArrayList<>();
            int[] orderCounter = new int[1];

            class Collector {
                void collect(List<PlaceMatConditionDefinition> condList, int depth) {
                    for (PlaceMatConditionDefinition condDef : condList) {
                        if (condDef.condition().test(context)) {
                            matched.add(new MatchedOverride(depth, condDef.condition().getType().getPriority(), orderCounter[0]++, condDef));
                            if (!condDef.conditions().isEmpty()) {
                                collect(condDef.conditions(), depth + 1);
                            }
                        }
                    }
                }
            }

            new Collector().collect(conditions, 0);

            if (matched.isEmpty()) {
                return this;
            }

            // Quality comparator: higher depth wins; if equal depth, higher hierarchy priority (lower priority index) wins; if equal, later order wins.
            Comparator<MatchedOverride> comparator = (a, b) -> {
                if (a.depth != b.depth) return Integer.compare(a.depth, b.depth);
                if (a.priority != b.priority) return Integer.compare(b.priority, a.priority);
                return Integer.compare(a.order, b.order);
            };

            Vec2 effectiveSize = this.size;
            ResourceLocation effectiveModel = this.model;
            float effectiveScale = this.scale;
            boolean effectiveFlat = this.flat;
            Float effectiveItemHeight = this.itemHeight;
            boolean effectiveStackable = this.stackable;
            boolean effectiveAllowsStackingOnTop = this.allowsStackingOnTop;
            boolean effectiveIgnoreItemTints = this.ignoreItemTints;
            String effectiveFluidTextureIndex = this.fluidTextureIndex;
            Map<Integer, Integer> effectiveTintColors = new HashMap<>(this.tintColors != null ? this.tintColors : Map.of());
            Vector3f effectivePos = this.pos;
            Vector3f effectiveRot = this.rot;

            MatchedOverride bestSize = matched.stream().filter(m -> m.def.size() != null).max(comparator).orElse(null);
            if (bestSize != null) effectiveSize = bestSize.def.size();

            MatchedOverride bestModel = matched.stream().filter(m -> m.def.model() != null).max(comparator).orElse(null);
            if (bestModel != null) effectiveModel = bestModel.def.model();

            MatchedOverride bestScale = matched.stream().filter(m -> m.def.scale() != null).max(comparator).orElse(null);
            if (bestScale != null && bestScale.def.scale() != null) effectiveScale = bestScale.def.scale();

            MatchedOverride bestFlat = matched.stream().filter(m -> m.def.flat() != null).max(comparator).orElse(null);
            if (bestFlat != null && bestFlat.def.flat() != null) effectiveFlat = bestFlat.def.flat();

            MatchedOverride bestHeight = matched.stream().filter(m -> m.def.itemHeight() != null).max(comparator).orElse(null);
            if (bestHeight != null) effectiveItemHeight = bestHeight.def.itemHeight();

            MatchedOverride bestStackable = matched.stream().filter(m -> m.def.stackable() != null).max(comparator).orElse(null);
            if (bestStackable != null && bestStackable.def.stackable() != null) effectiveStackable = bestStackable.def.stackable();

            MatchedOverride bestAllowsStacking = matched.stream().filter(m -> m.def.allowsStackingOnTop() != null).max(comparator).orElse(null);
            if (bestAllowsStacking != null && bestAllowsStacking.def.allowsStackingOnTop() != null) effectiveAllowsStackingOnTop = bestAllowsStacking.def.allowsStackingOnTop();

            MatchedOverride bestIgnoreTints = matched.stream().filter(m -> m.def.ignoreItemTints() != null).max(comparator).orElse(null);
            if (bestIgnoreTints != null && bestIgnoreTints.def.ignoreItemTints() != null) effectiveIgnoreItemTints = bestIgnoreTints.def.ignoreItemTints();

            MatchedOverride bestFluidTex = matched.stream().filter(m -> m.def.fluidTextureIndex() != null).max(comparator).orElse(null);
            if (bestFluidTex != null) effectiveFluidTextureIndex = bestFluidTex.def.fluidTextureIndex();

            matched.stream().filter(m -> m.def.tintColors() != null && !m.def.tintColors().isEmpty())
                    .sorted(comparator)
                    .forEach(m -> effectiveTintColors.putAll(m.def.tintColors()));

            MatchedOverride bestPos = matched.stream().filter(m -> m.def.pos() != null).max(comparator).orElse(null);
            if (bestPos != null) effectivePos = bestPos.def.pos();

            MatchedOverride bestRot = matched.stream().filter(m -> m.def.rot() != null).max(comparator).orElse(null);
            if (bestRot != null) effectiveRot = bestRot.def.rot();

            return new PlaceMatDefinition(effectiveSize, effectiveModel, this.modelRotten, effectiveScale, effectiveFlat, effectiveItemHeight, effectiveStackable, effectiveAllowsStackingOnTop, effectiveIgnoreItemTints, effectiveFluidTextureIndex, effectiveTintColors.isEmpty() ? Map.of() : Collections.unmodifiableMap(effectiveTintColors), effectivePos, effectiveRot, List.of());
        }
    }
}

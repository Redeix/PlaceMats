package net.placemats.common.datagen.definitions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public interface Condition {
    ConditionType getType();

    boolean test(ConditionContext context);

    JsonObject toJson();

    void encode(FriendlyByteBuf buffer);

    default Condition not() {
        return new NotCondition(this);
    }

    static StackSizeCondition stackSize(int count) {
        return new StackSizeCondition(count);
    }

    static StackSizeCondition stackSize(float percentage) {
        return new StackSizeCondition(percentage);
    }

    static StackSizeCondition isStackSize(int count) {
        return stackSize(count);
    }

    static StackSizeCondition isStackSize(float percentage) {
        return stackSize(percentage);
    }

    static IsFluidContainerCondition fluidContainer() {
        return IsFluidContainerCondition.ANY;
    }

    static IsFluidContainerCondition fluidContainer(boolean requireNotEmpty) {
        return requireNotEmpty ? IsFluidContainerCondition.NOT_EMPTY : IsFluidContainerCondition.ANY;
    }

    static IsFluidContainerCondition isFluidContainer() {
        return fluidContainer();
    }

    static IsFluidContainerCondition isFluidContainer(boolean requireNotEmpty) {
        return fluidContainer(requireNotEmpty);
    }

    static HasFluidCondition hasFluid() {
        return HasFluidCondition.INSTANCE;
    }

    static HasFluidCondition containsFluid() {
        return HasFluidCondition.INSTANCE;
    }

    static Condition notHasFluid() {
        return new NotCondition(HasFluidCondition.INSTANCE);
    }

    static Condition notContainsFluid() {
        return new NotCondition(HasFluidCondition.INSTANCE);
    }

    static IsPlacematBlockCondition isPlacematBlock(ResourceLocation block) {
        return new IsPlacematBlockCondition(block, null);
    }

    static IsPlacematBlockCondition isPlacematBlock(TagKey<Block> tag) {
        return new IsPlacematBlockCondition(null, tag);
    }

    static IsPlacematBlockCondition isPlacematBlock(String blockOrTag) {
        if (blockOrTag.startsWith("#")) {
            return new IsPlacematBlockCondition(null, TagKey.create(Registries.BLOCK, ResourceLocation.parse(blockOrTag.substring(1))));
        } else {
            return new IsPlacematBlockCondition(ResourceLocation.parse(blockOrTag), null);
        }
    }

    static IsRottenCondition isRotten() {
        return IsRottenCondition.INSTANCE;
    }

    static IsPlacementRangeCondition isPlacementRange(int rangeIndex) {
        return new IsPlacementRangeCondition(rangeIndex);
    }

    static HasTraitCondition hasTrait(ResourceLocation trait) {
        return new HasTraitCondition(trait);
    }

    static HasTraitCondition hasTrait(String trait) {
        return new HasTraitCondition(ResourceLocation.parse(trait));
    }

    static IsItemCondition isItem(ResourceLocation item) {
        return new IsItemCondition(item, null);
    }

    static IsItemCondition isItem(TagKey<Item> tag) {
        return new IsItemCondition(null, tag);
    }

    static IsItemCondition isItem(String itemOrTag) {
        if (itemOrTag.startsWith("#")) {
            return new IsItemCondition(null, TagKey.create(Registries.ITEM, ResourceLocation.parse(itemOrTag.substring(1))));
        } else {
            return new IsItemCondition(ResourceLocation.parse(itemOrTag), null);
        }
    }

    static NotCondition not(Condition condition) {
        return new NotCondition(condition);
    }

    static Condition fromJson(JsonObject json) {
        String condStr = json.has("condition") ? json.get("condition").getAsString() : (json.has("type") ? json.get("type").getAsString() : "");
        ConditionType type = ConditionType.fromId(condStr);
        if (type == null) {
            throw new IllegalArgumentException("Unknown condition type: " + condStr);
        }
        return switch (type) {
            case IS_PLACEMENT_RANGE -> {
                int range = json.has("range") ? json.get("range").getAsInt() : json.get("range_index").getAsInt();
                yield new IsPlacementRangeCondition(range);
            }
            case IS_STACK_SIZE -> {
                if (json.has("percentage")) {
                    yield stackSize(json.get("percentage").getAsFloat());
                } else if (json.has("percent")) {
                    yield stackSize(json.get("percent").getAsFloat());
                } else if (json.has("min_percentage")) {
                    yield stackSize(json.get("min_percentage").getAsFloat());
                } else if (json.has("count")) {
                    yield stackSize(json.get("count").getAsInt());
                } else if (json.has("min_count")) {
                    yield stackSize(json.get("min_count").getAsInt());
                } else if (json.has("amount")) {
                    yield stackSize(json.get("amount").getAsInt());
                } else if (json.has("value")) {
                    JsonElement val = json.get("value");
                    if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) {
                        float f = val.getAsFloat();
                        if (f <= 1.0f && f > 0.0f && !val.getAsString().matches("^\\d+$")) {
                            yield stackSize(f);
                        } else {
                            yield stackSize(val.getAsInt());
                        }
                    } else {
                        yield stackSize(val.getAsInt());
                    }
                } else {
                    yield stackSize(1);
                }
            }
            case HAS_FLUID -> HasFluidCondition.INSTANCE;
            case IS_FLUID_CONTAINER -> {
                boolean notEmpty = json.has("require_not_empty") && json.get("require_not_empty").getAsBoolean();
                yield isFluidContainer(notEmpty);
            }
            case HAS_TRAIT -> {
                ResourceLocation trait = ResourceLocation.parse(json.get("trait").getAsString());
                yield new HasTraitCondition(trait);
            }
            case IS_ROTTEN -> IsRottenCondition.INSTANCE;
            case IS_PLACEMAT_BLOCK -> {
                if (json.has("tag")) {
                    yield isPlacematBlock(TagKey.create(Registries.BLOCK, ResourceLocation.parse(json.get("tag").getAsString())));
                } else if (json.has("block")) {
                    yield isPlacematBlock(json.get("block").getAsString());
                } else {
                    throw new IllegalArgumentException("is_placemat_block condition must specify 'block' or 'tag'");
                }
            }
            case IS_ITEM -> {
                if (json.has("tag")) {
                    yield isItem(TagKey.create(Registries.ITEM, ResourceLocation.parse(json.get("tag").getAsString())));
                } else if (json.has("item")) {
                    yield isItem(json.get("item").getAsString());
                } else {
                    throw new IllegalArgumentException("is_item condition must specify 'item' or 'tag'");
                }
            }
            case NOT -> {
                JsonObject valJson = json.has("value") ? json.getAsJsonObject("value") : (json.has("condition_value") ? json.getAsJsonObject("condition_value") : json);
                yield new NotCondition(fromJson(valJson));
            }
        };
    }

    static Condition decode(FriendlyByteBuf buffer) {
        ConditionType type = buffer.readEnum(ConditionType.class);
        return switch (type) {
            case IS_PLACEMENT_RANGE -> new IsPlacementRangeCondition(buffer.readInt());
            case IS_STACK_SIZE -> buffer.readBoolean() ? stackSize(buffer.readFloat()) : stackSize(buffer.readInt());
            case HAS_FLUID -> HasFluidCondition.INSTANCE;
            case IS_FLUID_CONTAINER -> isFluidContainer(buffer.readBoolean());
            case HAS_TRAIT -> new HasTraitCondition(buffer.readResourceLocation());
            case IS_ROTTEN -> IsRottenCondition.INSTANCE;
            case IS_PLACEMAT_BLOCK -> {
                boolean isTag = buffer.readBoolean();
                ResourceLocation loc = buffer.readResourceLocation();
                yield isTag ? isPlacematBlock(TagKey.create(Registries.BLOCK, loc)) : isPlacematBlock(loc);
            }
            case IS_ITEM -> {
                boolean isTag = buffer.readBoolean();
                ResourceLocation loc = buffer.readResourceLocation();
                yield isTag ? isItem(TagKey.create(Registries.ITEM, loc)) : isItem(loc);
            }
            case NOT -> new NotCondition(decode(buffer));
        };
    }
}

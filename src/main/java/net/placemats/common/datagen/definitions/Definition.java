package net.placemats.common.datagen.definitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public class Definition {
    public final String id;
    public Float width = 0.25f;
    public Float depth = 0.25f;
    public String model;
    public String modelRotten;
    public ModelData modelData;
    public ModelData modelRottenData;
    public Float height;
    public Float scale;
    public Boolean flat;
    public Boolean stackable;
    public Boolean allowsStackingOnTop;
    private final List<ConditionDefinition> conditions = new ArrayList<>();

    public Definition() {
        this.id = null;
        this.width = null;
        this.depth = null;
    }

    public Definition(String id) {
        this.id = id;
    }

    public Definition size(float width, float depth) {
        this.width = width;
        this.depth = depth;
        return this;
    }

    public Definition model(String model) {
        this.model = model;
        return this;
    }

    public Definition model(ModelData modelData) {
        this.modelData = modelData;
        if (modelData.path() != null) {
            this.model = modelData.path();
        }
        return this;
    }

    @Deprecated
    public Definition modelRotten(String modelRotten) {
        this.modelRotten = modelRotten;
        return condition(Condition.isRotten(), d -> d.model(modelRotten));
    }

    @Deprecated
    public Definition modelRotten(ModelData modelRottenData) {
        this.modelRottenData = modelRottenData;
        if (modelRottenData.path() != null) {
            this.modelRotten = modelRottenData.path();
        }
        return condition(Condition.isRotten(), d -> d.model(modelRottenData));
    }

    public static ModelData parentModel(String parent, String[]... textures) {
        return parentModel(null, parent, textures);
    }

    public static ModelData parentModel(String path, String parent, String[]... textures) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String[] pair : textures) {
            if (pair.length >= 2) {
                map.put(pair[0], pair[1]);
            }
        }
        return new ModelData(path, parent, map);
    }

    public Definition height(float height) {
        this.height = height;
        return this;
    }

    public Definition scale(float scale) {
        this.scale = scale;
        return this;
    }

    public Definition flat(boolean flat) {
        this.flat = flat;
        return this;
    }

    public Definition stackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public Definition allowsStackingOnTop(boolean allowsStackingOnTop) {
        this.allowsStackingOnTop = allowsStackingOnTop;
        return this;
    }

    public Definition condition(Condition condition, Consumer<Definition> consumer) {
        Definition nested = new Definition();
        consumer.accept(nested);
        this.conditions.add(new ConditionDefinition(condition, nested));
        return this;
    }

    public Definition condition(Condition condition, Definition nested) {
        this.conditions.add(new ConditionDefinition(condition, nested));
        return this;
    }

    public Definition notCondition(Condition condition, Consumer<Definition> consumer) {
        return condition(condition.not(), consumer);
    }

    public Definition notCondition(Condition condition, Definition nested) {
        return condition(condition.not(), nested);
    }

    public Definition isPlacematBlock(String blockOrTag, Consumer<Definition> consumer) {
        return condition(Condition.isPlacematBlock(blockOrTag), consumer);
    }

    public Definition isPlacematBlock(ResourceLocation block, Consumer<Definition> consumer) {
        return condition(Condition.isPlacematBlock(block), consumer);
    }

    public Definition isPlacematBlock(TagKey<Block> tag, Consumer<Definition> consumer) {
        return condition(Condition.isPlacematBlock(tag), consumer);
    }

    public Definition notPlacematBlock(String blockOrTag, Consumer<Definition> consumer) {
        return notCondition(Condition.isPlacematBlock(blockOrTag), consumer);
    }

    public Definition isRotten(Consumer<Definition> consumer) {
        return condition(Condition.isRotten(), consumer);
    }

    public Definition notRotten(Consumer<Definition> consumer) {
        return notCondition(Condition.isRotten(), consumer);
    }

    public Definition isPlacementRange(int rangeIndex, Consumer<Definition> consumer) {
        return condition(Condition.isPlacementRange(rangeIndex), consumer);
    }

    public Definition notPlacementRange(int rangeIndex, Consumer<Definition> consumer) {
        return notCondition(Condition.isPlacementRange(rangeIndex), consumer);
    }

    public Definition hasTrait(ResourceLocation trait, Consumer<Definition> consumer) {
        return condition(Condition.hasTrait(trait), consumer);
    }

    public Definition hasTrait(String trait, Consumer<Definition> consumer) {
        return condition(Condition.hasTrait(trait), consumer);
    }

    public Definition notTrait(ResourceLocation trait, Consumer<Definition> consumer) {
        return notCondition(Condition.hasTrait(trait), consumer);
    }

    public Definition notTrait(String trait, Consumer<Definition> consumer) {
        return notCondition(Condition.hasTrait(trait), consumer);
    }

    public Definition isItem(String itemOrTag, Consumer<Definition> consumer) {
        return condition(Condition.isItem(itemOrTag), consumer);
    }

    public Definition isItem(ResourceLocation item, Consumer<Definition> consumer) {
        return condition(Condition.isItem(item), consumer);
    }

    public Definition isItem(TagKey<Item> tag, Consumer<Definition> consumer) {
        return condition(Condition.isItem(tag), consumer);
    }

    public Definition notItem(String itemOrTag, Consumer<Definition> consumer) {
        return notCondition(Condition.isItem(itemOrTag), consumer);
    }

    public List<ConditionDefinition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }
}

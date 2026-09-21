package net.placemats.compat.kjs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

import dev.latvian.mods.kubejs.typings.Info;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.compat.tfc.TFCCompat;

@SuppressWarnings({"unused", "unchecked"})
public class PlaceMatBlockBuilder extends PlaceMatBlockBuilders {

    private int containerSize = 12;
    private boolean disableExtraction = false;
    private boolean disableInsertion = false;
    private int maxStackSize = 64;
    private boolean cardinal = false;
    private boolean disableLayFlat = false;
    private boolean disableCustomModels = false;
    private boolean rotateZones = true;
    private float scaleMultiplier = 1.0f;
    private float defaultYaw = 0;
    private float defaultPitch = 0;
    private float defaultRoll = 0;
    private float defaultElevation = 0;
    private ResourceLocation foodTrait = null;
    private final List<PlacementRangeBuilder> ranges = new ArrayList<>();
    private final List<StateRangeEntry> stateRanges = new ArrayList<>();
    private final List<PlaceMatBlock.RangeAdjuster> rangeAdjusters = new ArrayList<>();

    public record StateRangeEntry(int index, @Nullable Predicate<BlockState> predicate, PlacementRangeBuilder builder) {}

    public PlaceMatBlockBuilder(ResourceLocation i) {
        super(i);
    }

    @Info("Sets the max container size. (int, default: 12)")
    public PlaceMatBlockBuilder containerSize(int size) {
        this.containerSize = size;
        return this;
    }

    @Info("Disables item extraction from this block.")
    public PlaceMatBlockBuilder disableExtraction() {
        this.disableExtraction = true;
        return this;
    }

    @Info("Disables item insertion into this block.")
    public PlaceMatBlockBuilder disableInsertion() {
        this.disableInsertion = true;
        return this;
    }

    @Info("Sets the max stack size for items in this block. (int)")
    public PlaceMatBlockBuilder maxStackSize(int size) {
        this.maxStackSize = size;
        return this;
    }

    @Info("Sets if the block can rotate in the cardinal directions. (boolean)")
    public PlaceMatBlockBuilder isCardinal(boolean cardinal) {
        this.cardinal = cardinal;
        return this;
    }

    @Info("Sets whether placement zones should rotate with the block facing or stay fixed. (boolean, default: true)")
    public PlaceMatBlockBuilder rotateZones(boolean rotateZones) {
        this.rotateZones = rotateZones;
        return this;
    }

    @Info("Disables zone rotation with the block facing so zones stay fixed.")
    public PlaceMatBlockBuilder disableZoneRotation() {
        this.rotateZones = false;
        return this;
    }

    @Info("Applies a food trait to all items in this block. (String or ResourceLocation)")
    public PlaceMatBlockBuilder addFoodTrait(Object trait) {
        this.foodTrait = trait instanceof ResourceLocation rl ? rl : ResourceLocation.tryParse(trait.toString());
        return this;
    }

    @Info("Disables lay flat rendering for all items in this block.")
    public PlaceMatBlockBuilder disableLayFlat() {
        this.disableLayFlat = true;
        return this;
    }

    @Info("Disables custom model overrides for all items in this block.")
    public PlaceMatBlockBuilder disableCustomModels() {
        this.disableCustomModels = true;
        return this;
    }

    @Info("Sets the scale multiplier for all items in this block. (float, default: 1.0)")
    public PlaceMatBlockBuilder scaleMultiplier(float scale) {
        this.scaleMultiplier = scale;
        return this;
    }

    @Info("Sets the default rotation in degrees for all items in this block. (yaw, pitch, tilt)")
    public PlaceMatBlockBuilder setDisplayRotation(int yaw, int pitch, int tilt) {
        this.defaultYaw = yaw;
        this.defaultPitch = pitch;
        this.defaultRoll = tilt;
        return this;
    }

    @Info("Sets the default elevation for all items in this block. (float)")
    public PlaceMatBlockBuilder setDisplayElevation(float elevation) {
        this.defaultElevation = elevation;
        return this;
    }

    private int getNextRangeIndex() {
        int maxIndex = this.ranges.size() - 1;
        for (StateRangeEntry entry : this.stateRanges) {
            if (entry.index() > maxIndex) {
                maxIndex = entry.index();
            }
        }
        return maxIndex + 1;
    }

    @Info("Adds a placement range bounding box with custom parameters.")
    public PlaceMatBlockBuilder addPlacementRange(Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        consumer.accept(builder);
        this.ranges.add(builder);
        return this;
    }

    @Info("Adds a placement range bounding box at a specific index with custom parameters.")
    public PlaceMatBlockBuilder addPlacementRange(int index, Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        consumer.accept(builder);
        while (this.ranges.size() <= index) {
            this.ranges.add(null);
        }
        this.ranges.set(index, builder);
        return this;
    }

    @Info("Adds a placement range bounding box with custom parameters when a blockstate condition matches.")
    public PlaceMatBlockBuilder addPlacementRange(Predicate<BlockState> predicate, Consumer<PlacementRangeBuilder> consumer) {
        return addPlacementRange(getNextRangeIndex(), predicate, consumer);
    }

    @Info("Adds a placement range bounding box at a specific index with custom parameters when a blockstate condition matches.")
    public PlaceMatBlockBuilder addPlacementRange(int index, Predicate<BlockState> predicate, Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        consumer.accept(builder);
        this.stateRanges.add(new StateRangeEntry(index, predicate, builder));
        return this;
    }

    @Info("Adds a placement range bounding box when a blockstate property matches.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addPlacementRange(Property<T> property, T value, Consumer<PlacementRangeBuilder> consumer) {
        return addPlacementRange(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), consumer);
    }

    @Info("Adds a placement range bounding box at a specific index when a blockstate property matches.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addPlacementRange(int index, Property<T> property, T value, Consumer<PlacementRangeBuilder> consumer) {
        return addPlacementRange(index, state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), consumer);
    }

    @Info("Adds a placement range bounding box when a blockstate property matches any of the given values.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addPlacementRange(Property<T> property, Collection<T> values, Consumer<PlacementRangeBuilder> consumer) {
        return addPlacementRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), consumer);
    }

    @Info("Adds a placement range bounding box at a specific index when a blockstate property matches any of the given values.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addPlacementRange(int index, Property<T> property, Collection<T> values, Consumer<PlacementRangeBuilder> consumer) {
        return addPlacementRange(index, state -> state.hasProperty(property) && values.contains(state.getValue(property)), consumer);
    }

    @Info("Adds a restricted placement range bounding box with custom parameters. These boxes only hold one itemStack that cant be moved.")
    public PlaceMatBlockBuilder addRestrictedPlacementRange(Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        builder.restricted = true;
        consumer.accept(builder);
        this.ranges.add(builder);
        return this;
    }

    @Info("Adds a restricted placement range bounding box at a specific index with custom parameters.")
    public PlaceMatBlockBuilder addRestrictedPlacementRange(int index, Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        builder.restricted = true;
        consumer.accept(builder);
        while (this.ranges.size() <= index) {
            this.ranges.add(null);
        }
        this.ranges.set(index, builder);
        return this;
    }

    @Info("Adds a restricted placement range bounding box with custom parameters when a blockstate condition matches.")
    public PlaceMatBlockBuilder addRestrictedPlacementRange(Predicate<BlockState> predicate, Consumer<PlacementRangeBuilder> consumer) {
        return addRestrictedPlacementRange(getNextRangeIndex(), predicate, consumer);
    }

    @Info("Adds a restricted placement range bounding box at a specific index with custom parameters when a blockstate condition matches.")
    public PlaceMatBlockBuilder addRestrictedPlacementRange(int index, Predicate<BlockState> predicate, Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        builder.restricted = true;
        consumer.accept(builder);
        this.stateRanges.add(new StateRangeEntry(index, predicate, builder));
        return this;
    }

    @Info("Adds a restricted placement range bounding box when a blockstate property matches.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addRestrictedPlacementRange(Property<T> property, T value, Consumer<PlacementRangeBuilder> consumer) {
        return addRestrictedPlacementRange(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), consumer);
    }

    @Info("Adds a restricted placement range bounding box at a specific index when a blockstate property matches.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addRestrictedPlacementRange(int index, Property<T> property, T value, Consumer<PlacementRangeBuilder> consumer) {
        return addRestrictedPlacementRange(index, state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), consumer);
    }

    @Info("Adds a restricted placement range bounding box when a blockstate property matches any of the given values.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addRestrictedPlacementRange(Property<T> property, Collection<T> values, Consumer<PlacementRangeBuilder> consumer) {
        return addRestrictedPlacementRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), consumer);
    }

    @Info("Adds a restricted placement range bounding box at a specific index when a blockstate property matches any of the given values.")
    public <T extends Comparable<T>> PlaceMatBlockBuilder addRestrictedPlacementRange(int index, Property<T> property, Collection<T> values, Consumer<PlacementRangeBuilder> consumer) {
        return addRestrictedPlacementRange(index, state -> state.hasProperty(property) && values.contains(state.getValue(property)), consumer);
    }

    @Info("Adds a custom range adjuster to modify placement ranges based on blockstate.")
    public PlaceMatBlockBuilder addRangeAdjuster(PlaceMatBlock.RangeAdjuster adjuster) {
        this.rangeAdjusters.add(adjuster);
        return this;
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using a new bounding box.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, AABB newBox) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? range.withBox(newBox) : range);
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using a new bounding box shape.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, VoxelShape shape) {
        return adjustRange(predicate, shape.bounds());
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, double x1, double y1, double z1, double x2, double y2, double z2) {
        return adjustRange(predicate, Block.box(x1, y1, z1, x2, y2, z2));
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using a new bounding box and max height.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using a new bounding box shape and max height.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
        return adjustRange(predicate, shape.bounds(), newMaxHeight);
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using bounding box coordinates (in 1/16ths) and max height.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, double x1, double y1, double z1, double x2, double y2, double z2, float newMaxHeight) {
        return adjustRange(predicate, Block.box(x1, y1, z1, x2, y2, z2), newMaxHeight);
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using bounding box coordinates (in 1/16ths) and max height.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, float x1, float y1, float z1, float x2, float y2, float z2, float newMaxHeight) {
        return adjustRange(predicate, Block.box(x1, y1, z1, x2, y2, z2), newMaxHeight);
    }

    @Info("Adjusts placement ranges when a blockstate condition matches using bounding box coordinates (in 1/16ths) and max height.")
    public PlaceMatBlockBuilder adjustRange(Predicate<BlockState> predicate, int x1, int y1, int z1, int x2, int y2, int z2, float newMaxHeight) {
        return adjustRange(predicate, Block.box(x1, y1, z1, x2, y2, z2), newMaxHeight);
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using a new bounding box.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, AABB newBox) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? range.withBox(newBox) : range);
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using a new bounding box shape.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, VoxelShape shape) {
        return adjustRange(rangeIndex, predicate, shape.bounds());
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, double x1, double y1, double z1, double x2, double y2, double z2) {
        return adjustRange(rangeIndex, predicate, Block.box(x1, y1, z1, x2, y2, z2));
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, float x1, float y1, float z1, float x2, float y2, float z2) {
        return adjustRange(rangeIndex, predicate, Block.box(x1, y1, z1, x2, y2, z2));
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, int x1, int y1, int z1, int x2, int y2, int z2) {
        return adjustRange(rangeIndex, predicate, Block.box(x1, y1, z1, x2, y2, z2));
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using a new bounding box and max height.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using a new bounding box shape and max height.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, predicate, shape.bounds(), newMaxHeight);
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using bounding box coordinates (in 1/16ths) and max height.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, double x1, double y1, double z1, double x2, double y2, double z2, float newMaxHeight) {
        return adjustRange(rangeIndex, predicate, Block.box(x1, y1, z1, x2, y2, z2), newMaxHeight);
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using bounding box coordinates (in 1/16ths) and max height.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, float x1, float y1, float z1, float x2, float y2, float z2, float newMaxHeight) {
        return adjustRange(rangeIndex, predicate, Block.box(x1, y1, z1, x2, y2, z2), newMaxHeight);
    }

    @Info("Adjusts a specific placement range by index when a blockstate condition matches using bounding box coordinates (in 1/16ths) and max height.")
    public PlaceMatBlockBuilder adjustRange(int rangeIndex, Predicate<BlockState> predicate, int x1, int y1, int z1, int x2, int y2, int z2, float newMaxHeight) {
        return adjustRange(rangeIndex, predicate, Block.box(x1, y1, z1, x2, y2, z2), newMaxHeight);
    }

    @Override
    public PlaceMatBlock createObject() {
        PlaceMatBlock block = (PlaceMatBlock) TFCCompat.INSTANCE.createPlaceMatBlock(createProperties(), cardinal);
        block.containerSize(containerSize);
        block.maxStackSize(maxStackSize);
        if (disableExtraction) {
            block.disableExtraction();
        }
        if (disableInsertion) {
            block.disableInsertion();
        }
        if (foodTrait != null) {
            block.applyFoodTrait(foodTrait);
        }
        if (disableLayFlat) {
            block.disableLayFlat();
        }
        if (disableCustomModels) {
            block.disableCustomModels();
        }
        block.rotateZones(rotateZones);
        block.scaleMultiplier(scaleMultiplier);
        block.defaultRotation(defaultYaw, defaultPitch, defaultRoll);
        block.defaultElevation(defaultElevation);
        for (int i = 0; i < ranges.size(); i++) {
            PlacementRangeBuilder rangeBuilder = ranges.get(i);
            if (rangeBuilder != null) {
                block.addRange(i, rangeBuilder.build());
                for (PlaceMatBlock.RangeAdjuster adjuster : rangeBuilder.rangeAdjusters) {
                    int targetIndex = i;
                    block.addRangeAdjuster((state, range, index) -> index == targetIndex ? adjuster.adjust(state, range, index) : range);
                }
            }
        }
        for (StateRangeEntry entry : stateRanges) {
            block.addRange(entry.index(), entry.predicate(), entry.builder().build());
            for (PlaceMatBlock.RangeAdjuster adjuster : entry.builder().rangeAdjusters) {
                int targetIndex = entry.index();
                block.addRangeAdjuster((state, range, index) -> index == targetIndex ? adjuster.adjust(state, range, index) : range);
            }
        }
        for (PlaceMatBlock.RangeAdjuster adjuster : rangeAdjusters) {
            block.addRangeAdjuster(adjuster);
        }
        PlaceMatBlockEntities.addValidBEBlock(PlaceMatBlockEntities.PLACE_MAT, block);
        return block;
    }

    public static class PlacementRangeBuilder {
        private AABB box = new AABB(0, 0, 0, 1, 1, 1);
        private boolean disableRoll = false;
        private boolean disableYaw = false;
        private boolean disablePitch = false;
        private boolean disableElevation = false;
        private boolean disableStacking = false;
        private boolean disableCollision = false;
        private boolean disableExtraction = false;
        private boolean disableInsertion = false;
        private int maxStackSize = 64;
        private ResourceLocation foodTrait = null;
        private TagKey<Item> whitelistTag = null;
        private boolean restricted = false;
        private boolean disableLayFlat = false;
        private boolean disableCustomModels = false;
        private boolean snapToCenter = false;
        private float scaleMultiplier = 1.0f;
        private float defaultYaw = 0;
        private float defaultPitch = 0;
        private float defaultRoll = 0;
        private float defaultElevation = 0;
        private final List<PlaceMatBlock.RangeAdjuster> rangeAdjusters = new ArrayList<>();

        @Info("Adjusts this placement range when a blockstate condition matches using a new bounding box.")
        public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, AABB newBox) {
            this.rangeAdjusters.add((state, range, index) -> predicate.test(state) ? range.withBox(newBox) : range);
            return this;
        }

        @Info("Adjusts this placement range when a blockstate condition matches using a new bounding box shape.")
        public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, VoxelShape shape) {
            return adjustForState(predicate, shape.bounds());
        }

        @Info("Adjusts this placement range when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
    public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, double x1, double y1, double z1, double x2, double y2, double z2) {
            return adjustForState(predicate, Block.box(x1, y1, z1, x2, y2, z2));
        }

        @Info("Adjusts this placement range when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
        public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, float x1, float y1, float z1, float x2, float y2, float z2) {
            return adjustForState(predicate, Block.box(x1, y1, z1, x2, y2, z2));
        }

        @Info("Adjusts this placement range when a blockstate condition matches using bounding box coordinates (in 1/16ths).")
        public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, int x1, int y1, int z1, int x2, int y2, int z2) {
            return adjustForState(predicate, Block.box(x1, y1, z1, x2, y2, z2));
        }

        @Info("Adjusts this placement range when a blockstate condition matches using a new bounding box and max height.")
        public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
            this.rangeAdjusters.add((state, range, index) -> predicate.test(state) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
            return this;
        }

        @Info("Adjusts this placement range when a blockstate condition matches using a new bounding box shape and max height.")
        public PlacementRangeBuilder adjustForState(Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
            return adjustForState(predicate, shape.bounds(), newMaxHeight);
        }

        @Info("Sets the placement bounds using a VoxelShape.")
        public PlacementRangeBuilder placementBounds(VoxelShape shape) {
            this.box = shape.bounds();
            return this;
        }

        @Info("Sets the placement bounds. (x1, y1, z1, x2, y2, z2)")
        public PlacementRangeBuilder placementBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.box = Block.box(x1, y1, z1, x2, y2, z2).bounds();
            return this;
        }

        @Info("Sets the placement bounds. (x1, y1, z1, x2, y2, z2)")
        public PlacementRangeBuilder placementBounds(float x1, float y1, float z1, float x2, float y2, float z2) {
            this.box = Block.box(x1, y1, z1, x2, y2, z2).bounds();
            return this;
        }

        @Info("Sets the placement bounds. (x1, y1, z1, x2, y2, z2)")
        public PlacementRangeBuilder placementBounds(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.box = Block.box(x1, y1, z1, x2, y2, z2).bounds();
            return this;
        }

        @Info("Disables item roll rotation for this range.")
        public PlacementRangeBuilder disableRoll() {
            this.disableRoll = true;
            return this;
        }

        @Info("Disables item yaw rotation for this range.")
        public PlacementRangeBuilder disableYaw() {
            this.disableYaw = true;
            return this;
        }

        @Info("Disables item pitch rotation for this range.")
        public PlacementRangeBuilder disablePitch() {
            this.disablePitch = true;
            return this;
        }

        @Info("Disables item elevation changes for this range.")
        public PlacementRangeBuilder disableElevation() {
            this.disableElevation = true;
            return this;
        }

        @Info("Disables item stacking for this range.")
        public PlacementRangeBuilder disableStacking() {
            this.disableStacking = true;
            return this;
        }

        @Info("Disables collision checks between items for this range.")
        public PlacementRangeBuilder disableCollision() {
            this.disableCollision = true;
            return this;
        }

        @Info("Disables item extraction for this range.")
        public PlacementRangeBuilder disableExtraction() {
            this.disableExtraction = true;
            return this;
        }

        @Info("Disables item insertion for this range.")
        public PlacementRangeBuilder disableInsertion() {
            this.disableInsertion = true;
            return this;
        }

        @Info("Sets the max stack size for items in this range. (int, default: 64)")
        public PlacementRangeBuilder maxStackSize(int size) {
            this.maxStackSize = size;
            return this;
        }

        @Info("Applies a food trait to all items in this range. (String or ResourceLocation)")
        public PlacementRangeBuilder addFoodTrait(Object trait) {
            this.foodTrait = trait instanceof ResourceLocation rl ? rl : ResourceLocation.tryParse(trait.toString());
            return this;
        }

        @Info("Sets a whitelist item tag for this range. (String or ResourceLocation)")
        public PlacementRangeBuilder whitelistTag(Object tag) {
            if (tag instanceof TagKey<?> tagKey) {
                this.whitelistTag = (TagKey<Item>) tagKey;
            } else {
                String tagStr = tag.toString();
                if (tagStr.startsWith("#")) {
                    tagStr = tagStr.substring(1);
                }
                this.whitelistTag = TagKey.create(ForgeRegistries.Keys.ITEMS, Objects.requireNonNull(ResourceLocation.tryParse(tagStr)));
            }
            return this;
        }

        @Info("Disables lay flat rendering for items in this range.")
        public PlacementRangeBuilder disableLayFlat() {
            this.disableLayFlat = true;
            return this;
        }

        @Info("Disables custom model overrides for items in this range.")
        public PlacementRangeBuilder disableCustomModels() {
            this.disableCustomModels = true;
            return this;
        }

        @Info("Snaps the item to the center of the vertical axis of the placement range.")
        public PlacementRangeBuilder snapToCenter() {
            this.snapToCenter = true;
            return this;
        }

        @Info("Sets the scale multiplier for items in this range. (float, default: 1.0)")
        public PlacementRangeBuilder scaleMultiplier(float scale) {
            this.scaleMultiplier = scale;
            return this;
        }

        @Info("Sets the default rotation in degrees for items in this range. (yaw, pitch, tilt)")
        public PlacementRangeBuilder setDisplayRotation(int yaw, int pitch, int tilt) {
            this.defaultYaw = yaw;
            this.defaultPitch = pitch;
            this.defaultRoll = tilt;
            return this;
        }

        @Info("Sets the default elevation for items in this range. (float)")
        public PlacementRangeBuilder setDisplayElevation(float elevation) {
            this.defaultElevation = elevation;
            return this;
        }

        public PlaceMatBlock.PlacementRange build() {
            return new PlaceMatBlock.PlacementRange(
                    box,
                    (float) box.maxY,
                    disableRoll,
                    disableYaw,
                    disablePitch,
                    disableElevation,
                    !disableStacking,
                    disableCollision,
                    whitelistTag,
                    disableExtraction,
                    disableInsertion,
                    maxStackSize,
                    foodTrait,
                    restricted,
                    disableLayFlat,
                    disableCustomModels,
                    snapToCenter,
                    scaleMultiplier,
                    defaultYaw,
                    defaultPitch,
                    defaultRoll,
                    defaultElevation);
        }
    }
}

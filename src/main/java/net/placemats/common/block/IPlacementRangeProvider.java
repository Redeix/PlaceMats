package net.placemats.common.block;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Interface providing placement range definitions, zone calculations, and transformation methods.
 */
@SuppressWarnings({ "unchecked", "UnusedReturnValue", "unused" })
public interface IPlacementRangeProvider {

    record StatePlacementRange(int index, @Nullable Predicate<BlockState> predicate, PlacementRange range) {}

    @FunctionalInterface
    interface RangeAdjuster {
        PlacementRange adjust(BlockState state, PlacementRange range, int index);
    }

    PlaceMatPlacementDefinition getPlacementDefinition();

    default int getContainerSize() {
        return getPlacementDefinition().getContainerSize();
    }

    default List<PlacementRange> getPlacementRanges() {
        return getPlacementDefinition().getPlacementRanges();
    }

    default List<StatePlacementRange> getStatePlacementRanges() {
        return getPlacementDefinition().getStatePlacementRanges();
    }

    default boolean isExtractionDisabled() {
        return getPlacementDefinition().isExtractionDisabled();
    }

    default boolean isInsertionDisabled() {
        return getPlacementDefinition().isInsertionDisabled();
    }

    default int getMaxStackSize() {
        return getPlacementDefinition().getMaxStackSize();
    }

    default boolean isDisableLayFlat() {
        return getPlacementDefinition().isDisableLayFlat();
    }

    default boolean isDisableCustomModels() {
        return getPlacementDefinition().isDisableCustomModels();
    }

    default boolean isRotateZones() {
        return getPlacementDefinition().isRotateZones();
    }

    default float getScaleMultiplier() {
        return getPlacementDefinition().getScaleMultiplier();
    }

    default float getDefaultYaw() {
        return getPlacementDefinition().getDefaultYaw();
    }

    default float getDefaultPitch() {
        return getPlacementDefinition().getDefaultPitch();
    }

    default float getDefaultRoll() {
        return getPlacementDefinition().getDefaultRoll();
    }

    default float getDefaultElevation() {
        return getPlacementDefinition().getDefaultElevation();
    }

    @Nullable
    default ResourceLocation getFoodTrait() {
        return getPlacementDefinition().getFoodTrait();
    }

    default List<PlacementRange> getPlacementRanges(BlockState state) {
        return getPlacementDefinition().getPlacementRanges(state);
    }

    default PlacementRange getAdjustedPlacementRange(BlockState state, PlacementRange range) {
        return getPlacementDefinition().getAdjustedPlacementRange(state, range);
    }

    default PlacementRange getAdjustedPlacementRange(BlockState state, PlacementRange range, int index) {
        return getPlacementDefinition().getAdjustedPlacementRange(state, range, index);
    }

    default void addPlacementRanges(BlockState state, Consumer<PlacementRange> consumer) {
        getPlacementDefinition().addPlacementRanges(state, consumer);
    }

    @Nullable
    default PlacementRange getTargetedPlacementRange(BlockState state, Vec3 relativeHitVec) {
        return getPlacementDefinition().getTargetedPlacementRange(state, relativeHitVec);
    }

    static void addPlacementRangesStatic(BlockState state, Consumer<PlacementRange> consumer) {
        if (state.getBlock() instanceof IPlacementRangeProvider provider) {
            provider.addPlacementRanges(state, consumer);
        }
    }

    /* ======================== Configuration Builders ========================== */

    default <T extends IPlacementRangeProvider> T containerSize(int size) {
        getPlacementDefinition().setContainerSize(size);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T disableExtraction() {
        getPlacementDefinition().setExtractionDisabled(true);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T disableInsertion() {
        getPlacementDefinition().setInsertionDisabled(true);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T maxStackSize(int maxStackSize) {
        getPlacementDefinition().setMaxStackSize(maxStackSize);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T applyFoodTrait(ResourceLocation trait) {
        getPlacementDefinition().setFoodTrait(trait);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T disableLayFlat() {
        getPlacementDefinition().setDisableLayFlat(true);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T disableCustomModels() {
        getPlacementDefinition().setDisableCustomModels(true);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T rotateZones(boolean rotateZones) {
        getPlacementDefinition().setRotateZones(rotateZones);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T disableZoneRotation() {
        getPlacementDefinition().setRotateZones(false);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T scaleMultiplier(float scaleMultiplier) {
        getPlacementDefinition().setScaleMultiplier(scaleMultiplier);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T defaultRotation(float yaw, float pitch, float roll) {
        getPlacementDefinition().setDefaultYaw(yaw);
        getPlacementDefinition().setDefaultPitch(pitch);
        getPlacementDefinition().setDefaultRoll(roll);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T defaultElevation(float elevation) {
        getPlacementDefinition().setDefaultElevation(elevation);
        return (T) this;
    }

    /* ======================== Range Addition ========================== */

    default <T extends IPlacementRangeProvider> T addRange(int index, PlacementRange range) {
        while (getPlacementDefinition().getPlacementRanges().size() <= index) {
            getPlacementDefinition().getPlacementRanges().add(null);
        }
        getPlacementDefinition().getPlacementRanges().set(index, range);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T addRange(PlacementRange range) {
        return addRange(getPlacementDefinition().getNextRangeIndex(), range);
    }

    default <T extends IPlacementRangeProvider> T addRange(int index, Predicate<BlockState> predicate, PlacementRange range) {
        getPlacementDefinition().getStatePlacementRanges().add(new StatePlacementRange(index, predicate, range));
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T addRange(Predicate<BlockState> predicate, PlacementRange range) {
        return addRange(getPlacementDefinition().getNextRangeIndex(), predicate, range);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T addRange(int index, Property<V> property, V value, PlacementRange range) {
        return addRange(index, state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), range);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T addRange(Property<V> property, V value, PlacementRange range) {
        return addRange(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), range);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T addRange(int index, Property<V> property, Collection<V> values, PlacementRange range) {
        return addRange(index, state -> state.hasProperty(property) && values.contains(state.getValue(property)), range);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T addRange(Property<V> property, Collection<V> values, PlacementRange range) {
        return addRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), range);
    }

    /* ======================== Range Adjusters ========================== */

    default <T extends IPlacementRangeProvider> T addRangeAdjuster(RangeAdjuster adjuster) {
        getPlacementDefinition().getRangeAdjusters().add(adjuster);
        return (T) this;
    }

    default <T extends IPlacementRangeProvider> T adjustRange(RangeAdjuster adjuster) {
        return addRangeAdjuster(adjuster);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(Predicate<BlockState> predicate, AABB newBox) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? range.withBox(newBox) : range);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(Predicate<BlockState> predicate, VoxelShape shape) {
        return adjustRange(predicate, shape.bounds());
    }

    default <T extends IPlacementRangeProvider> T adjustRange(Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
        return adjustRange(predicate, shape.bounds(), newMaxHeight);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Predicate<BlockState> predicate, AABB newBox) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? range.withBox(newBox) : range);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Predicate<BlockState> predicate, VoxelShape shape) {
        return adjustRange(rangeIndex, predicate, shape.bounds());
    }

    default <T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, predicate, shape.bounds(), newMaxHeight);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(Predicate<BlockState> predicate, Function<PlacementRange, PlacementRange> transformer) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? transformer.apply(range) : range);
    }

    default <T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Predicate<BlockState> predicate, Function<PlacementRange, PlacementRange> transformer) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? transformer.apply(range) : range);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, V value, AABB newBox) {
        return adjustRange(state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, V value, VoxelShape shape) {
        return adjustRange(property, value, shape.bounds());
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, V value, AABB newBox, float newMaxHeight) {
        return adjustRange(state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox, newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, V value, VoxelShape shape, float newMaxHeight) {
        return adjustRange(property, value, shape.bounds(), newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, Collection<V> values, AABB newBox) {
        return adjustRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, Collection<V> values, VoxelShape shape) {
        return adjustRange(property, values, shape.bounds());
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, Collection<V> values, AABB newBox, float newMaxHeight) {
        return adjustRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox, newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, Collection<V> values, VoxelShape shape, float newMaxHeight) {
        return adjustRange(property, values, shape.bounds(), newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, Collection<V> values, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), transformer);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, V value, AABB newBox) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, V value, VoxelShape shape) {
        return adjustRange(rangeIndex, property, value, shape.bounds());
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, V value, AABB newBox, float newMaxHeight) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox, newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, V value, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, property, value, shape.bounds(), newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, Collection<V> values, AABB newBox) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, Collection<V> values, VoxelShape shape) {
        return adjustRange(rangeIndex, property, values, shape.bounds());
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, Collection<V> values, AABB newBox, float newMaxHeight) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox, newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, Collection<V> values, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, property, values, shape.bounds(), newMaxHeight);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, V value, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), transformer);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(int rangeIndex, Property<V> property, Collection<V> values, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && values.contains(state.getValue(property)), transformer);
    }

    default <V extends Comparable<V>, T extends IPlacementRangeProvider> T adjustRange(Property<V> property, V value, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), transformer);
    }
}

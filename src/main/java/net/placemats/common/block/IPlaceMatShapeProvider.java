package net.placemats.common.block;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.placemats.common.block.PlaceMatShapeHelper.*;

/**
 * Interface providing shape and collision shape definitions and transformation methods.
 */
@SuppressWarnings({ "unchecked", "UnusedReturnValue", "unused" })
public interface IPlaceMatShapeProvider {

    record StateShape(Predicate<BlockState> predicate, VoxelShape shape) {}

    PlaceMatShapeDefinition getShapeDefinition();

    default VoxelShape getShape(BlockState state) {
        return getShapeDefinition().getShape(state);
    }

    default VoxelShape getCollisionShape(BlockState state) {
        return getShapeDefinition().getCollisionShape(state);
    }

    default VoxelShape getShape() {
        return getShapeDefinition().getShape();
    }

    default VoxelShape getCollisionShape() {
        return getShapeDefinition().getCollisionShape();
    }

    default List<StateShape> getShapeOverrides() {
        return getShapeDefinition().getShapeOverrides();
    }

    default List<StateShape> getCollisionShapeOverrides() {
        return getShapeDefinition().getCollisionShapeOverrides();
    }

    /* ======================== Shape ========================== */

    default <T extends IPlaceMatShapeProvider> T shape(VoxelShape shape) {
        getShapeDefinition().setShape(shape);
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T shape(AABB box) {
        return shape(Shapes.create(box));
    }

    default <T extends IPlaceMatShapeProvider> T shape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    default <T extends IPlaceMatShapeProvider> T shape(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T shape(Property<V> property, V value, VoxelShape shape) {
        return addShapeOverride(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T shape(Property<V> property, V value, AABB box) {
        return shape(property, value, Shapes.create(box));
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T shape(Property<V> property, Collection<V> values, VoxelShape shape) {
        return addShapeOverride(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T shape(Property<V> property, Collection<V> values, AABB box) {
        return shape(property, values, Shapes.create(box));
    }

    default <T extends IPlaceMatShapeProvider> T addShapeOverride(Predicate<BlockState> predicate, VoxelShape shape) {
        getShapeDefinition().getShapeOverrides().add(new StateShape(predicate, shape));
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T addShapeOverride(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, Shapes.create(box));
    }

    default <T extends IPlaceMatShapeProvider> T adjustShape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    default <T extends IPlaceMatShapeProvider> T adjustShape(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustShape(Property<V> property, V value, VoxelShape shape) {
        return shape(property, value, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustShape(Property<V> property, V value, AABB box) {
        return shape(property, value, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustShape(Property<V> property, Collection<V> values, VoxelShape shape) {
        return shape(property, values, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustShape(Property<V> property, Collection<V> values, AABB box) {
        return shape(property, values, box);
    }

    /* ======================== Bounding Box ========================== */

    default <T extends IPlaceMatShapeProvider> T boundingBox(VoxelShape shape) {
        return shape(shape);
    }

    default <T extends IPlaceMatShapeProvider> T boundingBox(AABB box) {
        return shape(box);
    }

    default <T extends IPlaceMatShapeProvider> T boundingBox(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    default <T extends IPlaceMatShapeProvider> T boundingBox(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T boundingBox(Property<V> property, V value, VoxelShape shape) {
        return shape(property, value, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T boundingBox(Property<V> property, V value, AABB box) {
        return shape(property, value, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T boundingBox(Property<V> property, Collection<V> values, VoxelShape shape) {
        return shape(property, values, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T boundingBox(Property<V> property, Collection<V> values, AABB box) {
        return shape(property, values, box);
    }

    default <T extends IPlaceMatShapeProvider> T adjustBoundingBox(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    default <T extends IPlaceMatShapeProvider> T adjustBoundingBox(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustBoundingBox(Property<V> property, V value, VoxelShape shape) {
        return shape(property, value, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustBoundingBox(Property<V> property, V value, AABB box) {
        return shape(property, value, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustBoundingBox(Property<V> property, Collection<V> values, VoxelShape shape) {
        return shape(property, values, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustBoundingBox(Property<V> property, Collection<V> values, AABB box) {
        return shape(property, values, box);
    }

    /* ======================== Collision Shape ========================== */

    default <T extends IPlaceMatShapeProvider> T collisionShape(VoxelShape collisionShape) {
        getShapeDefinition().setCollisionShape(collisionShape);
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T collisionShape(AABB box) {
        return collisionShape(Shapes.create(box));
    }

    default <T extends IPlaceMatShapeProvider> T addCollisionShapeOverride(Predicate<BlockState> predicate, VoxelShape shape) {
        getShapeDefinition().getCollisionShapeOverrides().add(new StateShape(predicate, shape));
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T addCollisionShapeOverride(Predicate<BlockState> predicate, AABB box) {
        return addCollisionShapeOverride(predicate, Shapes.create(box));
    }

    default <T extends IPlaceMatShapeProvider> T collisionShape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addCollisionShapeOverride(predicate, shape);
    }

    default <T extends IPlaceMatShapeProvider> T collisionShape(Predicate<BlockState> predicate, AABB box) {
        return addCollisionShapeOverride(predicate, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T collisionShape(Property<V> property, V value, VoxelShape shape) {
        return addCollisionShapeOverride(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T collisionShape(Property<V> property, V value, AABB box) {
        return collisionShape(property, value, Shapes.create(box));
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T collisionShape(Property<V> property, Collection<V> values, VoxelShape shape) {
        return addCollisionShapeOverride(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T collisionShape(Property<V> property, Collection<V> values, AABB box) {
        return collisionShape(property, values, Shapes.create(box));
    }

    default <T extends IPlaceMatShapeProvider> T adjustCollisionShape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addCollisionShapeOverride(predicate, shape);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCollisionShape(Predicate<BlockState> predicate, AABB box) {
        return addCollisionShapeOverride(predicate, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCollisionShape(Property<V> property, V value, VoxelShape shape) {
        return collisionShape(property, value, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCollisionShape(Property<V> property, V value, AABB box) {
        return collisionShape(property, value, box);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCollisionShape(Property<V> property, Collection<V> values, VoxelShape shape) {
        return collisionShape(property, values, shape);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCollisionShape(Property<V> property, Collection<V> values, AABB box) {
        return collisionShape(property, values, box);
    }

    /* ======================== Cardinal Helpers ========================== */

    default <T extends IPlaceMatShapeProvider> T cardinalShape(VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            shape(PlaceMatCardinalBlock.FACING, dir, rotateShape(dir, combined));
        }
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T cardinalShape(AABB... boxes) {
        return cardinalShape(combineBoxes(boxes));
    }

    default <T extends IPlaceMatShapeProvider> T cardinalBoundingBox(VoxelShape... shapes) {
        return cardinalShape(shapes);
    }

    default <T extends IPlaceMatShapeProvider> T cardinalBoundingBox(AABB... boxes) {
        return cardinalShape(boxes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalShape(VoxelShape... shapes) {
        return cardinalShape(shapes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalShape(AABB... boxes) {
        return cardinalShape(boxes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(VoxelShape... shapes) {
        return cardinalShape(shapes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(AABB... boxes) {
        return cardinalShape(boxes);
    }

    default <T extends IPlaceMatShapeProvider> T cardinalShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            addShapeOverride(state -> state.hasProperty(PlaceMatCardinalBlock.FACING)
                    && state.getValue(PlaceMatCardinalBlock.FACING) == dir
                    && predicate.test(state), rotateShape(dir, combined));
        }
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T cardinalShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, combineBoxes(boxes));
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalShape(Property<V> property, V value, VoxelShape... shapes) {
        return cardinalShape(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalShape(Property<V> property, V value, AABB... boxes) {
        return cardinalShape(property, value, combineBoxes(boxes));
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalShape(Property<V> property, Collection<V> values, VoxelShape... shapes) {
        return cardinalShape(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalShape(Property<V> property, Collection<V> values, AABB... boxes) {
        return cardinalShape(property, values, combineBoxes(boxes));
    }

    default <T extends IPlaceMatShapeProvider> T cardinalBoundingBox(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalShape(predicate, shapes);
    }

    default <T extends IPlaceMatShapeProvider> T cardinalBoundingBox(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalBoundingBox(Property<V> property, V value, VoxelShape... shapes) {
        return cardinalShape(property, value, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalBoundingBox(Property<V> property, V value, AABB... boxes) {
        return cardinalShape(property, value, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalBoundingBox(Property<V> property, Collection<V> values, VoxelShape... shapes) {
        return cardinalShape(property, values, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalBoundingBox(Property<V> property, Collection<V> values, AABB... boxes) {
        return cardinalShape(property, values, boxes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalShape(predicate, shapes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalShape(Property<V> property, V value, VoxelShape... shapes) {
        return cardinalShape(property, value, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalShape(Property<V> property, V value, AABB... boxes) {
        return cardinalShape(property, value, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalShape(Property<V> property, Collection<V> values, VoxelShape... shapes) {
        return cardinalShape(property, values, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalShape(Property<V> property, Collection<V> values, AABB... boxes) {
        return cardinalShape(property, values, boxes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalShape(predicate, shapes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(Property<V> property, V value, VoxelShape... shapes) {
        return cardinalShape(property, value, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(Property<V> property, V value, AABB... boxes) {
        return cardinalShape(property, value, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(Property<V> property, Collection<V> values, VoxelShape... shapes) {
        return cardinalShape(property, values, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalBoundingBox(Property<V> property, Collection<V> values, AABB... boxes) {
        return cardinalShape(property, values, boxes);
    }

    default <T extends IPlaceMatShapeProvider> T cardinalCollisionShape(VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            collisionShape(PlaceMatCardinalBlock.FACING, dir, rotateShape(dir, combined));
        }
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T cardinalCollisionShape(AABB... boxes) {
        return cardinalCollisionShape(combineBoxes(boxes));
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(VoxelShape... shapes) {
        return cardinalCollisionShape(shapes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(AABB... boxes) {
        return cardinalCollisionShape(boxes);
    }

    default <T extends IPlaceMatShapeProvider> T cardinalCollisionShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            addCollisionShapeOverride(state -> state.hasProperty(PlaceMatCardinalBlock.FACING)
                    && state.getValue(PlaceMatCardinalBlock.FACING) == dir
                    && predicate.test(state), rotateShape(dir, combined));
        }
        return (T) this;
    }

    default <T extends IPlaceMatShapeProvider> T cardinalCollisionShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalCollisionShape(predicate, combineBoxes(boxes));
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalCollisionShape(Property<V> property, V value, VoxelShape... shapes) {
        return cardinalCollisionShape(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalCollisionShape(Property<V> property, V value, AABB... boxes) {
        return cardinalCollisionShape(property, value, combineBoxes(boxes));
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalCollisionShape(Property<V> property, Collection<V> values, VoxelShape... shapes) {
        return cardinalCollisionShape(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T cardinalCollisionShape(Property<V> property, Collection<V> values, AABB... boxes) {
        return cardinalCollisionShape(property, values, combineBoxes(boxes));
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalCollisionShape(predicate, shapes);
    }

    default <T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalCollisionShape(predicate, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(Property<V> property, V value, VoxelShape... shapes) {
        return cardinalCollisionShape(property, value, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(Property<V> property, V value, AABB... boxes) {
        return cardinalCollisionShape(property, value, boxes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(Property<V> property, Collection<V> values, VoxelShape... shapes) {
        return cardinalCollisionShape(property, values, shapes);
    }

    default <V extends Comparable<V>, T extends IPlaceMatShapeProvider> T adjustCardinalCollisionShape(Property<V> property, Collection<V> values, AABB... boxes) {
        return cardinalCollisionShape(property, values, boxes);
    }
}

package net.placemats.common.block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.placemats.common.event.PlaceMatInteractions;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;

import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatTags;

/**
 * Base class for place mats, which are blocks that can hold items.
 * Provides consumers for individual placement ranges and for the block as a whole.
 * Settings for the block (such as max stack size) take priority over individual placement range settings.
 */
@SuppressWarnings({ "deprecation", "UnusedReturnValue", "unused" })
public class PlaceMatBlock extends Block implements EntityBlock {

    public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;

    @Getter
    private int containerSize = 12;
    @Getter
    private final List<PlacementRange> placementRanges = new ArrayList<>();
    @Getter
    private final List<StatePlacementRange> statePlacementRanges = new ArrayList<>();

    public record StatePlacementRange(int index, @Nullable Predicate<BlockState> predicate, PlacementRange range) {}

    @Getter
    private boolean extractionDisabled = false;
    @Getter
    private boolean insertionDisabled = false;
    @Getter
    private int maxStackSize = 64;
    @Getter
    private boolean disableLayFlat = false;
    @Getter
    private boolean disableCustomModels = false;
    @Getter
    private boolean rotateZones = true;
    @Getter
    private float scaleMultiplier = 1.0f;
    @Getter
    private float defaultYaw = 0;
    @Getter
    private float defaultPitch = 0;
    @Getter
    private float defaultRoll = 0;
    @Getter
    private float defaultElevation = 0;
    @Getter
    @Nullable
    private ResourceLocation foodTrait = null;
    @Getter
    private VoxelShape shape = Shapes.block();
    @Getter
    private VoxelShape collisionShape = Shapes.block();
    @Getter
    private final List<StateShape> shapeOverrides = new ArrayList<>();
    @Getter
    private final List<StateShape> collisionShapeOverrides = new ArrayList<>();

    public record StateShape(Predicate<BlockState> predicate, VoxelShape shape) {}

    public PlaceMatBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(LOCKED, false));
    }

    /**
     * Sets the container size which limits the amount of item stacks within the block's inventory.
     */
    public PlaceMatBlock containerSize(int size) {
        this.containerSize = size;
        return this;
    }

    /**
     * Disables the ability to extract item stacks from the block's inventory.
     */
    public PlaceMatBlock disableExtraction() {
        this.extractionDisabled = true;
        return this;
    }

    /**
     * Disables the ability to insert item stacks into the block's inventory.
     */
    public PlaceMatBlock disableInsertion() {
        this.insertionDisabled = true;
        return this;
    }

    /**
     * Sets the maximum stack size for individual item stacks within the block's inventory.
     */
    public PlaceMatBlock maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    /**
     * If TFC compat is enabled, applies a specified food trait to all food items placed on the block.
     */
    public PlaceMatBlock applyFoodTrait(ResourceLocation trait) {
        this.foodTrait = trait;
        return this;
    }

    /**
     * Default items will render laying flat on the block. This setting will make them upright instead.
     * Useful when you want to display things like item frames.
     */
    public PlaceMatBlock disableLayFlat() {
        this.disableLayFlat = true;
        return this;
    }

    /**
     * Disables rendering for custom models defined with a custom model JSON file.
     * Useful when you want items to display normally.
     */
    public PlaceMatBlock disableCustomModels() {
        this.disableCustomModels = true;
        return this;
    }

    /**
     * Sets whether placement zones should rotate with the block facing or stay fixed.
     * Default is true (rotation enabled).
     */
    public PlaceMatBlock rotateZones(boolean rotateZones) {
        this.rotateZones = rotateZones;
        return this;
    }

    /**
     * Disables zone rotation with block facing so placement zones stay fixed.
     */
    public PlaceMatBlock disableZoneRotation() {
        this.rotateZones = false;
        return this;
    }

    /**
     * Sets a scale multiplier for all rendered models.
     */
    public PlaceMatBlock scaleMultiplier(float scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier;
        return this;
    }

    /**
     * Sets a default rotation for all rendered models.
     */
    public PlaceMatBlock defaultRotation(float yaw, float pitch, float roll) {
        this.defaultYaw = yaw;
        this.defaultPitch = pitch;
        this.defaultRoll = roll;
        return this;
    }

    /**
     * Sets a default elevation (y-offset) for all rendered models.
     */
    public PlaceMatBlock defaultElevation(float elevation) {
        this.defaultElevation = elevation;
        return this;
    }

    public static VoxelShape box(float x1, float y1, float z1, float x2, float y2, float z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    /* ======================== Shape ========================== */

    public PlaceMatBlock shape(VoxelShape shape) {
        this.shape = shape;
        return this;
    }

    public PlaceMatBlock shape(AABB box) {
        return shape(Shapes.create(box));
    }

    public PlaceMatBlock shape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    public PlaceMatBlock shape(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock shape(Property<T> property, T value, VoxelShape shape) {
        return addShapeOverride(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock shape(Property<T> property, T value, AABB box) {
        return shape(property, value, Shapes.create(box));
    }

    public <T extends Comparable<T>> PlaceMatBlock shape(Property<T> property, Collection<T> values, VoxelShape shape) {
        return addShapeOverride(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock shape(Property<T> property, Collection<T> values, AABB box) {
        return shape(property, values, Shapes.create(box));
    }

    public PlaceMatBlock addShapeOverride(Predicate<BlockState> predicate, VoxelShape shape) {
        this.shapeOverrides.add(new StateShape(predicate, shape));
        return this;
    }

    public PlaceMatBlock addShapeOverride(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, Shapes.create(box));
    }

    public PlaceMatBlock adjustShape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    public PlaceMatBlock adjustShape(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustShape(Property<T> property, T value, VoxelShape shape) {
        return shape(property, value, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustShape(Property<T> property, T value, AABB box) {
        return shape(property, value, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustShape(Property<T> property, Collection<T> values, VoxelShape shape) {
        return shape(property, values, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustShape(Property<T> property, Collection<T> values, AABB box) {
        return shape(property, values, box);
    }

    /* ======================== Bounding Box ========================== */

    public PlaceMatBlock boundingBox(VoxelShape shape) {
        return shape(shape);
    }

    public PlaceMatBlock boundingBox(AABB box) {
        return shape(box);
    }

    public PlaceMatBlock boundingBox(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    public PlaceMatBlock boundingBox(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock boundingBox(Property<T> property, T value, VoxelShape shape) {
        return shape(property, value, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock boundingBox(Property<T> property, T value, AABB box) {
        return shape(property, value, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock boundingBox(Property<T> property, Collection<T> values, VoxelShape shape) {
        return shape(property, values, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock boundingBox(Property<T> property, Collection<T> values, AABB box) {
        return shape(property, values, box);
    }

    public PlaceMatBlock adjustBoundingBox(Predicate<BlockState> predicate, VoxelShape shape) {
        return addShapeOverride(predicate, shape);
    }

    public PlaceMatBlock adjustBoundingBox(Predicate<BlockState> predicate, AABB box) {
        return addShapeOverride(predicate, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustBoundingBox(Property<T> property, T value, VoxelShape shape) {
        return shape(property, value, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustBoundingBox(Property<T> property, T value, AABB box) {
        return shape(property, value, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustBoundingBox(Property<T> property, Collection<T> values, VoxelShape shape) {
        return shape(property, values, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustBoundingBox(Property<T> property, Collection<T> values, AABB box) {
        return shape(property, values, box);
    }

    /* ======================== Collision Shape ========================== */

    public PlaceMatBlock collisionShape(VoxelShape collisionShape) {
        this.collisionShape = collisionShape;
        return this;
    }

    public PlaceMatBlock collisionShape(AABB box) {
        return collisionShape(Shapes.create(box));
    }

    public PlaceMatBlock addCollisionShapeOverride(Predicate<BlockState> predicate, VoxelShape shape) {
        this.collisionShapeOverrides.add(new StateShape(predicate, shape));
        return this;
    }

    public PlaceMatBlock addCollisionShapeOverride(Predicate<BlockState> predicate, AABB box) {
        return addCollisionShapeOverride(predicate, Shapes.create(box));
    }

    public PlaceMatBlock collisionShape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addCollisionShapeOverride(predicate, shape);
    }

    public PlaceMatBlock collisionShape(Predicate<BlockState> predicate, AABB box) {
        return addCollisionShapeOverride(predicate, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock collisionShape(Property<T> property, T value, VoxelShape shape) {
        return addCollisionShapeOverride(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock collisionShape(Property<T> property, T value, AABB box) {
        return collisionShape(property, value, Shapes.create(box));
    }

    public <T extends Comparable<T>> PlaceMatBlock collisionShape(Property<T> property, Collection<T> values, VoxelShape shape) {
        return addCollisionShapeOverride(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock collisionShape(Property<T> property, Collection<T> values, AABB box) {
        return collisionShape(property, values, Shapes.create(box));
    }

    public PlaceMatBlock adjustCollisionShape(Predicate<BlockState> predicate, VoxelShape shape) {
        return addCollisionShapeOverride(predicate, shape);
    }

    public PlaceMatBlock adjustCollisionShape(Predicate<BlockState> predicate, AABB box) {
        return addCollisionShapeOverride(predicate, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCollisionShape(Property<T> property, T value, VoxelShape shape) {
        return collisionShape(property, value, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCollisionShape(Property<T> property, T value, AABB box) {
        return collisionShape(property, value, box);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCollisionShape(Property<T> property, Collection<T> values, VoxelShape shape) {
        return collisionShape(property, values, shape);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCollisionShape(Property<T> property, Collection<T> values, AABB box) {
        return collisionShape(property, values, box);
    }

    /* ======================== Shape Manipulation ========================== */

    public static VoxelShape combineShapes(VoxelShape... shapes) {
        VoxelShape combined = Shapes.empty();
        for (VoxelShape shape : shapes) {
            if (shape != null) {
                combined = Shapes.or(combined, shape);
            }
        }
        return combined;
    }

    public static VoxelShape combineBoxes(AABB... boxes) {
        VoxelShape combined = Shapes.empty();
        for (AABB box : boxes) {
            if (box != null) {
                combined = Shapes.or(combined, Shapes.create(box));
            }
        }
        return combined;
    }

    public static AABB rotateAABB(Direction direction, AABB box) {
        return switch (direction) {
            case SOUTH -> new AABB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY, 1 - box.minZ);
            case EAST -> new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
            case WEST -> new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
            default -> box;
        };
    }

    public static VoxelShape rotateShape(Direction direction, VoxelShape shape) {
        if (direction == Direction.NORTH || direction == null) {
            return shape;
        }
        VoxelShape result = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            result = Shapes.or(result, Shapes.create(rotateAABB(direction, box)));
        }
        return result;
    }

    public static VoxelShape rotateShape(Direction direction, VoxelShape... shapes) {
        return rotateShape(direction, combineShapes(shapes));
    }

    public static VoxelShape rotateShape(Direction direction, AABB... boxes) {
        return rotateShape(direction, combineBoxes(boxes));
    }


    /* ======================== Cardinal Helpers ========================== */

    public static Map<Direction, VoxelShape> createCardinalRotations(VoxelShape shape) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            map.put(dir, rotateShape(dir, shape));
        }
        return map;
    }

    public static Map<Direction, VoxelShape> createCardinalRotations(VoxelShape... shapes) {
        return createCardinalRotations(combineShapes(shapes));
    }

    public static Map<Direction, VoxelShape> createCardinalRotations(AABB... boxes) {
        return createCardinalRotations(combineBoxes(boxes));
    }

    public PlaceMatBlock cardinalShape(VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            shape(PlaceMatCardinalBlock.FACING, dir, rotateShape(dir, combined));
        }
        return this;
    }

    public PlaceMatBlock cardinalShape(AABB... boxes) {
        return cardinalShape(combineBoxes(boxes));
    }

    public PlaceMatBlock cardinalBoundingBox(VoxelShape... shapes) {
        return cardinalShape(shapes);
    }

    public PlaceMatBlock cardinalBoundingBox(AABB... boxes) {
        return cardinalShape(boxes);
    }

    public PlaceMatBlock adjustCardinalShape(VoxelShape... shapes) {
        return cardinalShape(shapes);
    }

    public PlaceMatBlock adjustCardinalShape(AABB... boxes) {
        return cardinalShape(boxes);
    }

    public PlaceMatBlock adjustCardinalBoundingBox(VoxelShape... shapes) {
        return cardinalShape(shapes);
    }

    public PlaceMatBlock adjustCardinalBoundingBox(AABB... boxes) {
        return cardinalShape(boxes);
    }

    public PlaceMatBlock cardinalShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            addShapeOverride(state -> state.hasProperty(PlaceMatCardinalBlock.FACING)
                    && state.getValue(PlaceMatCardinalBlock.FACING) == dir
                    && predicate.test(state), rotateShape(dir, combined));
        }
        return this;
    }

    public PlaceMatBlock cardinalShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, combineBoxes(boxes));
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalShape(Property<T> property, T value, VoxelShape... shapes) {
        return cardinalShape(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalShape(Property<T> property, T value, AABB... boxes) {
        return cardinalShape(property, value, combineBoxes(boxes));
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalShape(Property<T> property, Collection<T> values, VoxelShape... shapes) {
        return cardinalShape(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalShape(Property<T> property, Collection<T> values, AABB... boxes) {
        return cardinalShape(property, values, combineBoxes(boxes));
    }

    public PlaceMatBlock cardinalBoundingBox(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalShape(predicate, shapes);
    }

    public PlaceMatBlock cardinalBoundingBox(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalBoundingBox(Property<T> property, T value, VoxelShape... shapes) {
        return cardinalShape(property, value, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalBoundingBox(Property<T> property, T value, AABB... boxes) {
        return cardinalShape(property, value, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalBoundingBox(Property<T> property, Collection<T> values, VoxelShape... shapes) {
        return cardinalShape(property, values, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalBoundingBox(Property<T> property, Collection<T> values, AABB... boxes) {
        return cardinalShape(property, values, boxes);
    }

    public PlaceMatBlock adjustCardinalShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalShape(predicate, shapes);
    }

    public PlaceMatBlock adjustCardinalShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalShape(Property<T> property, T value, VoxelShape... shapes) {
        return cardinalShape(property, value, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalShape(Property<T> property, T value, AABB... boxes) {
        return cardinalShape(property, value, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalShape(Property<T> property, Collection<T> values, VoxelShape... shapes) {
        return cardinalShape(property, values, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalShape(Property<T> property, Collection<T> values, AABB... boxes) {
        return cardinalShape(property, values, boxes);
    }

    public PlaceMatBlock adjustCardinalBoundingBox(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalShape(predicate, shapes);
    }

    public PlaceMatBlock adjustCardinalBoundingBox(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalShape(predicate, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalBoundingBox(Property<T> property, T value, VoxelShape... shapes) {
        return cardinalShape(property, value, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalBoundingBox(Property<T> property, T value, AABB... boxes) {
        return cardinalShape(property, value, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalBoundingBox(Property<T> property, Collection<T> values, VoxelShape... shapes) {
        return cardinalShape(property, values, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalBoundingBox(Property<T> property, Collection<T> values, AABB... boxes) {
        return cardinalShape(property, values, boxes);
    }

    public PlaceMatBlock cardinalCollisionShape(VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            collisionShape(PlaceMatCardinalBlock.FACING, dir, rotateShape(dir, combined));
        }
        return this;
    }

    public PlaceMatBlock cardinalCollisionShape(AABB... boxes) {
        return cardinalCollisionShape(combineBoxes(boxes));
    }

    public PlaceMatBlock adjustCardinalCollisionShape(VoxelShape... shapes) {
        return cardinalCollisionShape(shapes);
    }

    public PlaceMatBlock adjustCardinalCollisionShape(AABB... boxes) {
        return cardinalCollisionShape(boxes);
    }

    public PlaceMatBlock cardinalCollisionShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        VoxelShape combined = combineShapes(shapes);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            addCollisionShapeOverride(state -> state.hasProperty(PlaceMatCardinalBlock.FACING)
                    && state.getValue(PlaceMatCardinalBlock.FACING) == dir
                    && predicate.test(state), rotateShape(dir, combined));
        }
        return this;
    }

    public PlaceMatBlock cardinalCollisionShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalCollisionShape(predicate, combineBoxes(boxes));
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalCollisionShape(Property<T> property, T value, VoxelShape... shapes) {
        return cardinalCollisionShape(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalCollisionShape(Property<T> property, T value, AABB... boxes) {
        return cardinalCollisionShape(property, value, combineBoxes(boxes));
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalCollisionShape(Property<T> property, Collection<T> values, VoxelShape... shapes) {
        return cardinalCollisionShape(state -> state.hasProperty(property) && values.contains(state.getValue(property)), shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock cardinalCollisionShape(Property<T> property, Collection<T> values, AABB... boxes) {
        return cardinalCollisionShape(property, values, combineBoxes(boxes));
    }

    public PlaceMatBlock adjustCardinalCollisionShape(Predicate<BlockState> predicate, VoxelShape... shapes) {
        return cardinalCollisionShape(predicate, shapes);
    }

    public PlaceMatBlock adjustCardinalCollisionShape(Predicate<BlockState> predicate, AABB... boxes) {
        return cardinalCollisionShape(predicate, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalCollisionShape(Property<T> property, T value, VoxelShape... shapes) {
        return cardinalCollisionShape(property, value, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalCollisionShape(Property<T> property, T value, AABB... boxes) {
        return cardinalCollisionShape(property, value, boxes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalCollisionShape(Property<T> property, Collection<T> values, VoxelShape... shapes) {
        return cardinalCollisionShape(property, values, shapes);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustCardinalCollisionShape(Property<T> property, Collection<T> values, AABB... boxes) {
        return cardinalCollisionShape(property, values, boxes);
    }

    /* ======================== Range ========================== */

    public PlaceMatBlock addRange(int index, PlacementRange range) {
        while (this.placementRanges.size() <= index) {
            this.placementRanges.add(null);
        }
        this.placementRanges.set(index, range);
        return this;
    }

    public PlaceMatBlock addRange(PlacementRange range) {
        return addRange(getNextRangeIndex(), range);
    }

    public PlaceMatBlock addRange(int index, Predicate<BlockState> predicate, PlacementRange range) {
        this.statePlacementRanges.add(new StatePlacementRange(index, predicate, range));
        return this;
    }

    public PlaceMatBlock addRange(Predicate<BlockState> predicate, PlacementRange range) {
        return addRange(getNextRangeIndex(), predicate, range);
    }

    public <T extends Comparable<T>> PlaceMatBlock addRange(int index, Property<T> property, T value, PlacementRange range) {
        return addRange(index, state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), range);
    }

    public <T extends Comparable<T>> PlaceMatBlock addRange(Property<T> property, T value, PlacementRange range) {
        return addRange(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), range);
    }

    public <T extends Comparable<T>> PlaceMatBlock addRange(int index, Property<T> property, Collection<T> values, PlacementRange range) {
        return addRange(index, state -> state.hasProperty(property) && values.contains(state.getValue(property)), range);
    }

    public <T extends Comparable<T>> PlaceMatBlock addRange(Property<T> property, Collection<T> values, PlacementRange range) {
        return addRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), range);
    }

    private int getNextRangeIndex() {
        int maxIndex = this.placementRanges.size() - 1;
        for (StatePlacementRange entry : this.statePlacementRanges) {
            if (entry.index() > maxIndex) {
                maxIndex = entry.index();
            }
        }
        return maxIndex + 1;
    }

    public record PlacementRange(
            AABB box,
            float maxHeight,
            boolean rollDisabled,
            boolean yawDisabled,
            boolean pitchDisabled,
            boolean elevationDisabled,
            boolean stackingEnabled,
            boolean collisionDisabled,
            @Nullable TagKey<Item> whitelistTag,
            boolean extractionDisabled,
            boolean insertionDisabled,
            int maxStackSize,
            @Nullable ResourceLocation foodTrait,
            boolean restricted,
            boolean disableLayFlat,
            boolean disableCustomModels,
            boolean snapToCenter,
            float scaleMultiplier,
            float defaultYaw,
            float defaultPitch,
            float defaultRoll,
            float defaultElevation) {

        public PlacementRange(
                VoxelShape shape,
                float maxHeight,
                boolean rollDisabled,
                boolean yawDisabled,
                boolean pitchDisabled,
                boolean elevationDisabled,
                boolean stackingEnabled,
                boolean collisionDisabled,
                @Nullable TagKey<Item> whitelistTag,
                boolean extractionDisabled,
                boolean insertionDisabled,
                int maxStackSize,
                @Nullable ResourceLocation foodTrait,
                boolean restricted,
                boolean disableLayFlat,
                boolean disableCustomModels,
                boolean snapToCenter,
                float scaleMultiplier,
                float defaultYaw,
                float defaultPitch,
                float defaultRoll,
                float defaultElevation) {
            this(shape.bounds(), maxHeight, rollDisabled, yawDisabled, pitchDisabled, elevationDisabled, stackingEnabled, collisionDisabled, whitelistTag, extractionDisabled, insertionDisabled, maxStackSize, foodTrait, restricted, disableLayFlat, disableCustomModels, snapToCenter, scaleMultiplier, defaultYaw, defaultPitch, defaultRoll, defaultElevation);
        }

        public PlacementRange(
                VoxelShape shape,
                boolean rollDisabled,
                boolean yawDisabled,
                boolean pitchDisabled,
                boolean elevationDisabled,
                boolean stackingEnabled,
                boolean collisionDisabled,
                @Nullable TagKey<Item> whitelistTag,
                boolean extractionDisabled,
                boolean insertionDisabled,
                int maxStackSize,
                @Nullable ResourceLocation foodTrait,
                boolean restricted,
                boolean disableLayFlat,
                boolean disableCustomModels,
                boolean snapToCenter,
                float scaleMultiplier,
                float defaultYaw,
                float defaultPitch,
                float defaultRoll,
                float defaultElevation) {
            this(shape.bounds(), (float) shape.bounds().maxY, rollDisabled, yawDisabled, pitchDisabled, elevationDisabled, stackingEnabled, collisionDisabled, whitelistTag, extractionDisabled, insertionDisabled, maxStackSize, foodTrait, restricted, disableLayFlat, disableCustomModels, snapToCenter, scaleMultiplier, defaultYaw, defaultPitch, defaultRoll, defaultElevation);
        }

        public PlacementRange(
                AABB box,
                boolean rollDisabled,
                boolean yawDisabled,
                boolean pitchDisabled,
                boolean elevationDisabled,
                boolean stackingEnabled,
                boolean collisionDisabled,
                @Nullable TagKey<Item> whitelistTag,
                boolean extractionDisabled,
                boolean insertionDisabled,
                int maxStackSize,
                @Nullable ResourceLocation foodTrait,
                boolean restricted,
                boolean disableLayFlat,
                boolean disableCustomModels,
                boolean snapToCenter,
                float scaleMultiplier,
                float defaultYaw,
                float defaultPitch,
                float defaultRoll,
                float defaultElevation) {
            this(box, (float) box.maxY, rollDisabled, yawDisabled, pitchDisabled, elevationDisabled, stackingEnabled, collisionDisabled, whitelistTag, extractionDisabled, insertionDisabled, maxStackSize, foodTrait, restricted, disableLayFlat, disableCustomModels, snapToCenter, scaleMultiplier, defaultYaw, defaultPitch, defaultRoll, defaultElevation);
        }

        public PlacementRange withBox(AABB newBox) {
            return new PlacementRange(newBox, (float) newBox.maxY, this.rollDisabled, this.yawDisabled, this.pitchDisabled, this.elevationDisabled, this.stackingEnabled, this.collisionDisabled, this.whitelistTag, this.extractionDisabled, this.insertionDisabled, this.maxStackSize, this.foodTrait, this.restricted, this.disableLayFlat, this.disableCustomModels, this.snapToCenter, this.scaleMultiplier, this.defaultYaw, this.defaultPitch, this.defaultRoll, this.defaultElevation);
        }

        public PlacementRange withBox(VoxelShape newShape) {
            return withBox(newShape.bounds());
        }

        public PlacementRange withMaxHeight(float newMaxHeight) {
            return new PlacementRange(this.box, newMaxHeight, this.rollDisabled, this.yawDisabled, this.pitchDisabled, this.elevationDisabled, this.stackingEnabled, this.collisionDisabled, this.whitelistTag, this.extractionDisabled, this.insertionDisabled, this.maxStackSize, this.foodTrait, this.restricted, this.disableLayFlat, this.disableCustomModels, this.snapToCenter, this.scaleMultiplier, this.defaultYaw, this.defaultPitch, this.defaultRoll, this.defaultElevation);
        }

        public PlacementRange withBoxAndHeight(AABB newBox, float newMaxHeight) {
            return new PlacementRange(newBox, newMaxHeight, this.rollDisabled, this.yawDisabled, this.pitchDisabled, this.elevationDisabled, this.stackingEnabled, this.collisionDisabled, this.whitelistTag, this.extractionDisabled, this.insertionDisabled, this.maxStackSize, this.foodTrait, this.restricted, this.disableLayFlat, this.disableCustomModels, this.snapToCenter, this.scaleMultiplier, this.defaultYaw, this.defaultPitch, this.defaultRoll, this.defaultElevation);
        }

        public PlacementRange withBoxAndHeight(VoxelShape newShape, float newMaxHeight) {
            return withBoxAndHeight(newShape.bounds(), newMaxHeight);
        }
    }

    @FunctionalInterface
    public interface RangeAdjuster {
        PlacementRange adjust(BlockState state, PlacementRange range, int index);
    }

    private final List<RangeAdjuster> rangeAdjusters = new ArrayList<>();

    public PlaceMatBlock addRangeAdjuster(RangeAdjuster adjuster) {
        this.rangeAdjusters.add(adjuster);
        return this;
    }

    public PlaceMatBlock adjustRange(RangeAdjuster adjuster) {
        return addRangeAdjuster(adjuster);
    }

    public PlaceMatBlock adjustRange(Predicate<BlockState> predicate, AABB newBox) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? range.withBox(newBox) : range);
    }

    public PlaceMatBlock adjustRange(Predicate<BlockState> predicate, VoxelShape shape) {
        return adjustRange(predicate, shape.bounds());
    }


    public PlaceMatBlock adjustRange(Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
    }

    public PlaceMatBlock adjustRange(Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
        return adjustRange(predicate, shape.bounds(), newMaxHeight);
    }


    public PlaceMatBlock adjustRange(int rangeIndex, Predicate<BlockState> predicate, AABB newBox) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? range.withBox(newBox) : range);
    }

    public PlaceMatBlock adjustRange(int rangeIndex, Predicate<BlockState> predicate, VoxelShape shape) {
        return adjustRange(rangeIndex, predicate, shape.bounds());
    }

    public PlaceMatBlock adjustRange(int rangeIndex, Predicate<BlockState> predicate, AABB newBox, float newMaxHeight) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? range.withBoxAndHeight(newBox, newMaxHeight) : range);
    }

    public PlaceMatBlock adjustRange(int rangeIndex, Predicate<BlockState> predicate, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, predicate, shape.bounds(), newMaxHeight);
    }

    public PlaceMatBlock adjustRange(Predicate<BlockState> predicate, Function<PlacementRange, PlacementRange> transformer) {
        return addRangeAdjuster((state, range, index) -> predicate.test(state) ? transformer.apply(range) : range);
    }

    public PlaceMatBlock adjustRange(int rangeIndex, Predicate<BlockState> predicate, Function<PlacementRange, PlacementRange> transformer) {
        return addRangeAdjuster((state, range, index) -> (index == rangeIndex && predicate.test(state)) ? transformer.apply(range) : range);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, T value, AABB newBox) {
        return adjustRange(state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, T value, VoxelShape shape) {
        return adjustRange(property, value, shape.bounds());
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, T value, AABB newBox, float newMaxHeight) {
        return adjustRange(state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox, newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, T value, VoxelShape shape, float newMaxHeight) {
        return adjustRange(property, value, shape.bounds(), newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, Collection<T> values, AABB newBox) {
        return adjustRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, Collection<T> values, VoxelShape shape) {
        return adjustRange(property, values, shape.bounds());
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, Collection<T> values, AABB newBox, float newMaxHeight) {
        return adjustRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox, newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, Collection<T> values, VoxelShape shape, float newMaxHeight) {
        return adjustRange(property, values, shape.bounds(), newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, Collection<T> values, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(state -> state.hasProperty(property) && values.contains(state.getValue(property)), transformer);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, T value, AABB newBox) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, T value, VoxelShape shape) {
        return adjustRange(rangeIndex, property, value, shape.bounds());
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, T value, AABB newBox, float newMaxHeight) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && state.getValue(property).equals(value), newBox, newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, T value, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, property, value, shape.bounds(), newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, Collection<T> values, AABB newBox) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, Collection<T> values, VoxelShape shape) {
        return adjustRange(rangeIndex, property, values, shape.bounds());
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, Collection<T> values, AABB newBox, float newMaxHeight) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && values.contains(state.getValue(property)), newBox, newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, Collection<T> values, VoxelShape shape, float newMaxHeight) {
        return adjustRange(rangeIndex, property, values, shape.bounds(), newMaxHeight);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, T value, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), transformer);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(int rangeIndex, Property<T> property, Collection<T> values, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(rangeIndex, state -> state.hasProperty(property) && values.contains(state.getValue(property)), transformer);
    }

    public <T extends Comparable<T>> PlaceMatBlock adjustRange(Property<T> property, T value, Function<PlacementRange, PlacementRange> transformer) {
        return adjustRange(state -> state.hasProperty(property) && Objects.equals(state.getValue(property), value), transformer);
    }

    public List<PlacementRange> getPlacementRanges(BlockState state) {
        if (this.placementRanges.isEmpty() && this.statePlacementRanges.isEmpty()) {
            return this.placementRanges;
        }
        if (this.statePlacementRanges.isEmpty() && this.rangeAdjusters.isEmpty()) {
            return this.placementRanges;
        }

        int maxIndex = this.placementRanges.size() - 1;
        for (StatePlacementRange entry : this.statePlacementRanges) {
            if (entry.index() > maxIndex) {
                maxIndex = entry.index();
            }
        }

        if (maxIndex < 0) {
            return List.of();
        }

        List<PlacementRange> result = new ArrayList<>(maxIndex + 1);
        for (int i = 0; i <= maxIndex; i++) {
            PlacementRange range = null;
            for (int j = this.statePlacementRanges.size() - 1; j >= 0; j--) {
                StatePlacementRange entry = this.statePlacementRanges.get(j);
                if (entry.index() == i && (entry.predicate() == null || entry.predicate().test(state))) {
                    range = entry.range();
                    break;
                }
            }
            if (range == null && i < this.placementRanges.size()) {
                range = this.placementRanges.get(i);
            }
            if (range != null) {
                PlacementRange adjusted = getAdjustedPlacementRange(state, range, i);
                result.add(adjusted);
            }
        }
        return result;
    }

    public PlacementRange getAdjustedPlacementRange(BlockState state, PlacementRange range) {
        return range;
    }

    public PlacementRange getAdjustedPlacementRange(BlockState state, PlacementRange range, int index) {
        PlacementRange current = getAdjustedPlacementRange(state, range);
        for (RangeAdjuster adjuster : this.rangeAdjusters) {
            current = adjuster.adjust(state, current, index);
        }
        return current;
    }

    public void addPlacementRanges(BlockState state, Consumer<PlacementRange> consumer) {
        for (PlacementRange range : getPlacementRanges(state)) {
            if (range != null) {
                consumer.accept(range);
            }
        }
    }

    @Nullable
    public PlacementRange getTargetedPlacementRange(BlockState state, Vec3 relativeHitVec) {
        PlacementRange best = null;
        double minDistance = Double.MAX_VALUE;

        Vec3 localHitVec = getLocalHitVec(state, relativeHitVec);

        for (PlacementRange range : getPlacementRanges(state)) {
            if (range == null) {
                continue;
            }
            if (localHitVec.x >= range.box.minX && localHitVec.x <= range.box.maxX &&
                    localHitVec.z >= range.box.minZ && localHitVec.z <= range.box.maxZ) {
                if (localHitVec.y >= range.box.minY && localHitVec.y <= range.box.maxY) {
                    return range;
                }
                double centerBoxY = (range.box.minY + range.box.maxY) / 2.0;
                double distY = Math.abs(localHitVec.y - centerBoxY);
                if (distY < minDistance) {
                    minDistance = distY;
                    best = range;
                }
            }
        }

        if (best != null) {
            return best;
        }

        for (PlacementRange range : getPlacementRanges(state)) {
            if (range == null) {
                continue;
            }
            double dist = getDistanceToBoxSqr(localHitVec, range.box);
            if (dist < minDistance) {
                minDistance = dist;
                best = range;
            }
        }

        return best;
    }

    // Okay, this sucks. But otherwise snapping to restricted zones feels very clunky.
    private static double getDistanceToBoxSqr(Vec3 point, AABB box) {
        double dx = point.x < box.minX ? box.minX - point.x : (point.x > box.maxX ? point.x - box.maxX : 0);
        double dy = point.y < box.minY ? box.minY - point.y : (point.y > box.maxY ? point.y - box.maxY : 0);
        double dz = point.z < box.minZ ? box.minZ - point.z : (point.z > box.maxZ ? point.z - box.maxZ : 0);
        return dx * dx + dy * dy + dz * dz;
    }

    public static Vec3 getLocalHitVec(BlockState state, Vec3 relativeHitVec) {
        if (!state.hasProperty(PlaceMatCardinalBlock.FACING)) {
            return relativeHitVec;
        }
        if (state.getBlock() instanceof PlaceMatBlock pmb && !pmb.isRotateZones()) {
            return relativeHitVec;
        }
        Direction facing = state.getValue(PlaceMatCardinalBlock.FACING);
        return switch (facing) {
            case SOUTH -> new Vec3(1 - relativeHitVec.x, relativeHitVec.y, 1 - relativeHitVec.z);
            case EAST -> new Vec3(relativeHitVec.z, relativeHitVec.y, 1 - relativeHitVec.x);
            case WEST -> new Vec3(1 - relativeHitVec.z, relativeHitVec.y, relativeHitVec.x);
            default -> relativeHitVec;
        };
    }

    public static Vec3 getWorldHitVec(BlockState state, Vec3 localHitVec) {
        if (!state.hasProperty(PlaceMatCardinalBlock.FACING)) {
            return localHitVec;
        }
        if (state.getBlock() instanceof PlaceMatBlock pmb && !pmb.isRotateZones()) {
            return localHitVec;
        }
        Direction facing = state.getValue(PlaceMatCardinalBlock.FACING);
        return switch (facing) {
            case SOUTH -> new Vec3(1 - localHitVec.x, localHitVec.y, 1 - localHitVec.z);
            case WEST -> new Vec3(localHitVec.z, localHitVec.y, 1 - localHitVec.x);
            case EAST -> new Vec3(1 - localHitVec.z, localHitVec.y, localHitVec.x);
            default -> localHitVec;
        };
    }

    public static Vec3 rotateDirection(Direction facing, Vec3 dir) {
        return switch (facing) {
            case SOUTH -> new Vec3(-dir.x, dir.y, -dir.z);
            case WEST -> new Vec3(dir.z, dir.y, -dir.x);
            case EAST -> new Vec3(-dir.z, dir.y, dir.x);
            default -> dir;
        };
    }

    public static Vec3 rotateDirectionInverse(Direction facing, Vec3 dir) {
        return switch (facing) {
            case SOUTH -> new Vec3(-dir.x, dir.y, -dir.z);
            case WEST -> new Vec3(-dir.z, dir.y, dir.x);
            case EAST -> new Vec3(dir.z, dir.y, -dir.x);
            default -> dir;
        };
    }

    public static void addPlacementRangesStatic(BlockState state, Consumer<PlacementRange> consumer) {
        if (state.getBlock() instanceof PlaceMatBlock fpb) {
            fpb.addPlacementRanges(state, consumer);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        boolean currentState = state.getValue(LOCKED);

        if (be instanceof PlaceMatBlockEntity pmbe && !pmbe.getBlockState().getValue(LOCKED).equals(true)) {
            InteractionResult result = PlaceMatInteractions.handleInteraction(pmbe, player, hand, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty() && !held.is(PlaceMatTags.Items.PLACE_MAT_BLACKLIST) && !state.getValue(LOCKED).equals(true)) {
            Vec3 location = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            PlacementRange targetedRange = getTargetedPlacementRange(state, location);
            if (targetedRange != null) {
                if (targetedRange.whitelistTag() == null || held.is(targetedRange.whitelistTag())) {
                    return InteractionResult.SUCCESS;
                }
            } else if (getPlacementRanges(state).isEmpty()) {
                return InteractionResult.SUCCESS;
            }
        }

        if (held.is(PlaceMatTags.Items.KEY) && level instanceof ServerLevel serverLevel) {
            boolean newState = !currentState;
            state = state.setValue(LOCKED, newState);

            if (be instanceof PlaceMatBlockEntity pmbe) {
                if (newState) {
                    pmbe.setLockedBy(player.getUUID());
                } else if (player.getUUID() == pmbe.getLockedBy()) {
                    pmbe.setLockedBy(null);
                }
                pmbe.setChanged();
            }

            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 2f, 0.5f);
            serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
            level.setBlockAndUpdate(pos, state);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return PlaceMatBlockEntities.PLACE_MAT.get().create(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOCKED);
    }

    // Method for pick-block item cloning when looking at placed items.
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult blockHit && level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            PlaceMatBlockEntity.PlacedItem targeted = pmbe.getTargetedItem(eyePos, lookVec, pos);
            if (targeted != null) {
                return targeted.stack.copy();
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlaceMatBlockEntity pmbe) {
                pmbe.dropItems();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        } else if (!state.equals(newState)) {
            boolean onlyLockChanged = state.hasProperty(LOCKED) && newState.hasProperty(LOCKED)
                    && state.setValue(LOCKED, false).equals(newState.setValue(LOCKED, false));
            if (!onlyLockChanged) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof PlaceMatBlockEntity pmbe) {
                    pmbe.dropItems();
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PlaceMatBlockEntity pmbe) {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            if (pmbe.getTargetedItem(eyePos, lookVec, pos) != null || pmbe.getBlockState().getValue(LOCKED).equals(true)) {
                return 0.0f;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        for (int i = this.shapeOverrides.size() - 1; i >= 0; i--) {
            StateShape override = this.shapeOverrides.get(i);
            if (override.predicate().test(state)) {
                return override.shape();
            }
        }
        return this.shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        for (int i = this.collisionShapeOverrides.size() - 1; i >= 0; i--) {
            StateShape override = this.collisionShapeOverrides.get(i);
            if (override.predicate().test(state)) {
                return override.shape();
            }
        }
        return this.collisionShape;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PlaceMatBlockEntities.PLACE_MAT.get() ? (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof PlaceMatBlockEntity be) {
                be.tick(level1, pos, state1);
            }
        } : null;
    }
}

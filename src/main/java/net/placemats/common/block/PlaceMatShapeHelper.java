package net.placemats.common.block;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Utility functions for geometric and voxel shape operations, rotations, and vector transformations.
 */
public final class PlaceMatShapeHelper {

    private PlaceMatShapeHelper() {}

    public static VoxelShape box(float x1, float y1, float z1, float x2, float y2, float z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static VoxelShape combineShapes(VoxelShape... shapes) {
        VoxelShape combined = Shapes.empty();
        for (VoxelShape shape : shapes) {
            combined = Shapes.or(combined, shape);
        }
        return combined;
    }

    public static VoxelShape combineBoxes(AABB... boxes) {
        VoxelShape combined = Shapes.empty();
        for (AABB box : boxes) {
            combined = Shapes.or(combined, Shapes.create(box));
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
        if (direction == Direction.NORTH) {
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

    public static Vec3 getLocalHitVec(BlockState state, Vec3 relativeHitVec) {
        if (!state.hasProperty(PlaceMatCardinalBlock.FACING)) {
            return relativeHitVec;
        }
        if (state.getBlock() instanceof IPlacementRangeProvider provider && !provider.isRotateZones()) {
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
        if (state.getBlock() instanceof IPlacementRangeProvider provider && !provider.isRotateZones()) {
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
}

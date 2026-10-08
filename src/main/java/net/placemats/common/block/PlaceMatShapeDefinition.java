package net.placemats.common.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import lombok.Getter;
import lombok.Setter;
import net.placemats.common.block.IPlaceMatShapeProvider.StateShape;

/**
 * Definition holding voxel shapes and collision shapes with overrides.
 */
@Getter
@Setter
public class PlaceMatShapeDefinition {

    private VoxelShape shape = Shapes.block();
    private VoxelShape collisionShape = Shapes.block();
    private final List<StateShape> shapeOverrides = new ArrayList<>();
    private final List<StateShape> collisionShapeOverrides = new ArrayList<>();

    public VoxelShape getShape(BlockState state) {
        if (this.shapeOverrides.isEmpty()) {
            return this.shape;
        }
        for (int i = this.shapeOverrides.size() - 1; i >= 0; i--) {
            StateShape override = this.shapeOverrides.get(i);
            if (override.predicate().test(state)) {
                return override.shape();
            }
        }
        return this.shape;
    }

    public VoxelShape getCollisionShape(BlockState state) {
        if (this.collisionShapeOverrides.isEmpty()) {
            return this.collisionShape;
        }
        for (int i = this.collisionShapeOverrides.size() - 1; i >= 0; i--) {
            StateShape override = this.collisionShapeOverrides.get(i);
            if (override.predicate().test(state)) {
                return override.shape();
            }
        }
        return this.collisionShape;
    }
}

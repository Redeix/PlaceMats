package net.placemats.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import lombok.Getter;
import lombok.Setter;
import net.placemats.common.block.IPlacementRangeProvider.RangeAdjuster;
import net.placemats.common.block.IPlacementRangeProvider.StatePlacementRange;

/**
 * Definition holding placement ranges, transformations, state ranges, and placement configurations.
 */
@Getter
@Setter
public class PlaceMatPlacementDefinition {

    private int containerSize = 12;
    private final List<PlacementRange> placementRanges = new ArrayList<>();
    private final List<StatePlacementRange> statePlacementRanges = new ArrayList<>();
    private final List<RangeAdjuster> rangeAdjusters = new ArrayList<>();

    private boolean extractionDisabled = false;
    private boolean insertionDisabled = false;
    private int maxStackSize = 64;
    private boolean disableLayFlat = false;
    private boolean disableCustomModels = false;
    private boolean rotateZones = true;
    private float scaleMultiplier = 1.0f;
    private float defaultYaw = 0;
    private float defaultPitch = 0;
    private float defaultRoll = 0;
    private float defaultElevation = 0;
    @Nullable
    private ResourceLocation foodTrait = null;

    public int getNextRangeIndex() {
        int maxIndex = this.placementRanges.size() - 1;
        for (StatePlacementRange entry : this.statePlacementRanges) {
            if (entry.index() > maxIndex) {
                maxIndex = entry.index();
            }
        }
        return maxIndex + 1;
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
            consumer.accept(range);
        }
    }

    @Nullable
    public PlacementRange getTargetedPlacementRange(BlockState state, Vec3 relativeHitVec) {
        PlacementRange best = null;
        double minDistance = Double.MAX_VALUE;

        Vec3 localHitVec = PlaceMatShapeHelper.getLocalHitVec(state, relativeHitVec);

        for (PlacementRange range : getPlacementRanges(state)) {
            AABB box = range.box();
            if (localHitVec.x >= box.minX && localHitVec.x <= box.maxX &&
                    localHitVec.z >= box.minZ && localHitVec.z <= box.maxZ) {
                if (localHitVec.y >= box.minY && localHitVec.y <= box.maxY) {
                    return range;
                }
                double centerBoxY = (box.minY + box.maxY) / 2.0;
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
            double dist = getDistanceToBoxSqr(localHitVec, range.box());
            if (dist < minDistance) {
                minDistance = dist;
                best = range;
            }
        }

        return best;
    }

    public static double getDistanceToBoxSqr(Vec3 point, AABB box) {
        double dx = point.x < box.minX ? box.minX - point.x : (point.x > box.maxX ? point.x - box.maxX : 0);
        double dy = point.y < box.minY ? box.minY - point.y : (point.y > box.maxY ? point.y - box.maxY : 0);
        double dz = point.z < box.minZ ? box.minZ - point.z : (point.z > box.maxZ ? point.z - box.maxZ : 0);
        return dx * dx + dy * dy + dz * dz;
    }
}

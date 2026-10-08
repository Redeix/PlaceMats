package net.placemats.common.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Definition record describing a placement zone.
 * @param box AABB or VoxelShape box size for placement range.
 * @param maxHeight Maximum height for the individual placement range. Otherwise, the default height is the collision box height. Not zone height.
 * @param rollDisabled Prevents placed items from rotating around the X-axis.
 * @param yawDisabled Prevents placed items from rotating around the Z-axis.
 * @param elevationDisabled Prevents placed items from being raised and lowered.
 * @param stackingEnabled Allows placed items to stack on top of one another.
 * @param collisionDisabled Prevents placed items from entering each others hitbox.
 * @param whitelistTag Whitelist tag for items that can be placed in this zone.
 * @param extractionDisabled Prevents placed items from being extracted from this zone.
 * @param insertionDisabled Prevents placed items from being inserted into this zone.
 * @param maxStackSize Maximum stack size for placed items.
 * @param foodTrait Food trait given to placed items. For {@link net.placemats.compat.tfc.TFCCompat}
 * @param restricted Converts placement zone into the restricted type, which prevents transformations.
 * @param disableLayFlat Prevents default item models from laying flat.
 * @param disableCustomModels Disables custom item models from rendering.
 * @param snapToCenter Snap placed items to the center of the placement range.
 * @param scaleMultiplier Scale multiplier for placed items.
 * @param defaultYaw Default yaw rotation for placed items.
 * @param defaultPitch Default pitch rotation for placed items.
 * @param defaultRoll Default roll rotation for placed items.
 * @param defaultElevation Default elevation for placed items.
 */
@SuppressWarnings("unused")
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

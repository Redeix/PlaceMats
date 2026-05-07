package net.placemats.compat.kjs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import dev.latvian.mods.kubejs.typings.Info;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.compat.tfc.TFCCompat;

@SuppressWarnings("unused")
public class PlaceMatBlockBuilder extends PlaceMatBlockBuilders {

    private int containerSize = 12;
    private boolean disableExtraction = false;
    private boolean disableInsertion = false;
    private int maxStackSize = 64;
    private boolean cardinal = false;
    private boolean disableLayFlat = false;
    private boolean disableCustomModels = false;
    private float scaleMultiplier = 1.0f;
    private float defaultYaw = 0;
    private float defaultPitch = 0;
    private float defaultRoll = 0;
    private float defaultElevation = 0;
    private ResourceLocation foodTrait = null;
    private final List<PlacementRangeBuilder> ranges = new ArrayList<>();

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

    @Info("Adds a placement range bounding box with custom parameters.")
    public PlaceMatBlockBuilder addPlacementRange(Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        consumer.accept(builder);
        this.ranges.add(builder);
        return this;
    }

    @Info("Adds a restricted placement range bounding box with custom parameters. These boxes only hold one itemStack that cant be moved.")
    public PlaceMatBlockBuilder addRestrictedPlacementRange(Consumer<PlacementRangeBuilder> consumer) {
        PlacementRangeBuilder builder = new PlacementRangeBuilder();
        builder.restricted = true;
        consumer.accept(builder);
        this.ranges.add(builder);
        return this;
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
        block.scaleMultiplier(scaleMultiplier);
        block.defaultRotation(defaultYaw, defaultPitch, defaultRoll);
        block.defaultElevation(defaultElevation);
        for (PlacementRangeBuilder rangeBuilder : ranges) {
            block.addRange(rangeBuilder.build());
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

        @Info("Sets the placement bounds. (x1, y1, z1, x2, y2, z2)")
        public PlacementRangeBuilder placementBounds(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.box = new AABB(x1 / 16D, y1 / 16D, z1 / 16D, x2 / 16D, y2 / 16D, z2 / 16D);
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

package net.placemats.compat.tfc;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.placemats.common.block.PlaceMatAttachedStairsBlock;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlaceMatStairsBlock;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class TFCPlaceMatBlock extends PlaceMatBlock implements IForgeBlockExtension {
    private final ExtendedProperties extendedProperties;

    public TFCPlaceMatBlock(Block.Properties properties) {
        this(ExtendedProperties.of(properties));
    }

    public TFCPlaceMatBlock(ExtendedProperties properties) {
        super(properties.properties());
        this.extendedProperties = properties;
    }

    @Override
    public @NotNull ExtendedProperties getExtendedProperties() {
        return extendedProperties;
    }

    public static class Cardinal extends PlaceMatCardinalBlock implements IForgeBlockExtension {
        private final ExtendedProperties extendedProperties;

        public Cardinal(Block.Properties properties) {
            this(ExtendedProperties.of(properties));
        }

        public Cardinal(ExtendedProperties properties) {
            super(properties.properties());
            this.extendedProperties = properties;
        }

        @Override
        public @NotNull ExtendedProperties getExtendedProperties() {
            return extendedProperties;
        }
    }

    public static class Stairs extends PlaceMatStairsBlock implements IForgeBlockExtension {
        private final ExtendedProperties extendedProperties;

        public Stairs(Block.Properties properties) {
            this(ExtendedProperties.of(properties));
        }

        public Stairs(ExtendedProperties properties) {
            super(properties.properties());
            this.extendedProperties = properties;
        }

        @Override
        public @NotNull ExtendedProperties getExtendedProperties() {
            return extendedProperties;
        }
    }

    public static class AttachedStairs extends PlaceMatAttachedStairsBlock implements IForgeBlockExtension {
        private final ExtendedProperties extendedProperties;

        public AttachedStairs(Block.Properties properties) {
            this(ExtendedProperties.of(properties));
        }

        public AttachedStairs(ExtendedProperties properties) {
            super(properties.properties());
            this.extendedProperties = properties;
        }

        @Override
        public @NotNull ExtendedProperties getExtendedProperties() {
            return extendedProperties;
        }
    }
}

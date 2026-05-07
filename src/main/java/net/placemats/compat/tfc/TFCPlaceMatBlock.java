package net.placemats.compat.tfc;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.placemats.common.block.PlaceMatBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

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
    public ExtendedProperties getExtendedProperties() {
        return extendedProperties;
    }

    public static class Cardinal extends PlaceMatBlock.Cardinal implements IForgeBlockExtension {
        private final ExtendedProperties extendedProperties;

        public Cardinal(Block.Properties properties) {
            this(ExtendedProperties.of(properties));
        }

        public Cardinal(ExtendedProperties properties) {
            super(properties.properties());
            this.extendedProperties = properties;
        }

        @Override
        public ExtendedProperties getExtendedProperties() {
            return extendedProperties;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }
}

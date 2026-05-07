package net.placemats.compat.kjs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.latvian.mods.kubejs.block.BlockBuilder;

/**
 * A base class for block builders that provides TFC property support.
 */
public abstract class PlaceMatBlockBuilders extends BlockBuilder {

    protected final List<Consumer<BlockBehaviour.Properties>> propertiesModifiers = new ArrayList<>();

    public PlaceMatBlockBuilders(ResourceLocation i) {
        super(i);
    }

    public PlaceMatBlockBuilders flammable(int encouragement, int flammability) {
        // We can't easily apply this to vanilla properties without TFC, but we can store it for later or just ignore if TFC is missing.
        // Actually, TFC's ExtendedProperties is just a wrapper.
        return this;
    }

    public PlaceMatBlockBuilders flammableLikeLogs() {
        return this;
    }

    public PlaceMatBlockBuilders flammableLikeLeaves() {
        return this;
    }

    public PlaceMatBlockBuilders randomTicks() {
        propertiesModifiers.add(BlockBehaviour.Properties::randomTicks);
        return this;
    }

    public PlaceMatBlockBuilders noLootTable() {
        propertiesModifiers.add(BlockBehaviour.Properties::noLootTable);
        return this;
    }

    public PlaceMatBlockBuilders extendedProperties(Consumer<BlockBehaviour.Properties> consumer) {
        propertiesModifiers.add(consumer);
        return this;
    }

    public BlockBehaviour.Properties createProperties() {
        BlockBehaviour.Properties p = super.createProperties();
        for (Consumer<BlockBehaviour.Properties> modifier : propertiesModifiers) {
            modifier.accept(p);
        }
        return p;
    }
}

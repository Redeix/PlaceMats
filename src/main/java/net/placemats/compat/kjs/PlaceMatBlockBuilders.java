package net.placemats.compat.kjs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.minecraft.resources.ResourceLocation;

import dev.latvian.mods.kubejs.block.BlockBuilder;

/**
 * A base class for block builders that provides TFC property support.
 */
public abstract class PlaceMatBlockBuilders extends BlockBuilder {

    protected final List<Consumer<ExtendedProperties>> extendedPropertiesModifiers = new ArrayList<>();

    public PlaceMatBlockBuilders(ResourceLocation i) {
        super(i);
    }

    public PlaceMatBlockBuilders flammable(int encouragement, int flammability) {
        extendedPropertiesModifiers.add(p -> p.flammable(encouragement, flammability));
        return this;
    }

    public PlaceMatBlockBuilders flammableLikeLogs() {
        extendedPropertiesModifiers.add(ExtendedProperties::flammableLikeLogs);
        return this;
    }

    public PlaceMatBlockBuilders flammableLikeLeaves() {
        extendedPropertiesModifiers.add(ExtendedProperties::flammableLikeLeaves);
        return this;
    }

    public PlaceMatBlockBuilders randomTicks() {
        extendedPropertiesModifiers.add(ExtendedProperties::randomTicks);
        return this;
    }

    public PlaceMatBlockBuilders noLootTable() {
        extendedPropertiesModifiers.add(ExtendedProperties::noLootTable);
        return this;
    }

    public PlaceMatBlockBuilders extendedProperties(Consumer<ExtendedProperties> consumer) {
        extendedPropertiesModifiers.add(consumer);
        return this;
    }

    public ExtendedProperties createExtendedProperties() {
        ExtendedProperties ep = ExtendedProperties.of(createProperties());
        for (Consumer<ExtendedProperties> modifier : extendedPropertiesModifiers) {
            modifier.accept(ep);
        }
        return ep;
    }
}

package net.placemats.compat.kjs.events;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;

import net.placemats.common.data.resource.DefinitionManager;
import net.placemats.compat.kjs.PlaceMatDefinitionBuilder;

public class RegisterPlaceMatDefinitionsEventJS extends EventJS {
    private final List<PlaceMatDefinitionBuilder> builders = new ArrayList<>();

    public PlaceMatDefinitionBuilder register(String id) {
        PlaceMatDefinitionBuilder builder = new PlaceMatDefinitionBuilder(ResourceLocation.parse(id));
        builders.add(builder);
        return builder;
    }

    public void generateDataJsons(DataJsonGenerator generator) {
        for (PlaceMatDefinitionBuilder builder : builders) {
            builder.generateDataJsons(generator);
        }
    }

    public void generateAssetJsons(AssetJsonGenerator generator) {
        for (PlaceMatDefinitionBuilder builder : builders) {
            builder.generateAssetJsons(generator);
        }
    }

    public void apply(java.util.Map<net.minecraft.world.item.Item, DefinitionManager.PlaceMatDefinition> definitions,
            java.util.Map<net.minecraft.tags.TagKey<net.minecraft.world.item.Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions) {
        for (PlaceMatDefinitionBuilder builder : builders) {
            builder.apply(definitions, tagDefinitions);
        }
    }
}

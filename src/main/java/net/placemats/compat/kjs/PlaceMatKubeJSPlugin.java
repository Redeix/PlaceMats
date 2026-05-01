package net.placemats.compat.kjs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.BindingsEvent;

import net.placemats.PlaceMatMain;
import net.placemats.compat.kjs.events.RegisterPlaceMatDefinitionsEventJS;
import net.placemats.compat.kjs.events.PlaceMatKJSServerEvents;
import net.placemats.compat.kjs.events.PlaceMatKJSStartupEvents;

public final class PlaceMatKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void initStartup() {
        super.initStartup();
    }

    @Override
    public void init() {
        RegistryInfo.BLOCK.addType("tfg:place_mat", PlaceMatBlockBuilder.class, PlaceMatBlockBuilder::new);
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.register(PlaceMatMain.id("place_mat"), PlaceMatRecipeSchema.SCHEMA);
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        super.registerBindings(event);
    }

    @Override
    public void generateDataJsons(DataJsonGenerator generator) {
        super.generateDataJsons(generator);
        var event = new RegisterPlaceMatDefinitionsEventJS();
        PlaceMatKJSStartupEvents.PLACE_MAT_DEFINITIONS.post(event);
        event.generateDataJsons(generator);
    }

    @Override
    public void generateAssetJsons(AssetJsonGenerator generator) {
        super.generateAssetJsons(generator);
        var event = new RegisterPlaceMatDefinitionsEventJS();
        PlaceMatKJSStartupEvents.PLACE_MAT_DEFINITIONS.post(event);
        event.generateAssetJsons(generator);
    }

    @Override
    public void registerEvents() {
        super.registerEvents();
        PlaceMatKJSStartupEvents.GROUP.register();
        PlaceMatKJSServerEvents.GROUP.register();
    }

    @Override
    public void onServerReload() {
    }
}

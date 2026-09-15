package net.placemats.common.datagen.definitions;

import static net.placemats.common.datagen.definitions.DefinitionHelpers.*;

public enum MinecraftDefinitions implements IPlaceMatDefinition {
    APPLE(new Definition("minecraft:apple").size(0.19f, 0.19f).scale(1f).flat(false)
        .model(Definition.parentModel("place_mats:render/minecraft/food/apple", "place_mats:render/3x3_food", new String[]{"0", "place_mats:item/render/tfc/food/red_apple"})));

    private final Definition definition;

    MinecraftDefinitions(Definition definition) {
        this.definition = definition;
    }

    @Override
    public Definition getDefinition() {
        return definition;
    }
}

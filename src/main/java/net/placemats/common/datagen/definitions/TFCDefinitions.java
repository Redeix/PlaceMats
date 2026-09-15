package net.placemats.common.datagen.definitions;

public enum TFCDefinitions implements IPlaceMatDefinition {
    RED_APPLE(new Definition("tfc:food/red_apple").size(0.19f, 0.19f).scale(1f).flat(false)
            .model(Definition.parentModel("place_mats:render/tfc/food/red_apple", "place_mats:render/3x3_food", new String[]{"0", "place_mats:item/render/tfc/food/red_apple"}))
            .modelRotten(Definition.parentModel("place_mats:render/tfc/food/red_apple_rotten", "place_mats:render/3x3_food", new String[]{"0", "place_mats:item/render/tfc/food/red_apple_rotten"}))),
    GREEN_APPLE(new Definition("tfc:food/green_apple").size(0.19f, 0.19f).scale(1f).flat(false)
            .model(Definition.parentModel("place_mats:render/tfc/food/green_apple", "place_mats:render/3x3_food", new String[]{"0", "place_mats:item/render/tfc/food/green_apple"}))
        .modelRotten(Definition.parentModel("place_mats:render/tfc/food/green_apple_rotten", "place_mats:render/3x3_food", new String[]{"0", "place_mats:item/render/tfc/food/green_apple_rotten"})));

    private final Definition definition;

    TFCDefinitions(Definition definition) {
        this.definition = definition;
    }

    @Override
    public Definition getDefinition() {
        return definition;
    }
}

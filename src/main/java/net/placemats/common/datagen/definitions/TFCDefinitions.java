package net.placemats.common.datagen.definitions;

public enum TFCDefinitions implements IPlaceMatDefinition {
    RED_APPLE(new Definition("tfc:food/red_apple").size(0.19f, 0.19f).scale(1f).flat(false).model("place_mats:render/tfc/food/red_apple").modelRotten("place_mats:render/tfc/food/red_apple_rotten"));

    private final Definition definition;

    TFCDefinitions(Definition definition) {
        this.definition = definition;
    }

    @Override
    public Definition getDefinition() {
        return definition;
    }
}

package net.placemats.common.datagen.definitions;

import static net.placemats.common.datagen.definitions.DefinitionHelpers.*;

public enum TFCDefinitions implements IPlaceMatDefinition {
    ONION(tfcFood3x3("tfc:food/onion","onion")),
    ORANGE(tfcFood3x3("tfc:food/orange","orange")),
    PEACH(tfcFood3x3("tfc:food/peach","peach")),
    PLUM(tfcFood3x3("tfc:food/plum","plum")),
    RED_BELL_PEPPER(tfcFood3x3("tfc:food/red_bell_pepper","red_bell_pepper")),
    GREEN_BELL_PEPPER(tfcFood3x3("tfc:food/green_bell_pepper","green_bell_pepper")),
    YELLOW_BELL_PEPPER(tfcFood3x3("tfc:food/yellow_bell_pepper","yellow_bell_pepper")),
    SQUASH(tfcFood3x3("tfc:food/squash","squash")),
    TOMATO(tfcFood3x3("tfc:food/tomato","tomato")),
    RED_APPLE(tfcFood3x3("tfc:food/red_apple","red_apple")),
    GREEN_APPLE(tfcFood3x3("tfc:food/green_apple","green_apple"));

    private final Definition definition;

    TFCDefinitions(Definition definition) {
        this.definition = definition;
    }

    @Override
    public Definition getDefinition() {
        return definition;
    }
}

package net.placemats.compat.kjs.events;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public class PlaceMatKJSStartupEvents {
    public static final EventGroup GROUP = EventGroup.of("PlaceMatsStartupEvents");

    public static final EventHandler PLACE_MAT_DEFINITIONS = GROUP.startup("placeMatDefinitions", () -> RegisterPlaceMatDefinitionsEventJS.class);
}

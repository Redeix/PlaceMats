package net.placemats.compat.kjs.events;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public class PlaceMatKJSServerEvents {
    public static final EventGroup GROUP = EventGroup.of("PlaceMatsServerEvents");

    public static final EventHandler PLACEMAT_INTERACTION = GROUP.server("placeMatInteraction", () -> PlaceMatInteractionEventJS.class);
}

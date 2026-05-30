package net.placemats.compat.everycompat;

import net.mehvahdjukaar.every_compat.api.EveryCompatAPI;
import net.placemats.PlaceMatMain;

public class EveryCompatCompatImpl implements EveryCompatCompat {
    @Override
    public void init() {
        EveryCompatAPI.registerModule(new PlaceMatEveryCompatModule(PlaceMatMain.MOD_ID));
    }
}

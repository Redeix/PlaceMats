package net.placemats.compat.everycompat;

import net.mehvahdjukaar.every_compat.api.EveryCompatAPI;
import net.placemats.PlaceMatMain;
import net.placemats.compat.ModCompat;

public class EveryCompatCompatImpl implements EveryCompatCompat {
    @Override
    public void init() {
        if (ModCompat.EVERY_COMPAT_LOADED) {
            EveryCompatAPI.registerModule(new PlaceMatEveryCompatModule(PlaceMatMain.MOD_ID));
        }
    }
}

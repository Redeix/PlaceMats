package net.placemats.compat.everycompat;

import net.placemats.compat.ModCompat;

public interface EveryCompatCompat {
    EveryCompatCompat INSTANCE = ModCompat.EVERY_COMPAT_LOADED ? new EveryCompatCompatImpl() : new NoEveryCompatCompat();

    void init();
}

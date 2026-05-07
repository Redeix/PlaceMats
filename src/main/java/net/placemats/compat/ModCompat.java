package net.placemats.compat;

import net.minecraftforge.fml.ModList;

public class ModCompat {
    public static final boolean TFC_LOADED = ModList.get().isLoaded("tfc");
    public static final boolean FIRMALIFE_LOADED = ModList.get().isLoaded("firmalife");
    public static final boolean KJS_LOADED = ModList.get().isLoaded("kubejs");
    public static final boolean JADE_LOADED = ModList.get().isLoaded("jade");
}

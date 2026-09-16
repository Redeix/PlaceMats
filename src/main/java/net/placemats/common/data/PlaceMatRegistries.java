package net.placemats.common.data;

import com.tterrag.registrate.providers.ProviderType;

public class PlaceMatRegistries {
    public static final PlaceMatRegistrate REGISTRATE = PlaceMatRegistrate.create("place_mats");

    static {
        PlaceMatTags.addExistingTags(REGISTRATE);
        REGISTRATE.addDataGenerator(ProviderType.LANG, prov -> {
            prov.add("block_type.pm.storage_rack", "%s Storage Rack");
            prov.add("place_mats.creative_tab.place_mats", "Place Mats");
            prov.add("place_mats.tooltip.placemat.placing", "Hold an item and §3RMB§r to display.");
            prov.add("place_mats.tooltip.placemat.hold_alt_for_nutrition_info", "Hold (Alt) for Detailed Info");
            prov.add("place_mats.tooltip.placemat.pitch", "§3Shift+Ctrl+Scroll§r for Tilt. ");
            prov.add("place_mats.tooltip.placemat.yaw", "§3Shift+Scroll§r for Yaw. ");
            prov.add("place_mats.tooltip.placemat.interacting", "§3RMB§r to remove. §3Shift+RMB§r to remove stack. §3LMB§r to use.");
            prov.add("emi.category.place_mats.place_mat", "Place Mat Interaction");
            prov.add("place_mats.tooltip.placemat.elevation", "§3Shift+Space/ Shift+Ctrl+Space§r to Raise/Lower. ");
            prov.add("place_mats.tooltip.placemat.roll", "§3Shift+Alt+Scroll§r for Roll. ");
            prov.add("place_mats.tooltip.placemat.instructions", "§3Shift+Scroll§r for Yaw. §3Shift+Ctrl+Scroll§r for Tilt. §3Shift+Alt+Scroll§r for Roll. §3Shift+Space§r to Raise. §3Shift+Ctrl+Space§r to Lower. ");
            prov.add("config.jade.plugin_place_mats.placemat.title", "Place Mat");
            prov.add("place_mats.tooltip.placemat.full", "§l§c⚠ Container Full!§r§r Max Stacks:§6 %s§r");
            prov.add("place_mats.emi.placemat.zone", "Zone");
            prov.add("place_mats.emi.placemat.offhand", "Offhand");
            prov.add("place_mats.tooltip.placemat.key", "§3RMB§r to lock/unlock.");
            prov.add("place_mats.tooltip.placemat.locked", "§cLocked \uD83D\uDD12");
            prov.add("place_mats.tooltip.key.explain", "§3RMB§r to lock/unlock Place Mats. Preventing item insertion and extraction.");
            prov.add("place_mats.tooltip.key.joke", "Doesn't taste as good as bread...");
            prov.add("place_mats.tooltip.shift_hint", "[Shift]§r...");
        });
    }
}

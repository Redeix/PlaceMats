package net.placemats.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

import net.placemats.PlaceMatMain;
import net.placemats.common.data.blocks.PlaceMatBlocks;

public class PlaceMatLanguageProvider extends LanguageProvider {
    public PlaceMatLanguageProvider(PackOutput output) {
        super(output, PlaceMatMain.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addBlock(PlaceMatBlocks.STORAGE_RACK, "Storage Rack");
        addBlock(PlaceMatBlocks.OAK_STORAGE_RACK, "Oak Storage Rack");
        add("block_type.pm.storage_rack", "%s Storage Rack");

        add("place_mats.creative_tab.place_mats", "Place Mats");

        add("place_mats.tooltip.placemat.placing", "Hold an item and §3RMB§r to display.");
        add("place_mats.tooltip.placemat.hold_alt_for_nutrition_info", "Hold (Alt) for Detailed Info");
        add("place_mats.tooltip.placemat.pitch", "§3Shift+Ctrl+Scroll§r for Tilt. ");
        add("place_mats.tooltip.placemat.yaw", "§3Shift+Scroll§r for Yaw. ");
        add("place_mats.tooltip.placemat.interacting", "§3RMB§r to remove. §3Shift+RMB§r to remove stack. §3LMB§r to use.");
        add("emi.category.place_mats.place_mat", "Place Mat Interaction");
        add("place_mats.tooltip.placemat.elevation", "§3Shift+Space/ Shift+Ctrl+Space§r to Raise/Lower. ");
        add("place_mats.tooltip.placemat.roll", "§3Shift+Alt+Scroll§r for Roll. ");
        add("place_mats.tooltip.placemat.instructions", "§3Shift+Scroll§r for Yaw. §3Shift+Ctrl+Scroll§r for Tilt. §3Shift+Alt+Scroll§r for Roll. §3Shift+Space§r to Raise. §3Shift+Ctrl+Space§r to Lower. ");
        add("config.jade.plugin_place_mats.placemat.title", "Place Mat");
        add("place_mats.tooltip.placemat.full", "§l§c⚠ Container Full!§r§r Max Stacks:§6 %s§r");
        add("place_mats.emi.placemat.zone", "Zone");
        add("place_mats.emi.placemat.offhand", "Offhand");
    }
}

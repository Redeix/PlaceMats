package net.placemats.common.data;

import com.tterrag.registrate.providers.ProviderType;

public class PlaceMatRegistries {
    public static final PlaceMatRegistrate REGISTRATE = PlaceMatRegistrate.create("place_mats");

    static {
        PlaceMatTags.addExistingTags(REGISTRATE);
        REGISTRATE.addDataGenerator(ProviderType.LANG, prov -> {
            prov.add("block_type.pm.storage_rack", "%s Storage Rack");
            prov.add("block_type.pm.ornate_shelf", "%s Ornate Shelf");
            prov.add("block_type.pm.ornate_double_shelf", "%s Ornate Double Shelf");
            prov.add("block_type.pm.floating_shelf", "%s Floating Shelf");
            prov.add("block_type.place_mats.storage_rack", "%s Storage Rack");
            prov.add("block_type.place_mats.ornate_shelf", "%s Ornate Shelf");
            prov.add("block_type.place_mats.ornate_double_shelf", "%s Ornate Double Shelf");
            prov.add("block_type.place_mats.floating_shelf", "%s Floating Shelf");
            prov.add("place_mats.creative_tab.place_mats", "Place Mats");
            prov.add("place_mats.tooltip.placemat.placing", "Hold an item and §3RMB§r to display.");
            prov.add("place_mats.tooltip.placemat.hold_alt_for_nutrition_info", "Hold (Alt) for Detailed Info");
            prov.add("place_mats.tooltip.placemat.pitch", "§3Shift+Ctrl+Scroll§r for Tilt. ");
            prov.add("place_mats.tooltip.placemat.yaw", "§3Shift+Scroll§r for Yaw. ");
            prov.add("place_mats.tooltip.placemat.interacting", "§3RMB§r to remove. §3Shift+RMB§r to remove stack. §3LMB§r to use.");
            prov.add("emi.category.place_mats.place_mat", "Place Mat Interaction");
            prov.add("place_mats.tooltip.placemat.elevation", "§3Shift+Space§r to Raise. §3Shift+Ctrl+Space§r to Lower. ");
            prov.add("place_mats.tooltip.placemat.roll", "§3Shift+Alt+Scroll§r for Roll. ");
            prov.add("place_mats.tooltip.placemat.instructions", "§3Shift+Scroll§r for Yaw. §3Shift+Ctrl+Scroll§r for Tilt. §3Shift+Alt+Scroll§r for Roll. §3Shift+Space§r to Raise. §3Shift+Ctrl+Space§r to Lower. ");
            prov.add("config.jade.plugin_place_mats.placemat.title", "Place Mat");
            prov.add("place_mats.tooltip.placemat.full", "§l§c⚠ Container Full!§r§r Max Stacks:§6 %s§r");
            prov.add("place_mats.tooltip.placemat.key", "§3RMB§r to lock/unlock.");
            prov.add("place_mats.tooltip.placemat.locked", "§cLocked \uD83D\uDD12");
            prov.add("place_mats.tooltip.key.explain", "§3RMB§r to lock/unlock Place Mats. Preventing item insertion and extraction.");
            prov.add("place_mats.tooltip.key.joke", "Doesn't taste as good as bread...");
            prov.add("place_mats.tooltip.shift_hint", "[Shift]§r...");
            prov.add("place_mats.tooltip.placemat.owner", "Owner: %s");
            prov.add("container.place_mats.cabinet_maker_table", "Cabinetmaker's Table");
            prov.add("gui.place_mats.cabinet_maker.confirm", "Apply");
            prov.add("gui.place_mats.cabinet_maker.preview", "3D Preview");
            prov.add("gui.place_mats.cabinet_maker.material_tooltip", "Place an item to extract 16x16 textures!");
            prov.add("gui.place_mats.cabinet_maker.click_to_apply", "Click to select this texture");
            prov.add("gui.place_mats.cabinet_maker.key_slot_title", "Key Slot %s: %s");
            prov.add("gui.place_mats.cabinet_maker.key.base", "Base Surface");
            prov.add("gui.place_mats.cabinet_maker.key.bars", "Bars");
            prov.add("gui.place_mats.cabinet_maker.key.generic", "Key %s");
            prov.add("gui.place_mats.cabinet_maker.color_picker_tab", "Color Tint (%s)");
            prov.add("gui.place_mats.cabinet_maker.cycle_blockstate", "Cycle Block State");
            prov.add("emi.category.place_mats.cabinetmaker_table", "Cabinetmaker's Table");
            prov.add("place_mats.emi.cabinet_maker.desc1", "⏵ A Cabinetmaker's Table allows you to customize certain dynamic blocks using extracted textures from any block or item.");
            prov.add("place_mats.emi.cabinet_maker.desc2", "⏵ Each material layer can be individually §ct§6i§en§at§3e§9d§r using the built in color picker tab.");
            prov.add("place_mats.emi.cabinet_maker.desc3", "⏵ Once you are happy with your customization, click the apply button to craft. §7(Shift-Click to apply for a full stack)§r");
            prov.add("place_mats.emi.crafting.clear_dynamic_color.desc", "⏵ Craft any customized dynamic block by itself to clear all dynamic textures & tinting.");
            prov.add("place_mats.emi.crafting.copy_dynamic_color.desc", "⏵ Combine a customized dynamic block with a matching block to copy customizations.");
            prov.add("place_mats.emi.placemat.explanation", "Place target item on mat/shelf, then right-click with held item.");
            prov.add("place_mats.emi.placemat.hand", "Held Item (Right-Click)");
            prov.add("place_mats.emi.placemat.offhand", "Offhand");
            prov.add("place_mats.emi.placemat.required_block", "Required Place Mat or Shelf");
            prov.add("place_mats.emi.placemat.target_input", "Item Placed on Mat / Shelf");
            prov.add("place_mats.emi.placemat.zone", "Zone %s");
        });
    }
}

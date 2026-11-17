package net.placemats.common.data;

import com.tterrag.registrate.providers.ProviderType;
import net.placemats.client.PlaceMatKeyMappings;

public class PlaceMatRegistries {
    public static final PlaceMatRegistrate REGISTRATE = PlaceMatRegistrate.create("place_mats");

    static {
        PlaceMatTags.addExistingTags(REGISTRATE);
        REGISTRATE.addDataGenerator(ProviderType.LANG, prov -> {

            // HUD Tooltips
            prov.add("place_mats.hud.controls.title", "Placement Controls");
            prov.add("place_mats.hud.interaction.title", "Item Interaction Controls");
            prov.add("place_mats.tooltip.placemat.hold_alt_for_nutrition_info", "Hold (Alt) for Detailed Info");
            prov.add("place_mats.tooltip.placemat.roll", "%s to §cRoll ⟳§r");
            prov.add("place_mats.tooltip.placemat.pitch", "%s to §3Pitch ⟳§r");
            prov.add("place_mats.tooltip.placemat.raise", "%s to §3Raise ▲§r");
            prov.add("place_mats.tooltip.placemat.lower", "%s to §cLower ▼§r");
            prov.add("place_mats.tooltip.placemat.place", "%s to §ePlace ✔§r");
            prov.add("place_mats.tooltip.placemat.place_stack", "%s to §ePlace Stack ✔✔§r");
            prov.add("place_mats.tooltip.placemat.remove", "%s to §cRemove ✘§r");
            prov.add("place_mats.tooltip.placemat.remove_stack", "%s to §cRemove Stack ✘✘§r");
            prov.add("place_mats.tooltip.placemat.use", "%s to §aUse/Interact ☆§r");
            prov.add("place_mats.tooltip.placemat.key", PlaceMatKeyMappings.mouseRMBIconCode + " to Lock/Unlock \uD83D\uDD12");
            prov.add("place_mats.tooltip.placemat.sit", PlaceMatKeyMappings.mouseRMBIconCode + " to Sit ⚓");
            prov.add("place_mats.tooltip.placemat.kick", PlaceMatKeyMappings.mouseRMBIconCode + " to Kick-Out ⏏");
            prov.add("place_mats.tooltip.placemat.full", "§l§c⚠ Container Full!§r§r Max Stacks:§6 %s§r");

            // Key Bindings
            prov.add("key.categories.place_mats", "Place-Mats");
            prov.add("key.place_mats.y_rot", "Y-Rotation (Pitch)");
            prov.add("key.place_mats.x_rot", "X-Rotation (Roll)");
            prov.add("key.place_mats.up", "Raise Elevation");
            prov.add("key.place_mats.down", "Lower Elevation");

            // Faux Keys
            prov.add("place_mats.faux_key.shift", "Shift");
            prov.add("place_mats.faux_key.ctrl", "Ctrl");
            prov.add("place_mats.faux_key.alt", "Alt");
            prov.add("place_mats.faux_key.lmb", PlaceMatKeyMappings.mouseLMBIconCode);
            prov.add("place_mats.faux_key.rmb", PlaceMatKeyMappings.mouseRMBIconCode);
            prov.add("place_mats.faux_key.mouse", PlaceMatKeyMappings.mouseIconCode);
            prov.add("place_mats.faux_key.mmb", PlaceMatKeyMappings.mouseScrollIconCode);
            prov.add("place_mats.faux_key.space", PlaceMatKeyMappings.spaceBarIconCode);

            // EMI Tooltips
            prov.add("emi.category.place_mats.cabinetmaker_table", "Cabinetmaker's Table");
            prov.add("emi.category.place_mats.place_mat", "Place-Mat Interaction");
            prov.add("place_mats.emi.cabinet_maker.desc1", "⏵ A Cabinetmaker's Table allows you to customize certain dynamic blocks using extracted textures from any block or item.");
            prov.add("place_mats.emi.cabinet_maker.desc2", "⏵ Each material layer can be individually §ct§6i§en§at§3e§9d§r using the built in color picker tab.");
            prov.add("place_mats.emi.cabinet_maker.desc3", "⏵ Once you are happy with your customization, click the apply button to craft. §7(Shift-Click to apply for a full stack)§r");
            prov.add("place_mats.emi.crafting.clear_dynamic_color.desc", "⏵ Craft any customized dynamic block by itself to clear all dynamic textures & tinting.");
            prov.add("place_mats.emi.crafting.copy_dynamic_color.desc", "⏵ Combine a customized dynamic block with a matching block to copy customizations.");
            prov.add("place_mats.emi.placemat.explanation", "Place target item on mat/shelf, then right-click with held item.");
            prov.add("place_mats.emi.placemat.hand", "Held Item (Right-Click)");
            prov.add("place_mats.emi.placemat.offhand", "Offhand");
            prov.add("place_mats.emi.placemat.required_block", "Required Place-Mat or Shelf");
            prov.add("place_mats.emi.placemat.target_input", "Item Placed on Mat / Shelf");
            prov.add("place_mats.emi.placemat.zone", "Zone %s");

            // Categories
            prov.add("config.jade.plugin_place_mats.placemat.title", "Place-Mat");
            prov.add("place_mats.creative_tab.place_mats", "Place-Mats");

            // Cabinetmaker's Table Tooltips
            prov.add("container.place_mats.cabinet_maker_table", "Cabinetmaker's Table");
            prov.add("gui.place_mats.cabinet_maker.confirm", "Apply");
            prov.add("gui.place_mats.cabinet_maker.stack", "Stack");
            prov.add("gui.place_mats.cabinet_maker.preview", "3D Preview");
            prov.add("gui.place_mats.cabinet_maker.material_tooltip", "Place an item to extract 16x16 textures!");
            prov.add("gui.place_mats.cabinet_maker.click_to_apply", "Click to select this texture");
            prov.add("gui.place_mats.cabinet_maker.key_slot_title", "Key Slot %s: %s");
            prov.add("gui.place_mats.cabinet_maker.key.base", "Base Surface");
            prov.add("gui.place_mats.cabinet_maker.key.bars", "Bars");
            prov.add("gui.place_mats.cabinet_maker.key.legs", "Legs");
            prov.add("gui.place_mats.cabinet_maker.key.gilding", "Gilding");
            prov.add("gui.place_mats.cabinet_maker.key.brackets", "Brackets");
            prov.add("gui.place_mats.cabinet_maker.key.generic", "Key %s");
            prov.add("gui.place_mats.cabinet_maker.color_picker_tab", "Color Tint (%s)");
            prov.add("gui.place_mats.cabinet_maker.cycle_blockstate", "Cycle Block State");
            prov.add("gui.place_mats.cabinet_maker.hue", "Hue");
            prov.add("gui.place_mats.cabinet_maker.hue.info", "Pure tint color spectrum without shading.");
            prov.add("gui.place_mats.cabinet_maker.saturation", "Saturation");
            prov.add("gui.place_mats.cabinet_maker.saturation.info", "Vividness or intensity of a color, from a pure hue to a dull white.");
            prov.add("gui.place_mats.cabinet_maker.value", "Value");
            prov.add("gui.place_mats.cabinet_maker.value.info", "Light/darkness of a color hue relative to white.");
            prov.add("gui.place_mats.cabinet_maker.alpha", "Alpha/Opacity");
            prov.add("gui.place_mats.cabinet_maker.alpha.info", "Transparency of a color, from fully opaque to fully transparent.");
            prov.add("gui.place_mats.cabinet_maker.emissive", "Emissive");
            prov.add("gui.place_mats.cabinet_maker.emissive.info", "How shadows and light effect a material, from normal shadow absorption to albedo.");

            // Misc. Tooltips
            prov.add("place_mats.tooltip.placemat.locked", "§cLocked \uD83D\uDD12");
            prov.add("place_mats.tooltip.key.explain", PlaceMatKeyMappings.mouseRMBIconCode + " to lock/unlock Place-Mat blocks. Preventing item insertion, extraction, and block destruction.");
            prov.add("place_mats.tooltip.key.joke", "Doesn't taste as good as bread...");
            prov.add("place_mats.tooltip.shift_hint", "[Shift]§r...");
            prov.add("place_mats.tooltip.placemat.owner", "Owner: %s");
            prov.add("place_mats.tooltip.dynamic_materials", "§3This block has dynamic materials support!§r Use a §oCabinetmaker's Table§r to customize it's textures and tinting.");
            prov.add("place_mats.tooltip.press_for_info", "§6Press [%s§6]§r for more information.");
            prov.add("place_mats.tooltip.texture_bundles", "This item is intended as a source of textures for the §oCabinetmaker's Table§r. Simply §6drag and drop§r§r it into a material slot and tint away!");
            prov.add("place_mats.tooltip.tall_tables.desc1", "⏵ This Place-Mat block has an additional placement zone space above itself.");
            prov.add("place_mats.tooltip.tall_tables.desc2", "⏵ These two blocks are tied together mechanically. You can automatically insert/extract from either one.");
            prov.add("place_mats.tooltip.tall_tables.desc3", "⏵ If the top zone has no items, and you " + PlaceMatKeyMappings.mouseRMBIconCode + " either block with a §6Key§r to lock it, the top zone will be removed so that you may place other blocks in that space. Unlock it to return the zone.");

        });
    }
}

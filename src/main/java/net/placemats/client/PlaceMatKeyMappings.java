package net.placemats.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@SuppressWarnings("unused")
public final class PlaceMatKeyMappings {
    public static final String CATEGORY = "key.categories.place_mats";

    public static final KeyMapping KEY_Y_ROT = new KeyMapping(
            "key.place_mats.y_rot",
            InputConstants.KEY_LEFT,
            CATEGORY
    );

    public static final KeyMapping KEY_X_ROT = new KeyMapping(
            "key.place_mats.x_rot",
            InputConstants.KEY_RIGHT,
            CATEGORY
    );

    public static final KeyMapping KEY_UP = new KeyMapping(
            "key.place_mats.up",
            InputConstants.KEY_UP,
            CATEGORY
    );

    public static final KeyMapping KEY_DOWN = new KeyMapping(
            "key.place_mats.down",
            InputConstants.KEY_DOWN,
            CATEGORY
    );

    public static final KeyMapping KEY_RESET = new KeyMapping(
            "key.place_mats.reset",
            InputConstants.KEY_R,
            CATEGORY
    );

    private PlaceMatKeyMappings() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_Y_ROT);
        event.register(KEY_X_ROT);
        event.register(KEY_UP);
        event.register(KEY_DOWN);
        event.register(KEY_RESET);
    }

    // =========================== Key Helpers ===============================

    public static final String mouseIconCode = "\uE803";
    public static final String mouseLMBIconCode = "\uE800";
    public static final String mouseRMBIconCode = "\uE801";
    public static final String mouseScrollIconCode = "\uE802";
    public static final String spaceBarIconCode = "\uE804";
}

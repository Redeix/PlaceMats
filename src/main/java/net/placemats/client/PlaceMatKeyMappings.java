package net.placemats.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class PlaceMatKeyMappings {
    public static final String CATEGORY = "key.categories.place_mats";

    public static final KeyMapping KEY_Y_ROT = new KeyMapping(
            "key.place_mats.y_rot",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping KEY_X_ROT = new KeyMapping(
            "key.place_mats.x_rot",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping KEY_UP = new KeyMapping(
            "key.place_mats.up",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );

    public static final KeyMapping KEY_DOWN = new KeyMapping(
            "key.place_mats.down",
            KeyConflictContext.IN_GAME,
            InputConstants.UNKNOWN,
            CATEGORY
    );

    private PlaceMatKeyMappings() {}

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_Y_ROT);
        event.register(KEY_X_ROT);
        event.register(KEY_UP);
        event.register(KEY_DOWN);
    }
}

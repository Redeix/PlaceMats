package net.placemats.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client Config. Not synced with server, only loaded on the client. Only use this for aesthetic and rendering options!
 */
public class ClientConfig {
    public final ForgeConfigSpec.BooleanValue renderTransformationArrows;
    public final ForgeConfigSpec.BooleanValue renderTransformationCompass;

    ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("hud_options");
        renderTransformationArrows = builder
                .comment("Enables the rendering of transformation arrows on placeable item previews. (Default: true)")
                .define("renderTransformationArrows", true);
        renderTransformationCompass = builder
                .comment("Enables the rendering of the transformation compass on placeable item previews which rotate with the item to indicate how the item has been rotated. (Default: true)")
                .define("renderTransformationCompass", true);

        builder.pop();
    }
}
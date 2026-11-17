package net.placemats.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client Config. Not synced with server, only loaded on the client. Only use this for aesthetic and rendering options!
 */
public class ClientConfig {
    public final ForgeConfigSpec.BooleanValue renderTransformationArrows;
    public final ForgeConfigSpec.BooleanValue renderTransformationCompass;
    public final ForgeConfigSpec.BooleanValue disableCombinationBasedRotationControls;
    public final ForgeConfigSpec.BooleanValue disableCombinationBasedElevationControls;
    public final ForgeConfigSpec.BooleanValue disableScoreboardControlsPanel;
    public final ForgeConfigSpec.BooleanValue disableScoreboardInteractionsPanel;
    public final ForgeConfigSpec.BooleanValue disableActionBarInformation;

    ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("hud_options");
        renderTransformationArrows = builder
                .comment("Enables the rendering of transformation arrows on placeable item previews. (Default: true)")
                .define("Render Transformation Arrows", true);
        renderTransformationCompass = builder
                .comment("\nEnables the rendering of the transformation compass on placeable item previews which rotate with the item to indicate how the item has been rotated. (Default: true)")
                .define("Render Transformation Compass", true);
        disableCombinationBasedRotationControls = builder
                .comment("\n\nDisables the shift-based rotation controls like shift+scroll to rotate, etc. Preferring direct keybinds. (Default: false)")
                .define("Disable Combination-Based Rotation Controls", false);
        disableCombinationBasedElevationControls = builder
                .comment("\nDisables the shift-based elevation controls like shift+space to raise, etc. Preferring direct keybinds. (Default: true)")
                .define("Disable Combination-Based Elevation Controls", true);
        disableScoreboardControlsPanel = builder
                .comment("\n\nDisables the score board panel for keyboard control instructions. (Default: false)")
                .define("Disable Scoreboard Controls Panel", false);
        disableScoreboardInteractionsPanel = builder
                .comment("\nDisables the score board panel for keyboard interaction control instructions. (Default: false)")
                .define("Disable Scoreboard Interactions Panel", false);
        disableActionBarInformation = builder
                .comment("\nDisables the action bar information display. (Default: false)")
                .define("Disable Action Bar Information", false);

        builder.pop();
    }
}
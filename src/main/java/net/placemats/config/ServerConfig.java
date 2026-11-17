package net.placemats.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig  {

    public final ForgeConfigSpec.BooleanValue consumeCabinetMakerMaterials;

    ServerConfig(ForgeConfigSpec.Builder builder) {

        builder.push("Cabinetmaker's Table");
        consumeCabinetMakerMaterials = builder
                .comment("\nWhether the cabinetmaker's table consumes actual material items instead of using ghost items. (Default: false)")
                .define("Consume Cabinetmaker's Table Materials", false);
        builder.pop();
    }
}

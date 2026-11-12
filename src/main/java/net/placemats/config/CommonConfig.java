package net.placemats.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {

    public final ForgeConfigSpec.ConfigValue<List<? extends String>> seatMobBlacklist;

    CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.push("seats");
        seatMobBlacklist = builder
            .comment("List of entity IDs that are not allowed to sit in place mat chair zones (ex. \"minecraft:horse\", \"minecraft:ender_dragon\").")
            .defineListAllowEmpty(
                List.of("seatMobBlacklist"), () -> List.of(
                    "minecraft:horse", "minecraft:mule", "minecraft:donkey", "minecraft:skeleton_horse", "minecraft:zombie_horse", "minecraft:trader_llama",
                    "minecraft:wither", "minecraft:ender_dragon", "minecraft:warden", "minecraft:ghast"
                ), o -> o instanceof String);
        builder.pop();
    }
}

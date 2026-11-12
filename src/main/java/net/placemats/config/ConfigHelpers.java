package net.placemats.config;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import static net.placemats.config.PlaceMatConfig.*;

public class ConfigHelpers {

    /**
     * Checks if a mob's EntityType is blacklisted from sitting in chair zones.
     */
    public static boolean isMobChairBlacklisted(EntityType<?> type) {
        if (COMMON == null || COMMON.seatMobBlacklist == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return false;
        }
        List<? extends String> blacklist;
        try {
            blacklist = COMMON.seatMobBlacklist.get();
        } catch (Exception e) {
            return false;
        }
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        String idStr = id.toString();
        String pathStr = id.getPath();
        for (String entry : blacklist) {
            if (entry != null && (entry.equalsIgnoreCase(idStr) || entry.equalsIgnoreCase(pathStr))) {
                return true;
            }
        }
        return false;
    }
}

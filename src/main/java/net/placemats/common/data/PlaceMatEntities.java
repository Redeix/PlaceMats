package net.placemats.common.data;

import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.entity.MobCategory;
import net.placemats.client.renderer.entity.SeatRenderer;
import net.placemats.common.entity.SeatEntity;

public class PlaceMatEntities {

    public static final EntityEntry<SeatEntity> SEAT = PlaceMatRegistries.REGISTRATE
            .<SeatEntity>entity("seat", SeatEntity::new, MobCategory.MISC)
            .properties(p -> p.sized(0.0f, 0.0f).noSummon().fireImmune())
            .renderer(() -> SeatRenderer::new)
            .register();

    public static void init() {
    }
}

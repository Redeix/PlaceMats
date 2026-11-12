package net.placemats.client.renderer.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.placemats.common.entity.SeatEntity;
import org.jetbrains.annotations.NotNull;

public class SeatRenderer extends EntityRenderer<SeatEntity> {

    public SeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SeatEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "block/white_wool");
    }

    @Override
    public boolean shouldRender(@NotNull SeatEntity entity, @NotNull Frustum frustum, double camX, double camY, double camZ) {
        return false;
    }
}

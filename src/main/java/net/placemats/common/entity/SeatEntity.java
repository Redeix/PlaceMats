package net.placemats.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.data.PlaceMatEntities;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SeatEntity(Level level, double x, double y, double z) {
        this(PlaceMatEntities.SEAT.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            BlockPos pos = this.blockPosition();
            if (this.getPassengers().isEmpty() || this.level().isEmptyBlock(pos) || !(this.level().getBlockState(pos).getBlock() instanceof PlaceMatBlock)) {
                this.discard();
            }
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return new Vec3(this.getX(), this.getY() + 0.5, this.getZ());
    }
}

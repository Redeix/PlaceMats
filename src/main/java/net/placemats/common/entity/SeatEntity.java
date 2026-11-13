package net.placemats.common.entity;

import java.util.UUID;
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
import net.placemats.common.event.PlaceMatInteractions;
import net.placemats.config.ConfigHelpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        if (!this.getPassengers().isEmpty()) {
            return false;
        }
        if (passenger.getPersistentData().contains(PlaceMatInteractions.CHAIR_COOLDOWN_TAG)) {
            if (this.level().getGameTime() < passenger.getPersistentData().getLong(PlaceMatInteractions.CHAIR_COOLDOWN_TAG)) {
                return false;
            }
        }
        return !ConfigHelpers.isMobChairBlacklisted(passenger.getType());
    }

    public @Nullable Entity getPassenger() {
        return this.getFirstPassenger();
    }

    public @Nullable UUID getPassengerUUID() {
        Entity passenger = this.getFirstPassenger();
        return passenger != null ? passenger.getUUID() : null;
    }

    public @Nullable EntityType<?> getPassengerType() {
        Entity passenger = this.getFirstPassenger();
        return passenger != null ? passenger.getType() : null;
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
    public boolean isAlive() {
        return false;
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

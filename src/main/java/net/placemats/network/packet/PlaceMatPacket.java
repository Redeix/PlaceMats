package net.placemats.network.packet;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.network.NetworkEvent;

import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.blockentity.PlaceMatBlockEntity;

public class PlaceMatPacket {
    private final BlockPos pos;
    private final Vec2 placementPos;
    private final net.minecraft.world.phys.Vec3 hitLocation;
    private final float rotation;
    private final float pitch;
    private final float roll;
    private final float height;
    private final int count;

    public PlaceMatPacket(BlockPos pos, Vec2 placementPos, net.minecraft.world.phys.Vec3 hitLocation, float rotation, float pitch, float roll, float height, int count) {
        this.pos = pos;
        this.placementPos = placementPos;
        this.hitLocation = hitLocation;
        this.rotation = rotation;
        this.pitch = pitch;
        this.roll = roll;
        this.height = height;
        this.count = count;
    }

    public static void encode(PlaceMatPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeFloat(message.placementPos.x);
        buffer.writeFloat(message.placementPos.y);
        buffer.writeDouble(message.hitLocation.x);
        buffer.writeDouble(message.hitLocation.y);
        buffer.writeDouble(message.hitLocation.z);
        buffer.writeFloat(message.rotation);
        buffer.writeFloat(message.pitch);
        buffer.writeFloat(message.roll);
        buffer.writeFloat(message.height);
        buffer.writeVarInt(message.count);
    }

    public static PlaceMatPacket decode(FriendlyByteBuf buffer) {
        return new PlaceMatPacket(
                buffer.readBlockPos(),
                new Vec2(buffer.readFloat(), buffer.readFloat()),
                new net.minecraft.world.phys.Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt());
    }

    public static void handle(PlaceMatPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                if (context.getSender().level().getBlockEntity(message.pos) instanceof PlaceMatBlockEntity foodPlacer) {
                    final PlaceMatBlock.PlacementRange[] targetedRange = { null };
                    if (foodPlacer.getBlockState().getBlock() instanceof PlaceMatBlock pmb) {
                        targetedRange[0] = pmb.getTargetedPlacementRange(foodPlacer.getBlockState(), message.hitLocation);
                    }

                    if (targetedRange[0] != null) {
                        // Project ray onto the box's bottom plane.
                        var player = context.getSender();
                        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition(1.0f);
                        net.minecraft.world.phys.Vec3 lookVec = player.getViewVector(1.0f);
                        double planeY = message.pos.getY() + targetedRange[0].box().minY;

                        net.minecraft.world.phys.Vec3 intersection = null;
                        if (Math.abs(lookVec.y) > 1e-6) {
                            double t = (planeY - eyePos.y) / lookVec.y;
                            if (t > 0) {
                                intersection = eyePos.add(lookVec.scale(t));
                            }
                        }

                        Vec2 serverPlacementPos = message.placementPos;
                        var def = foodPlacer.getEffectiveDefinition(player.getMainHandItem(), targetedRange[0]);
                        float multiplier = foodPlacer.getScaleMultiplier(targetedRange[0]);

                        float minX = (float) targetedRange[0].box().minX;
                        float maxX = (float) targetedRange[0].box().maxX - def.size().x * multiplier;
                        float minZ = (float) targetedRange[0].box().minZ;
                        float maxZ = (float) targetedRange[0].box().maxZ - def.size().y * multiplier;

                        float clampedX = Math.max(minX, Math.min(maxX, serverPlacementPos.x));
                        float clampedZ = Math.max(minZ, Math.min(maxZ, serverPlacementPos.y));
                        serverPlacementPos = new Vec2(clampedX, clampedZ);

                        if (foodPlacer.canPlace(player.getMainHandItem(), serverPlacementPos, message.height, targetedRange[0])) {
                            ItemStack held = player.getMainHandItem();
                            int toPlaceCount = Math.min(message.count, held.getCount());
                            PlaceMatBlock pmbBlock = (PlaceMatBlock) foodPlacer.getBlockState().getBlock();
                            int maxStack = Math.min(held.getMaxStackSize(), Math.min(pmbBlock.getMaxStackSize(), targetedRange[0].maxStackSize()));
                            toPlaceCount = Math.min(toPlaceCount, maxStack);

                            if (toPlaceCount > 0) {
                                ItemStack toPlace = held.copy();
                                toPlace.setCount(toPlaceCount);
                                foodPlacer.placeItem(toPlace, serverPlacementPos, message.rotation, message.pitch, message.roll, message.height, targetedRange[0]);
                                if (!player.isCreative()) {
                                    held.shrink(toPlaceCount);
                                }
                            }
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}

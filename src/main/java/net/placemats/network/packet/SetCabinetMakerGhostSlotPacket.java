package net.placemats.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.placemats.common.inventory.CabinetMakerTableContainer;

public class SetCabinetMakerGhostSlotPacket {

    private final int slotIndex;
    private final ItemStack itemStack;

    public SetCabinetMakerGhostSlotPacket(int slotIndex, ItemStack itemStack) {
        this.slotIndex = slotIndex;
        this.itemStack = itemStack != null ? itemStack : ItemStack.EMPTY;
    }

    public static void encode(SetCabinetMakerGhostSlotPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slotIndex);
        buf.writeItem(msg.itemStack);
    }

    public static SetCabinetMakerGhostSlotPacket decode(FriendlyByteBuf buf) {
        int slotIndex = buf.readVarInt();
        ItemStack itemStack = buf.readItem();
        return new SetCabinetMakerGhostSlotPacket(slotIndex, itemStack);
    }

    public static void handle(SetCabinetMakerGhostSlotPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof CabinetMakerTableContainer menu) {
                menu.setGhostSlot(msg.slotIndex, msg.itemStack);
            }
        });
        ctx.setPacketHandled(true);
    }
}

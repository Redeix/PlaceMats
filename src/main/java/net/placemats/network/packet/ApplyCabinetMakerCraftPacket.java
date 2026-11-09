package net.placemats.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.placemats.common.inventory.CabinetMakerTableContainer;

public class ApplyCabinetMakerCraftPacket {

    private final Map<String, ResourceLocation> chosenTextures;
    private final Map<String, Integer> chosenColors;
    private final boolean transformAll;

    public ApplyCabinetMakerCraftPacket(Map<String, ResourceLocation> chosenTextures, Map<String, Integer> chosenColors, boolean transformAll) {
        this.chosenTextures = chosenTextures != null ? chosenTextures : new HashMap<>();
        this.chosenColors = chosenColors != null ? chosenColors : new HashMap<>();
        this.transformAll = transformAll;
    }

    public static void encode(ApplyCabinetMakerCraftPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.chosenTextures.size());
        for (Map.Entry<String, ResourceLocation> entry : msg.chosenTextures.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(entry.getValue());
        }
        buf.writeVarInt(msg.chosenColors.size());
        for (Map.Entry<String, Integer> entry : msg.chosenColors.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        buf.writeBoolean(msg.transformAll);
    }

    public static ApplyCabinetMakerCraftPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, ResourceLocation> textures = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            ResourceLocation rl = buf.readResourceLocation();
            textures.put(key, rl);
        }
        int colorSize = buf.readVarInt();
        Map<String, Integer> colors = new HashMap<>();
        for (int i = 0; i < colorSize; i++) {
            String key = buf.readUtf();
            int color = buf.readInt();
            colors.put(key, color);
        }
        boolean transformAll = buf.readBoolean();
        return new ApplyCabinetMakerCraftPacket(textures, colors, transformAll);
    }

    public static void handle(ApplyCabinetMakerCraftPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof CabinetMakerTableContainer menu) {
                menu.executeCraft(msg.chosenTextures, msg.chosenColors, msg.transformAll);
            }
        });
        ctx.setPacketHandled(true);
    }
}

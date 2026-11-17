package net.placemats.network.packet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.placemats.common.inventory.CabinetMakerTableContainer;

@SuppressWarnings("unused")
public class ApplyCabinetMakerCraftPacket {

    private final Map<String, ResourceLocation> chosenTextures;
    private final Map<String, Integer> chosenColors;
    private final Map<String, Integer> chosenEmissives;
    private final boolean transformAll;

    public ApplyCabinetMakerCraftPacket(Map<String, ResourceLocation> chosenTextures, Map<String, Integer> chosenColors, boolean transformAll) {
        this(chosenTextures, chosenColors, Collections.emptyMap(), transformAll);
    }

    public ApplyCabinetMakerCraftPacket(Map<String, ResourceLocation> chosenTextures, Map<String, Integer> chosenColors, Map<String, Integer> chosenEmissives, boolean transformAll) {
        this.chosenTextures = chosenTextures != null ? chosenTextures : new HashMap<>();
        this.chosenColors = chosenColors != null ? chosenColors : new HashMap<>();
        this.chosenEmissives = chosenEmissives != null ? chosenEmissives : new HashMap<>();
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
        buf.writeVarInt(msg.chosenEmissives.size());
        for (Map.Entry<String, Integer> entry : msg.chosenEmissives.entrySet()) {
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
        int emissiveSize = buf.readVarInt();
        Map<String, Integer> emissives = new HashMap<>();
        for (int i = 0; i < emissiveSize; i++) {
            String key = buf.readUtf();
            int emissive = buf.readInt();
            emissives.put(key, emissive);
        }
        boolean transformAll = buf.readBoolean();
        return new ApplyCabinetMakerCraftPacket(textures, colors, emissives, transformAll);
    }

    public static void handle(ApplyCabinetMakerCraftPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof CabinetMakerTableContainer menu) {
                menu.executeCraft(msg.chosenTextures, msg.chosenColors, msg.chosenEmissives, msg.transformAll);
            }
        });
        ctx.setPacketHandled(true);
    }
}

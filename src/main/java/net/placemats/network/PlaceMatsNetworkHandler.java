package net.placemats.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import net.placemats.PlaceMatMain;
import net.placemats.network.packet.PlaceMatPacket;
import net.placemats.network.packet.SyncPlaceMatDefinitionsPacket;

public class PlaceMatsNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void init() {
        INSTANCE.registerMessage(
                id(),
                SyncPlaceMatDefinitionsPacket.class,
                SyncPlaceMatDefinitionsPacket::encode,
                SyncPlaceMatDefinitionsPacket::decode,
                SyncPlaceMatDefinitionsPacket::handle);
        INSTANCE.registerMessage(
                id(),
                PlaceMatPacket.class,
                PlaceMatPacket::encode,
                PlaceMatPacket::decode,
                PlaceMatPacket::handle);
    }
}

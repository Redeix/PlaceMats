package net.placemats.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import net.placemats.common.data.resource.DefinitionManager;

public class SyncPlaceMatDefinitionsPacket {
    private final Map<Item, DefinitionManager.PlaceMatDefinition> definitions;
    private final Map<TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions;

    public SyncPlaceMatDefinitionsPacket(Map<Item, DefinitionManager.PlaceMatDefinition> definitions, Map<TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions) {
        this.definitions = definitions;
        this.tagDefinitions = tagDefinitions;
    }

    public static void encode(SyncPlaceMatDefinitionsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.definitions.size());
        packet.definitions.forEach((item, def) -> {
            buffer.writeResourceLocation(ForgeRegistries.ITEMS.getKey(item));
            encodeDefinition(def, buffer);
        });
        buffer.writeInt(packet.tagDefinitions.size());
        packet.tagDefinitions.forEach((tag, def) -> {
            buffer.writeResourceLocation(tag.location());
            encodeDefinition(def, buffer);
        });
    }

    private static void encodeDefinition(DefinitionManager.PlaceMatDefinition def, FriendlyByteBuf buffer) {
        buffer.writeFloat(def.size().x);
        buffer.writeFloat(def.size().y);
        buffer.writeBoolean(def.model() != null);
        if (def.model() != null)
            buffer.writeResourceLocation(def.model());
        buffer.writeBoolean(def.modelRotten() != null);
        if (def.modelRotten() != null)
            buffer.writeResourceLocation(def.modelRotten());
        buffer.writeFloat(def.scale());
        buffer.writeBoolean(def.flat());
        buffer.writeBoolean(def.itemHeight() != null);
        if (def.itemHeight() != null)
            buffer.writeFloat(def.itemHeight());
        buffer.writeBoolean(def.stackable());
        buffer.writeBoolean(def.allowsStackingOnTop());
    }

    public static SyncPlaceMatDefinitionsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<Item, DefinitionManager.PlaceMatDefinition> definitions = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Item item = ForgeRegistries.ITEMS.getValue(buffer.readResourceLocation());
            DefinitionManager.PlaceMatDefinition def = decodeDefinition(buffer);
            if (item != null) {
                definitions.put(item, def);
            }
        }
        int tagSize = buffer.readInt();
        Map<TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions = new HashMap<>();
        for (int i = 0; i < tagSize; i++) {
            TagKey<Item> tagKey = ForgeRegistries.ITEMS.tags().createTagKey(buffer.readResourceLocation());
            DefinitionManager.PlaceMatDefinition def = decodeDefinition(buffer);
            tagDefinitions.put(tagKey, def);
        }
        return new SyncPlaceMatDefinitionsPacket(definitions, tagDefinitions);
    }

    private static DefinitionManager.PlaceMatDefinition decodeDefinition(FriendlyByteBuf buffer) {
        float width = buffer.readFloat();
        float depth = buffer.readFloat();
        ResourceLocation model = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        ResourceLocation modelRotten = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        float scale = buffer.readFloat();
        boolean flat = buffer.readBoolean();
        Float itemHeight = buffer.readBoolean() ? buffer.readFloat() : null;
        boolean stackable = buffer.readBoolean();
        boolean allowsStackingOnTop = buffer.readBoolean();
        return new DefinitionManager.PlaceMatDefinition(new Vec2(width, depth), model, modelRotten, scale, flat, itemHeight, stackable, allowsStackingOnTop);
    }

    public static void handle(SyncPlaceMatDefinitionsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DefinitionManager.getDefinitionsMap().clear();
            DefinitionManager.getDefinitionsMap().putAll(packet.definitions);
            DefinitionManager.getTagDefinitionsMap().clear();
            DefinitionManager.getTagDefinitionsMap().putAll(packet.tagDefinitions);
            DefinitionManager.postKjsEvent();
        });
        context.setPacketHandled(true);
    }
}

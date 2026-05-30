package net.placemats.compat.kjs;

import java.util.Map;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;
import net.placemats.common.data.resource.DefinitionManager;
import net.placemats.compat.ModCompat;

public interface KJSCompat {
    KJSCompat INSTANCE = ModCompat.KJS_LOADED ? new KJSCompatImpl() : new NoKJSCompat();

    void postEvent(Map<Item, DefinitionManager.PlaceMatDefinition> definitions, Map<TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions);

    InteractionResult onInteraction(Player player, PlacedItem targeted, int zoneIndex, PlaceMatBlockEntity placeMat);

    class NoKJSCompat implements KJSCompat {
        @Override
        public void postEvent(Map<Item, DefinitionManager.PlaceMatDefinition> definitions, Map<TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions) {
        }

        @Override
        public InteractionResult onInteraction(Player player, PlacedItem targeted, int zoneIndex, PlaceMatBlockEntity placeMat) {
            return InteractionResult.PASS;
        }
    }
}

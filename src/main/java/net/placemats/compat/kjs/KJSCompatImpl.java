package net.placemats.compat.kjs;

import java.util.Map;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;
import net.placemats.common.data.resource.DefinitionManager;
import net.placemats.compat.kjs.events.PlaceMatInteractionEventJS;
import net.placemats.compat.kjs.events.PlaceMatKJSServerEvents;
import net.placemats.compat.kjs.events.PlaceMatKJSStartupEvents;
import net.placemats.compat.kjs.events.RegisterPlaceMatDefinitionsEventJS;

public class KJSCompatImpl implements KJSCompat {
    @Override
    public void postEvent(Map<Item, DefinitionManager.PlaceMatDefinition> definitions, Map<TagKey<Item>, DefinitionManager.PlaceMatDefinition> tagDefinitions) {
        var event = new RegisterPlaceMatDefinitionsEventJS();
        PlaceMatKJSStartupEvents.PLACE_MAT_DEFINITIONS.post(event);
        event.apply(definitions, tagDefinitions);
    }

    @Override
    public InteractionResult onInteraction(Player player, PlacedItem targeted, int zoneIndex, PlaceMatBlockEntity placeMat) {
        PlaceMatInteractionEventJS event = new PlaceMatInteractionEventJS(player, targeted, zoneIndex, placeMat);
        PlaceMatKJSServerEvents.PLACEMAT_INTERACTION.post(event);
        if (event.isInteractionHandled()) {
            return InteractionResult.sidedSuccess(placeMat.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }
}

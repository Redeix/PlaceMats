package net.placemats.common;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlaceMatInteractions;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.resource.DefinitionManager;

@Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID)
public final class ForgeCommonEventListener {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof PlaceMatBlockEntity foodPlacer) {
            if (PlaceMatInteractions.handleLeftClick(foodPlacer, event.getEntity(), null)) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DefinitionManager(false));
    }

}

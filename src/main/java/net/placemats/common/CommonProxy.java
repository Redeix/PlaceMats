package net.placemats.common;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatCreativeTab;
import net.placemats.common.data.blocks.PlaceMatBlocks;
import net.placemats.network.PlaceMatsNetworkHandler;

public class CommonProxy {

    @SuppressWarnings("removal")
    public CommonProxy() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.register(this);

        PlaceMatsNetworkHandler.init();
        PlaceMatBlocks.init();
        PlaceMatBlockEntities.init();
        PlaceMatCreativeTab.init();
    }

}

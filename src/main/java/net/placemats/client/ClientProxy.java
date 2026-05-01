package net.placemats.client;

import org.jetbrains.annotations.NotNull;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.placemats.common.CommonProxy;
import net.placemats.common.data.resource.DefinitionManager;

public class ClientProxy extends CommonProxy {
    @SuppressWarnings("removal")
    public ClientProxy() {
        super();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(ClientHandler.class);
        bus.addListener(this::registerClientReloadListeners);
        bus.addListener(this::registerSpecialModels);
        bus.addListener(this::registerRenderers);
    }

    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(net.placemats.common.data.PlaceMatBlockEntities.PLACE_MAT.get(), net.placemats.client.renderer.blockentity.PlaceMatRenderer::new);
    }

    public void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DefinitionManager(true));
    }

    @SubscribeEvent
    public void registerParticles(@NotNull RegisterParticleProvidersEvent event) {
    }

    @SubscribeEvent
    public void registerSpecialModels(ModelEvent.RegisterAdditional event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        DefinitionManager.registerModels(mc.getResourceManager(), event::register);
    }
}

package net.placemats.client;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.placemats.client.gui.CabinetMakerTableScreen;
import net.placemats.client.renderer.DynamicShelfBakedModel;
import net.placemats.client.renderer.blockentity.PlaceMatRenderer;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatContainers;
import org.jetbrains.annotations.NotNull;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.placemats.common.CommonProxy;
import net.placemats.common.data.resource.DefinitionManager;

public class ClientProxy extends CommonProxy {
    @SuppressWarnings("removal")
    public ClientProxy() {
        super();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(ClientHandler.class);
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerClientReloadListeners);
        bus.addListener(this::registerSpecialModels);
        bus.addListener(this::registerRenderers);
        bus.addListener(this::onModifyBakingResult);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(PlaceMatContainers.CABINET_MAKER_TABLE.get(), CabinetMakerTableScreen::new));
    }

    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PlaceMatBlockEntities.PLACE_MAT.get(), PlaceMatRenderer::new);
    }

    public void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DefinitionManager(true));
    }

    @SubscribeEvent
    public void registerParticles(@NotNull RegisterParticleProvidersEvent event) {
    }

    @SubscribeEvent
    public void registerSpecialModels(ModelEvent.RegisterAdditional event) {
        Minecraft mc = Minecraft.getInstance();
        DefinitionManager.registerModels(mc.getResourceManager(), event::register);
    }

    public void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof IDynamicTexturedBlock dynamicBlock && dynamicBlock.getTextureKeyCount() > 0) {
                ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
                if (blockId == null) {
                    continue;
                }

                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    ModelResourceLocation stateLoc = BlockModelShaper.stateToModelLocation(state);
                    BakedModel existing = models.get(stateLoc);
                    if (existing != null && !(existing instanceof DynamicShelfBakedModel)) {
                        models.put(stateLoc, new DynamicShelfBakedModel(existing, dynamicBlock));
                    }
                }

                if (block.asItem() != Items.AIR) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
                    if (itemId != null) {
                        ModelResourceLocation itemLoc = new ModelResourceLocation(itemId, "inventory");
                        BakedModel existing = models.get(itemLoc);
                        if (existing != null && !(existing instanceof DynamicShelfBakedModel)) {
                            models.put(itemLoc, new DynamicShelfBakedModel(existing, dynamicBlock));
                        }
                    }
                }

                for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
                    ResourceLocation loc = entry.getKey();
                    if (loc.getNamespace().equals(blockId.getNamespace()) && loc.getPath().equals(blockId.getPath())) {
                        BakedModel existing = entry.getValue();
                        if (existing != null && !(existing instanceof DynamicShelfBakedModel)) {
                            entry.setValue(new DynamicShelfBakedModel(existing, dynamicBlock));
                        }
                    }
                }
            }
        }
    }
}

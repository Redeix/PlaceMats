package net.placemats.client;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.placemats.PlaceMatMain;
import net.placemats.client.gui.CabinetMakerTableScreen;
import net.placemats.client.renderer.CyclicLayerBakedModel;
import net.placemats.client.renderer.DynamicTexturedBakedModel;
import net.placemats.client.renderer.blockentity.PlaceMatRenderer;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatContainers;
import org.jetbrains.annotations.NotNull;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.placemats.client.gui.tooltip.ClientTextureTooltip;
import net.placemats.client.gui.tooltip.TextureTooltip;
import net.placemats.client.gui.tooltip.TextureTooltipHelper;
import net.placemats.common.CommonProxy;
import net.placemats.common.data.resource.DefinitionManager;

public class ClientProxy extends CommonProxy {
    public static final ResourceLocation HUD_ARROWS_MODEL = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "hud/hud_arrows");
    public static final ResourceLocation HUD_COMPASS_MODEL = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "hud/hud_compass");

    @SuppressWarnings("removal")
    public ClientProxy() {
        super();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.register(ClientHandler.class);
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerClientReloadListeners);
        bus.addListener(this::registerSpecialModels);
        bus.addListener(this::registerRenderers);
        bus.addListener(this::registerTooltipFactories);
        bus.addListener(this::onModifyBakingResult);
        bus.addListener(PlaceMatKeyMappings::registerKeyMappings);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(PlaceMatContainers.CABINET_MAKER_TABLE.get(), CabinetMakerTableScreen::new));
    }

    public void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(TextureTooltip.class, ClientTextureTooltip::new);
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
        event.register(HUD_ARROWS_MODEL);
        event.register(HUD_COMPASS_MODEL);
    }

    public void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        TextureTooltipHelper.clearCache();
        Map<ResourceLocation, BakedModel> models = event.getModels();

        for (Block block : ForgeRegistries.BLOCKS) {
            // Dynamic Materials Blocks
            if (block instanceof IDynamicTexturedBlock dynamicBlock && dynamicBlock.getTextureKeyCount() > 0) {
                ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
                if (blockId == null) {
                    continue;
                }

                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    ModelResourceLocation stateLoc = BlockModelShaper.stateToModelLocation(state);
                    BakedModel existing = models.get(stateLoc);
                    if (existing != null && !(existing instanceof DynamicTexturedBakedModel)) {
                        models.put(stateLoc, new DynamicTexturedBakedModel(existing, dynamicBlock));
                    }
                }

                if (block.asItem() != Items.AIR) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
                    if (itemId != null) {
                        ModelResourceLocation itemLoc = new ModelResourceLocation(itemId, "inventory");
                        BakedModel existing = models.get(itemLoc);
                        if (existing != null && !(existing instanceof DynamicTexturedBakedModel)) {
                            models.put(itemLoc, new DynamicTexturedBakedModel(existing, dynamicBlock));
                        }
                    }
                }

                for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
                    ResourceLocation loc = entry.getKey();
                    if (loc.getNamespace().equals(blockId.getNamespace()) && loc.getPath().equals(blockId.getPath())) {
                        BakedModel existing = entry.getValue();
                        if (existing != null && !(existing instanceof DynamicTexturedBakedModel)) {
                            entry.setValue(new DynamicTexturedBakedModel(existing, dynamicBlock));
                        }
                    }
                }
            }
        }

        // Cyclic Layer Models
        for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
            if (entry.getKey() instanceof ModelResourceLocation mrl && "inventory".equals(mrl.getVariant())) {
                ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(mrl.getNamespace(), mrl.getPath());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);

                if (item != null) {
                    entry.setValue(new CyclicLayerBakedModel(entry.getValue(), item));
                }
            }
        }
    }
}

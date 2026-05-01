package net.placemats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

import net.placemats.client.ClientProxy;
import net.placemats.common.CommonProxy;
import net.placemats.common.data.PlaceMatBlockEntities;
import net.placemats.common.data.PlaceMatCreativeTab;
import net.placemats.common.data.RecipeSerializers;
import net.placemats.common.data.PlaceMatRecipeTypes;
import net.placemats.common.data.blocks.PlaceMatBlocks;

@Mod(PlaceMatMain.MOD_ID)
public final class PlaceMatMain {

    public static final String MOD_ID = "place_mats";
    public static final String NAME = "PlaceMats";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

    @SuppressWarnings("removal")
    public PlaceMatMain() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        PlaceMatBlocks.BLOCKS.register(bus);
        PlaceMatBlocks.ITEMS.register(bus);
        PlaceMatBlockEntities.BLOCK_ENTITIES.register(bus);
        PlaceMatCreativeTab.CREATIVE_MODE_TABS.register(bus);
        PlaceMatRecipeTypes.RECIPE_TYPES.register(bus);
        RecipeSerializers.RECIPE_SERIALIZERS.register(bus);

        setupFixForGlobalServerConfig();

        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    @SuppressWarnings("removal")
    private static void setupFixForGlobalServerConfig() {
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }
}

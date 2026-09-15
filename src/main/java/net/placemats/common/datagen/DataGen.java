package net.placemats.common.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.placemats.PlaceMatMain;

@Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(event.includeServer(), new PlaceMatRecipeProvider(packOutput));
        generator.addProvider(event.includeServer() || event.includeClient(), new PlaceMatDefinitionProvider(packOutput).addAll());
    }
}

package net.placemats.compat.jade;

import net.minecraft.resources.ResourceLocation;

import net.placemats.PlaceMatMain;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import net.placemats.common.block.PlaceMatBlock;

@WailaPlugin
public class PlaceMatJadePlugin implements IWailaPlugin {

    public static final ResourceLocation PLACE_MAT_INFO = PlaceMatMain.id("placemat.title");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PlaceMatProvider.INSTANCE, PlaceMatBlock.class);
    }
}

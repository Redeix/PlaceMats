package net.placemats.common.data;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.placemats.PlaceMatMain;
import net.placemats.common.inventory.CabinetMakerTableContainer;

public final class PlaceMatContainers
{

    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, PlaceMatMain.MOD_ID);

    public static final RegistryObject<MenuType<CabinetMakerTableContainer>> CABINET_MAKER_TABLE = CONTAINERS.register(
            "cabinet_maker_table",
            () -> IForgeMenuType.create((windowId, inv, data) -> new CabinetMakerTableContainer(windowId, inv))
    );

    public static void init() {
    }

    public static void register(IEventBus bus) {
        CONTAINERS.register(bus);
    }
}

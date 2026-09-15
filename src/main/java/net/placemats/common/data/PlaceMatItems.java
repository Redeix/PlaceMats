package net.placemats.common.data;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.placemats.PlaceMatMain;

@SuppressWarnings({ "unused" })
public final class PlaceMatItems
{
    public static void init() {
    }

    // Make sure item model locations are different from existing model locations.
    public static final ItemEntry<Item> KEY = PlaceMatRegistries.REGISTRATE.item("key", Item::new)
        .model(existingModel("item/parent/key"))
        .tag(PlaceMatTags.Items.KEY, PlaceMatTags.Items.PLACE_MAT_BLACKLIST)
        .register();

    // Helpers
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> existingModel(String path) {
        return (ctx, prov) -> prov.withExistingParent(ctx.getName(), ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, path));
    }
}

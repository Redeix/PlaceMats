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

    // Texture Bundles
    // Update TEXTURE_BUNDLE_COUNT to add new bundles.
    public static final int TEXTURE_BUNDLE_COUNT = 1;
    static {
        for (int i = 0; i < TEXTURE_BUNDLE_COUNT; ++i) {
            final int bundleIndex = i + 1;

            PlaceMatRegistries.REGISTRATE.item("texture_bundle_" + bundleIndex, Item::new)
                .lang("Texture Bundle [" + bundleIndex + "]")
                .model((ctx, prov) -> {
                    var model = prov.withExistingParent(ctx.getName(), prov.mcLoc("item/generated"));
                    for (int layer = 0; layer < 5; layer++) {
                        model.texture("layer" + layer, prov.modLoc("item/texture_bundles/b" + bundleIndex + "/b" + bundleIndex + "_" + layer));
                    }
                })
                .tag(PlaceMatTags.Items.TEXTURE_BUNDLES, PlaceMatTags.Items.TEXTURE_PROVIDERS)
                .register();
        }
    }

    // Helpers
    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> existingModel(String path) {
        return (ctx, prov) -> prov.withExistingParent(ctx.getName(), ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, path));
    }
}

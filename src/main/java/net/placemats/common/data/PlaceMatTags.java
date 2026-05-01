package net.placemats.common.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import net.placemats.PlaceMatMain;

public final class PlaceMatTags {

    public static final class Items {

        public static final TagKey<Item> PLACE_MAT_BLACKLIST = createItemTag("place_mat_blacklist");
        public static final TagKey<Item> PLACE_MATS = createItemTag("place_mats");

        private static TagKey<Item> createItemTag(String path) {
            return createItemTag(PlaceMatMain.id(path));
        }

        private static TagKey<Item> createItemTag(ResourceLocation resLoc) {
            return TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), resLoc);
        }
    }

    public static final class Blocks {

        public static final TagKey<Block> PLACE_MATS = createBlockTag("place_mats");
        public static final TagKey<Block> PLACE_MAT_BLACKLIST = createBlockTag("place_mat_blacklist");

        private static TagKey<Block> createBlockTag(String path) {
            return createBlockTag(PlaceMatMain.id(path));
        }

        private static TagKey<Block> createBlockTag(ResourceLocation resLoc) {
            return TagKey.create(ForgeRegistries.BLOCKS.getRegistryKey(), resLoc);
        }
    }
}

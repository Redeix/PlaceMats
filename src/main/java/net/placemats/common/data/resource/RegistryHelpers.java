package net.placemats.common.data.resource;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.placemats.common.block.PlaceMatBlock;

/**
 * Helper methods for place mat registration.
 */
public class RegistryHelpers {

    /* ===================================== Texture Helpers ==================================== */

    /**
     * Gets the stripped log texture for the given wood type.
     * @param woodType Wood type.
     * @return Stripped log texture.
     */
    public static ResourceLocation getStrippedLogTexture(WoodType woodType) {
        String name = woodType.name();
        if ("bamboo".equals(name)) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/stripped_bamboo_block");
        } else if ("crimson".equals(name) || "warped".equals(name)) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/stripped_" + name + "_stem");
        } else {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "block/stripped_" + name + "_log");
        }
    }

    /* ===================================== Blockstate Helpers ==================================== */

    /**
     * Basic helper method for creating blockstates for basic wood place mats.
     * @param woodType Wood type.
     * @param woodTextureSuffix Suffix for the wood texture in index 0. (ex. smooth_shelf, shelf)
     * @param blockName Name of the place mat type. Used for the model location parent. (ex. storage_rack, ornate_shelf)
     * @return Blockstate provider.
     */
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> basicWoodBlockState(WoodType woodType, String woodTextureSuffix, String blockName) {
        return (ctx, prov) -> {
            var model = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/" + blockName + "_parent"))
                .texture("0", prov.modLoc("block/" + woodType.name() + "_" + woodTextureSuffix));
            prov.horizontalBlock(ctx.getEntry(), model);
        };
    }

    /* ===================================== Placement Range Helpers ==================================== */

    /**
     * Constructs a basic placement range for a place mat block with default values.
     * @param shape The voxel shape of the placement range.
     * @param maxHeight The maximum height of the placement range.
     * @param maxStackSize The maximum stack size for items in the placement range.
     * @return A new PlaceMatBlock.PlacementRange instance.
     */
    public static PlaceMatBlock.PlacementRange basicPlacementRange(VoxelShape shape, float maxHeight, int maxStackSize) {
        return new PlaceMatBlock.PlacementRange(
            shape,
            maxHeight,
            false, false, false, false, true, false, null, false, false,
            maxStackSize,
            null, false, false, false, false, 1.0f, 0, 0, 0, 0);
    }

    /**
     * Constructs a basic placement range for a place mat block with default values. And a stack size of 16.
     * @param shape The voxel shape of the placement range.
     * @param maxHeight The maximum height of the placement range.
     * @return A new PlaceMatBlock.PlacementRange instance.
     */
    public static PlaceMatBlock.PlacementRange basicPlacementRange(VoxelShape shape, float maxHeight) {
        return basicPlacementRange(shape, maxHeight, 16);
    }
}

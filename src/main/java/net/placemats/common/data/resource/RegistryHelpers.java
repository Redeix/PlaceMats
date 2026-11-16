package net.placemats.common.data.resource;

import java.util.List;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.placemats.common.block.PlaceMatApronTableBlock;
import net.placemats.common.block.PlaceMatAttachedStairsBlock;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatStairsBlock;
import net.placemats.common.block.PlaceMatTableBlock;
import net.placemats.common.block.PlacementRange;

/**
 * Helper methods for place mat registration.
 */
@SuppressWarnings("unused")
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

    /* ===================================== Property Helpers ====================================== */

    public static final NonNullUnaryOperator<BlockBehaviour.Properties> WOOD_PROPERTIES = p -> p
            .sound(SoundType.WOOD)
            .ignitedByLava()
            .mapColor(MapColor.WOOD)
            .pushReaction(PushReaction.PUSH_ONLY)
            .strength(2.0f)
            .noOcclusion()
            .isViewBlocking((state, level, pos) -> false);

    /* ===================================== Blockstate Helpers ==================================== */

    /**
     * Basic helper method for creating blockstates for basic wood place mats.
     * @param woodType Wood type.
     * @param woodTextureSuffix Suffix for the wood texture in index 0. (ex. smooth_shelf, shelf)
     * @param blockName Name of the place mat type. Used for the model location parent. (ex. storage_rack, ornate_shelf)
     * @return Blockstate provider.
     */
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> basicWoodBlockState(
        WoodType woodType,
        String woodTextureSuffix,
        String blockName) {

        return (ctx, prov) -> {
            var model = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/" + blockName + "_parent"))
                .texture("0", prov.modLoc("block/" + woodType.name() + "_" + woodTextureSuffix));
            prov.horizontalBlock(ctx.getEntry(), model);
        };
    }

    public record TextureConfig(String index, ResourceLocation resource) {}

    /**
     * Helper method for creating stairs style place mat blockstates.
     * Handles cardinal rotations, inner, and outer states.
     * @param textures TextureConfig list. (ex. `List.of(new TextureConfig("0", ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "block/" + wood_type + "shelf"))))`)
     * @param blockName Name of the place mat type. Used for the model location parent. (ex. storage_rack, ornate_shelf)
     * @return Blockstate provider.
     */
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> stairsBlockState(
        List<TextureConfig> textures,
        String blockName) {

        return (ctx, prov) -> {
            var straightModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/" + blockName + "_parent"));
            var innerModel = prov.models().withExistingParent(ctx.getName() + "_inner", prov.modLoc("block/" + blockName + "_inner_parent"));
            var outerModel = prov.models().withExistingParent(ctx.getName() + "_outer", prov.modLoc("block/" + blockName + "_outer_parent"));

            var allModels = new BlockModelBuilder[]{ straightModel, innerModel, outerModel };

            for (var model : allModels) {
                for (TextureConfig texture : textures) {
                    model.texture(texture.index(), texture.resource());
                }
            }

            prov.getVariantBuilder(ctx.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(PlaceMatStairsBlock.FACING);
                StairsShape shape = state.getValue(PlaceMatStairsBlock.SHAPE);

                int yRot = (int) facing.getClockWise().toYRot();
                if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                    yRot += 270;
                }
                yRot %= 360;

                var chosenModel = shape == StairsShape.STRAIGHT
                    ? straightModel
                    : (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT
                    ? innerModel
                    : outerModel);

                return ConfiguredModel.builder()
                    .modelFile(chosenModel)
                    .rotationY(yRot)
                    .uvLock(true)
                    .build();
            }, PlaceMatBlock.LOCKED);
        };
    }

    /**
     * Helper method for creating attached stairs style place mat blockstates.
     * Handles cardinal rotations, inner, outer, and attached states.
     * @param textures TextureConfig list. (ex. `List.of(new TextureConfig("0", "block/" + wood_type + "shelf"))`)
     * @param blockName Name of the place mat type. Used for the model location parent. (ex. storage_rack, ornate_shelf)
     * @return Blockstate provider.
     */
    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> attachedStairsBlockState(
        List<TextureConfig> textures,
        String blockName) {

        return (ctx, prov) -> {
            var straightModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/" + blockName + "_parent"));
            var straightAttachedModel = prov.models().withExistingParent(ctx.getName() + "_attached", prov.modLoc("block/" + blockName + "_attached_parent"));
            var innerModel = prov.models().withExistingParent(ctx.getName() + "_inner", prov.modLoc("block/" + blockName + "_inner_parent"));
            var innerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_inner_attached", prov.modLoc("block/" + blockName + "_inner_attached_parent"));
            var outerModel = prov.models().withExistingParent(ctx.getName() + "_outer", prov.modLoc("block/" + blockName + "_outer_parent"));
            var outerAttachedModel = prov.models().withExistingParent(ctx.getName() + "_outer_attached", prov.modLoc("block/" + blockName + "_outer_attached_parent"));

            var allModels = new BlockModelBuilder[]{
                straightModel, straightAttachedModel,
                innerModel, innerAttachedModel,
                outerModel, outerAttachedModel
            };

            for (var model : allModels) {
                for (TextureConfig texture : textures) {
                    model.texture(texture.index(), texture.resource());
                }
            }

            prov.getVariantBuilder(ctx.getEntry()).forAllStatesExcept(state -> {
                Direction facing = state.getValue(PlaceMatAttachedStairsBlock.FACING);
                StairsShape shape = state.getValue(PlaceMatAttachedStairsBlock.SHAPE);
                boolean attached = state.getValue(PlaceMatAttachedStairsBlock.ATTACHED);

                int yRot = (int) facing.getClockWise().toYRot();
                if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                    yRot += 270;
                }
                yRot %= 360;

                var chosenModel = shape == StairsShape.STRAIGHT
                    ? (attached ? straightAttachedModel : straightModel)
                    : (shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT
                    ? (attached ? innerAttachedModel : innerModel)
                    : (attached ? outerAttachedModel : outerModel));

                return ConfiguredModel.builder()
                    .modelFile(chosenModel)
                    .rotationY(yRot)
                    .uvLock(true)
                    .build();
            }, PlaceMatBlock.LOCKED);
        };
    }

    /**
     * Helper method for creating multipart table blockstates with an unconditional base tabletop
     * and conditional corner legs based on horizontal neighbor connections.
     * @param textures TextureConfig list.
     * @param blockName Name of the table type.
     * @return Blockstate provider.
     */
    public static <T extends PlaceMatTableBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> tableBlockState(
        List<TextureConfig> textures,
        String blockName) {

        return (ctx, prov) -> {
            var baseModel = prov.models().withExistingParent(ctx.getName() + "_base", prov.modLoc("block/" + blockName + "_base_parent"));
            var legModel = prov.models().withExistingParent(ctx.getName() + "_leg", prov.modLoc("block/" + blockName + "_leg_parent"));
            var itemModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/" + blockName + "_parent"));

            var allModels = new BlockModelBuilder[]{ baseModel, legModel, itemModel };
            for (var model : allModels) {
                for (TextureConfig texture : textures) {
                    model.texture(texture.index(), texture.resource());
                }
            }

            var builder = prov.getMultipartBuilder(ctx.getEntry());

            // Base Tabletop Model
            builder.part().modelFile(baseModel).addModel();

            // NW Leg (rot 0)
            builder.part().modelFile(legModel).rotationY(0).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.WEST, false);

            // NE Leg (rot 90)
            builder.part().modelFile(legModel).rotationY(90).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.EAST, false);

            // SE Leg (rot 180)
            builder.part().modelFile(legModel).rotationY(180).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.EAST, false);

            // SW Leg (rot 270)
            builder.part().modelFile(legModel).rotationY(270).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.WEST, false);
        };
    }

    /**
     * Helper method for creating multipart apron table blockstates with an unconditional base tabletop,
     * corner legs, and perimeters.
     * @param textures TextureConfig list.
     * @param blockName Name of the table type.
     * @return Blockstate provider.
     */
    public static <T extends PlaceMatApronTableBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> apronTableBlockState(
        List<TextureConfig> textures,
        String blockName) {

        return (ctx, prov) -> {
            var baseModel = prov.models().withExistingParent(ctx.getName() + "_base", prov.modLoc("block/" + blockName + "_base_parent"));
            var legModel = prov.models().withExistingParent(ctx.getName() + "_leg", prov.modLoc("block/" + blockName + "_leg_parent"));
            var sideModel = prov.models().withExistingParent(ctx.getName() + "_side", prov.modLoc("block/" + blockName + "_side_parent"));
            var innerCornerModel = prov.models().withExistingParent(ctx.getName() + "_inner_corner", prov.modLoc("block/" + blockName + "_inner_corner_parent"));
            var outerCornerFillerModel = prov.models().withExistingParent(ctx.getName() + "_outer_corner_filler", prov.modLoc("block/" + blockName + "_outer_corner_filler_parent"));
            var outerCornerFiller2Model = prov.models().withExistingParent(ctx.getName() + "_outer_corner_filler2", prov.modLoc("block/" + blockName + "_outer_corner_filler2_parent"));
            var itemModel = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/" + blockName + "_parent"));

            var allModels = new BlockModelBuilder[]{ baseModel, legModel, sideModel, innerCornerModel, outerCornerFillerModel, outerCornerFiller2Model, itemModel };
            for (var model : allModels) {
                for (TextureConfig texture : textures) {
                    model.texture(texture.index(), texture.resource());
                }
            }

            var builder = prov.getMultipartBuilder(ctx.getEntry());

            // Base Tabletop Model
            builder.part().modelFile(baseModel).addModel();

            // NW Leg (rot 0)
            builder.part().modelFile(legModel).rotationY(0).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.WEST, false);

            // NE Leg (rot 90)
            builder.part().modelFile(legModel).rotationY(90).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.EAST, false);

            // SE Leg (rot 180)
            builder.part().modelFile(legModel).rotationY(180).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.EAST, false);

            // SW Leg (rot 270)
            builder.part().modelFile(legModel).rotationY(270).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.WEST, false);

            // West Side (rot 0)
            builder.part().modelFile(sideModel).rotationY(0).addModel()
                .condition(PlaceMatTableBlock.WEST, false)
                .condition(PlaceMatTableBlock.NORTH, true)
                .condition(PlaceMatTableBlock.SOUTH, true);

            // North Side (rot 90)
            builder.part().modelFile(sideModel).rotationY(90).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.WEST, true)
                .condition(PlaceMatTableBlock.EAST, true);

            // East Side (rot 180)
            builder.part().modelFile(sideModel).rotationY(180).addModel()
                .condition(PlaceMatTableBlock.EAST, false)
                .condition(PlaceMatTableBlock.NORTH, true)
                .condition(PlaceMatTableBlock.SOUTH, true);

            // South Side (rot 270)
            builder.part().modelFile(sideModel).rotationY(270).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.WEST, true)
                .condition(PlaceMatTableBlock.EAST, true);

            // NW Filler (rot 0)
            builder.part().modelFile(outerCornerFillerModel).rotationY(0).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.WEST, false)
                .condition(PlaceMatTableBlock.SOUTH, true);

            // NW Filler2 (rot 90)
            builder.part().modelFile(outerCornerFiller2Model).rotationY(90).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.WEST, false)
                .condition(PlaceMatTableBlock.EAST, true);

            // NE Filler (rot 90)
            builder.part().modelFile(outerCornerFillerModel).rotationY(90).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.EAST, false)
                .condition(PlaceMatTableBlock.WEST, true);

            // NE Filler2 (rot 180)
            builder.part().modelFile(outerCornerFiller2Model).rotationY(180).addModel()
                .condition(PlaceMatTableBlock.NORTH, false)
                .condition(PlaceMatTableBlock.EAST, false)
                .condition(PlaceMatTableBlock.SOUTH, true);

            // SE Filler (rot 180)
            builder.part().modelFile(outerCornerFillerModel).rotationY(180).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.EAST, false)
                .condition(PlaceMatTableBlock.NORTH, true);

            // SE Filler2 (rot 270)
            builder.part().modelFile(outerCornerFiller2Model).rotationY(270).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.EAST, false)
                .condition(PlaceMatTableBlock.WEST, true);

            // SW Filler (rot 270)
            builder.part().modelFile(outerCornerFillerModel).rotationY(270).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.WEST, false)
                .condition(PlaceMatTableBlock.EAST, true);

            // SW Filler2 (rot 0)
            builder.part().modelFile(outerCornerFiller2Model).rotationY(0).addModel()
                .condition(PlaceMatTableBlock.SOUTH, false)
                .condition(PlaceMatTableBlock.WEST, false)
                .condition(PlaceMatTableBlock.NORTH, true);

            // SE Inner Corner (rot 0)
            builder.part().modelFile(innerCornerModel).rotationY(0).addModel()
                .condition(PlaceMatTableBlock.EAST, true)
                .condition(PlaceMatTableBlock.SOUTH, true)
                .condition(PlaceMatApronTableBlock.SOUTH_EAST, false);

            // SW Inner Corner (rot 90)
            builder.part().modelFile(innerCornerModel).rotationY(90).addModel()
                .condition(PlaceMatTableBlock.SOUTH, true)
                .condition(PlaceMatTableBlock.WEST, true)
                .condition(PlaceMatApronTableBlock.SOUTH_WEST, false);

            // NW Inner Corner (rot 180)
            builder.part().modelFile(innerCornerModel).rotationY(180).addModel()
                .condition(PlaceMatTableBlock.WEST, true)
                .condition(PlaceMatTableBlock.NORTH, true)
                .condition(PlaceMatApronTableBlock.NORTH_WEST, false);

            // NE Inner Corner (rot 270)
            builder.part().modelFile(innerCornerModel).rotationY(270).addModel()
                .condition(PlaceMatTableBlock.NORTH, true)
                .condition(PlaceMatTableBlock.EAST, true)
                .condition(PlaceMatApronTableBlock.NORTH_EAST, false);
        };
    }

    /* ===================================== Placement Range Helpers ==================================== */

    public record BoxShapeSet(VoxelShape bounds, VoxelShape collision, VoxelShape bottom, VoxelShape top) {}

    /**
     * Creates a double shelf box shape set for Bounds, collision, bottom, and top.
     * Assumes shelf heights are 2px tall and 6px apart.
     * @return bounds with height 16, collision with height 10, bottom with height 8, and top with height 16.
     */
    private static BoxShapeSet createDoubleShelfBoxSet(double minX, double minZ, double maxX, double maxZ) {
        return new BoxShapeSet(
            Block.box(minX, 0,  minZ, maxX, 16, maxZ),
            Block.box(minX, 0,  minZ, maxX, 10, maxZ),
            Block.box(minX, 2,  minZ, maxX, 8,  maxZ),
            Block.box(minX, 10, minZ, maxX, 16, maxZ)
        );
    }

    // Shelves shape sets take up half a block like a vertical slab. And have two shelves.
    /**
     <p>◼◼
     <p>☐☐
    */
    public static final BoxShapeSet SHELVES_FULL = createDoubleShelfBoxSet(0, 0, 16, 8);
    /**
     <p>☐☐
     <p>◼☐
    */
    public static final BoxShapeSet SHELVES_INNER_LEFT = createDoubleShelfBoxSet(0, 8, 8, 16);
    /**
     <p>☐☐
     <p>☐◼
    */
    public static final BoxShapeSet SHELVES_INNER_RIGHT = createDoubleShelfBoxSet(8, 8, 16, 16);
    /**
     <p>◼☐
     <p>☐☐
    */
    public static final BoxShapeSet SHELVES_OUTER_LEFT = createDoubleShelfBoxSet(0, 0, 8, 8);
    /**
     <p>☐◼
     <p>☐☐
    */
    public static final BoxShapeSet SHELVES_OUTER_RIGHT = createDoubleShelfBoxSet(8, 0, 16, 8);

    /**
     * Constructs a basic placement range for a place mat block with default values.
     * @param shape The voxel shape of the placement range.
     * @param maxHeight The maximum height of the placement range.
     * @param maxStackSize The maximum stack size for items in the placement range.
     * @return A new PlaceMatBlock.PlacementRange instance.
     */
    public static PlacementRange basicPlacementRange(VoxelShape shape, float maxHeight, int maxStackSize) {
        return new PlacementRange(
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
     * @return A new PlacementRange instance.
     */
    public static PlacementRange basicPlacementRange(VoxelShape shape, float maxHeight) {
        return basicPlacementRange(shape, maxHeight, 16);
    }

    /**
     * Constructs a chair placement range for a place mat block.
     * @param shape The voxel shape of the chair placement range.
     * @return A new PlacementRange instance configured as a chair zone.
     */
    public static PlacementRange chairPlacementRange(VoxelShape shape) {
        return new PlacementRange(
            shape,
            (float) shape.max(Direction.Axis.Y),
            true, true, true, true, false, true, null, true, true,
            0,
            null, true, true, true, true, 1.0f, 0, 0, 0, 0, true);
    }

    public static <T extends PlaceMatStairsBlock> NonNullConsumer<T> stairsShelvesShapes() {
        return block -> {
            float bottomHeight = 8 / 16F;
            float topHeight = 16 / 16F;

            // Full
            block.cardinalBoundingBox(SHELVES_FULL.bounds);
            block.cardinalCollisionShape(SHELVES_FULL.collision);
            block.addRange(0, basicPlacementRange(SHELVES_FULL.bottom, bottomHeight));
            block.addRange(1, basicPlacementRange(SHELVES_FULL.top, topHeight));

            // Inner Left
            block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT), SHELVES_FULL.bounds, SHELVES_INNER_LEFT.bounds);
            block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT), SHELVES_FULL.collision, SHELVES_INNER_LEFT.collision);
            block.adjustRange(2, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT), SHELVES_INNER_LEFT.bottom, bottomHeight);
            block.adjustRange(3, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT), SHELVES_INNER_LEFT.top, topHeight);

            // Inner Right
            block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_RIGHT), SHELVES_FULL.bounds, SHELVES_INNER_RIGHT.bounds);
            block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_RIGHT), SHELVES_FULL.collision, SHELVES_INNER_RIGHT.collision);
            block.addRange(2, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT), basicPlacementRange(SHELVES_INNER_RIGHT.bottom, bottomHeight));
            block.addRange(3, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.INNER_LEFT, StairsShape.INNER_RIGHT), basicPlacementRange(SHELVES_INNER_RIGHT.top, topHeight));

            // Outer Left
            block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT), SHELVES_OUTER_LEFT.bounds);
            block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT), SHELVES_OUTER_LEFT.collision);
            block.adjustRange(0, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT), SHELVES_OUTER_LEFT.bottom, bottomHeight);
            block.adjustRange(1, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_LEFT), SHELVES_OUTER_LEFT.top, topHeight);

            // Outer Right
            block.cardinalBoundingBox(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT), SHELVES_OUTER_RIGHT.bounds);
            block.cardinalCollisionShape(PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT), SHELVES_OUTER_RIGHT.collision);
            block.adjustRange(0, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT), SHELVES_OUTER_RIGHT.bottom, bottomHeight);
            block.adjustRange(1, PlaceMatStairsBlock.SHAPE, List.of(StairsShape.OUTER_RIGHT), SHELVES_OUTER_RIGHT.top, topHeight);

        };
    }
}

package net.placemats.common.datagen.definitions;

/**
 * Premade definitions for common items
 */
@SuppressWarnings("unused")
public class DefinitionHelpers {

    /**
     * Generates a standard 3x3 food Definition. Used by a lot of fruits.
     * <li>Size: 0.19f
     * <li>Scale: 1f
     * <li>Flat: false
     * <li>Rotten: false
     * @param id The item id string (e.g., "tfc:food/red_apple")
     * @param namespace The item namespace string (e.g., "tfc")
     * @param name The food name string (e.g., "red_apple", "green_apple")
     * @return A pre-configured Definition object.
     */
    public static Definition food3x3(String id, String namespace, String name) {
        String modelPath = "place_mats:render/" + namespace + "/food/" + name;
        String texturePath = "place_mats:item/render/" + namespace + "/food/" + name;

        return new Definition(id)
            .size(0.19f, 0.19f)
            .scale(1f)
            .flat(false)
            .model(Definition.parentModel(modelPath, "place_mats:render/3x3_food", new String[]{"0", texturePath}));
    }

    /**
     * Generates a standard 3x3 food Definition. Used by a lot of fruits.
     * <li>Size: 0.19f
     * <li>Scale: 1f
     * <li>Flat: false
     * <li>Rotten: false
     * @param id The item id string (e.g., "minecraft:apple")
     * @param name The food name string (e.g., "apple")
     * @return A pre-configured Definition object.
     */
    public static Definition minecraftFood3x3(String id, String name) {
        return food3x3(id, "minecraft", name);
    }

    /**
     * Generates a standard 3x3 food Definition. Used by a lot of fruits.
     * <li>Size: 0.19f
     * <li>Scale: 1f
     * <li>Flat: false
     * <li>Rotten: true
     * @param id The item id string (e.g., "tfc:food/red_apple")
     * @param namespace The item namespace string (e.g., "tfc")
     * @param name The food name string (e.g., "red_apple", "green_apple")
     * @return A pre-configured Definition object.
     */
    public static Definition rottenFood3x3(String id, String namespace, String name) {
        String modelPath = "place_mats:render/" + namespace + "/food/" + name;
        String texturePath = "place_mats:item/render/" + namespace + "/food/" + name;

        String modelRottenPath = "place_mats:render/" + namespace + "/food/" + name + "_rotten";
        String textureRottenPath = "place_mats:item/render/" + namespace + "/food/" + name + "_rotten";

        return new Definition(id)
            .size(0.19f, 0.19f)
            .scale(1f)
            .flat(false)
            .model(Definition.parentModel(modelPath, "place_mats:render/3x3_food", new String[]{"0", texturePath}))
            .condition(Condition.isRotten(), d -> d.model(Definition.parentModel(modelRottenPath, "place_mats:render/3x3_food", new String[]{"0", textureRottenPath})));
    }

    /**
     * Generates a standard 3x3 food Definition. Used by a lot of fruits.
     * <li>Size: 0.19f
     * <li>Scale: 1f
     * <li>Flat: false
     * <li>Rotten: true
     * @param id The item id string (e.g., "tfc:food/red_apple")
     * @param name The food name string (e.g., "red_apple", "green_apple")
     * @return A pre-configured Definition object.
     */
    public static Definition tfcFood3x3(String id, String name) {
        return rottenFood3x3(id, "tfc", name);
    }

    public static Definition bucket(String id, String namespace, String name, boolean isWooden) {
        String modelPath = "place_mats:render/" + namespace + "/" + name;
        String texturePath;
        if (isWooden) {
            texturePath = "place_mats:item/render/buckets/wood_bucket";
        } else {
            texturePath = "place_mats:item/render/buckets/iron_bucket";
        }

        return new Definition(id)
            .size(5/16f, 5/16f, 6/16f)
            .pos(0, -0.26f, 0.075f)
            .scale(1f)
            .flat(false)
            //.fluidTextureIndex(1)
            .containsFluid(c -> c
                .model(Definition.parentModel(modelPath + "_filled", "place_mats:render/bucket_parent",
                    new String[]{"0", texturePath},
                    new String[]{"1", "place_mats:item/render/white_test"}
                ))
            )
            .model(Definition.parentModel(modelPath, "place_mats:render/bucket_parent",
                new String[]{"0", texturePath},
                new String[]{"1", "place_mats:item/render/empty"}
            ));
    }
}

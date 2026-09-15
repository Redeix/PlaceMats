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
            .modelRotten(Definition.parentModel(modelRottenPath, "place_mats:render/3x3_food", new String[]{"0", textureRottenPath}));
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
}

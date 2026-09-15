package net.placemats.common.datagen.definitions;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Definition {
    public final String id;
    public float width = 0.25f;
    public float depth = 0.25f;
    public String model;
    public String modelRotten;
    public ModelData modelData;
    public ModelData modelRottenData;
    public Float height;
    public Float scale;
    public Boolean flat;
    public Boolean stackable;
    public Boolean allowsStackingOnTop;

    public Definition(String id) {
        this.id = id;
    }

    public Definition size(float width, float depth) {
        this.width = width;
        this.depth = depth;
        return this;
    }

    public Definition model(String model) {
        this.model = model;
        return this;
    }

    public Definition model(ModelData modelData) {
        this.modelData = modelData;
        if (modelData.path() != null) {
            this.model = modelData.path();
        }
        return this;
    }

    public Definition modelRotten(String modelRotten) {
        this.modelRotten = modelRotten;
        return this;
    }

    public Definition modelRotten(ModelData modelRottenData) {
        this.modelRottenData = modelRottenData;
        if (modelRottenData.path() != null) {
            this.modelRotten = modelRottenData.path();
        }
        return this;
    }

    public static ModelData parentModel(String parent, String[]... textures) {
        return parentModel(null, parent, textures);
    }

    public static ModelData parentModel(String path, String parent, String[]... textures) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String[] pair : textures) {
            if (pair.length >= 2) {
                map.put(pair[0], pair[1]);
            }
        }
        return new ModelData(path, parent, map);
    }

    public Definition height(float height) {
        this.height = height;
        return this;
    }

    public Definition scale(float scale) {
        this.scale = scale;
        return this;
    }

    public Definition flat(boolean flat) {
        this.flat = flat;
        return this;
    }

    public Definition stackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public Definition allowsStackingOnTop(boolean allowsStackingOnTop) {
        this.allowsStackingOnTop = allowsStackingOnTop;
        return this;
    }
}

package net.placemats.common.datagen.definitions;

@SuppressWarnings("unused")
public class Definition {
    public final String id;
    public float width = 0.25f;
    public float depth = 0.25f;
    public String model;
    public String modelRotten;
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

    public Definition modelRotten(String modelRotten) {
        this.modelRotten = modelRotten;
        return this;
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

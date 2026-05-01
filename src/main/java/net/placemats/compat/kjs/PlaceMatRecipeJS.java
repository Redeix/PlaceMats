package net.placemats.compat.kjs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.util.MapJS;

public class PlaceMatRecipeJS extends RecipeJS {

    public PlaceMatRecipeJS block(String block) {
        json.addProperty("block", block);
        return this;
    }

    public PlaceMatRecipeJS zoneIndex(int index) {
        json.addProperty("zone_index", index);
        return this;
    }

    public PlaceMatRecipeJS input(Object input, int count) {
        json.add("input", parseJson(input));
        json.addProperty("input_count", count);
        return this;
    }

    public PlaceMatRecipeJS input(Object input) {
        if (input instanceof String s) {
            Matcher matcher = Pattern.compile("^(\\d+)\\s*x\\s*(.*)$").matcher(s);
            if (matcher.matches()) {
                return input(matcher.group(2), Integer.parseInt(matcher.group(1)));
            }
        }
        return input(input, 1);
    }

    public PlaceMatRecipeJS targetInput(Object input, int count) {
        json.add("target_input", parseJson(input));
        json.addProperty("target_input_count", count);
        return this;
    }

    public PlaceMatRecipeJS targetInput(Object input) {
        if (input instanceof String s) {
            Matcher matcher = Pattern.compile("^(\\d+)\\s*x\\s*(.*)$").matcher(s);
            if (matcher.matches()) {
                return targetInput(matcher.group(2), Integer.parseInt(matcher.group(1)));
            }
        }
        return targetInput(input, 1);
    }

    public PlaceMatRecipeJS result(Object result, int count) {
        JsonObject jo = new JsonObject();
        if (result instanceof String s) {
            jo.addProperty("stack", s);
        } else {
            jo.add("stack", parseJson(result));
        }
        jo.addProperty("count", count);
        json.add("result", jo);
        return this;
    }

    public PlaceMatRecipeJS result(Object result) {
        if (result instanceof String s) {
            Matcher matcher = Pattern.compile("^(\\d+)\\s*x\\s*(.*)$").matcher(s);
            if (matcher.matches()) {
                return result(matcher.group(2), Integer.parseInt(matcher.group(1)));
            }
        }
        json.add("result", parseJson(result));
        return this;
    }

    public PlaceMatRecipeJS sound(String sound) {
        json.addProperty("sound", sound);
        return this;
    }

    public PlaceMatRecipeJS volume(float volume) {
        json.addProperty("volume", volume);
        return this;
    }

    public PlaceMatRecipeJS pitch(float pitch) {
        json.addProperty("pitch", pitch);
        return this;
    }

    private JsonElement parseJson(Object o) {
        if (o instanceof JsonElement je) {
            return je;
        } else if (o instanceof java.util.Map<?, ?> map) {
            return MapJS.json(map);
        } else if (o instanceof String s) {
            if (s.startsWith("{") || s.startsWith("[")) {
                try {
                    return com.google.gson.JsonParser.parseString(s);
                } catch (Exception ignored) {
                }
            }
            JsonObject jo = new JsonObject();
            if (s.startsWith("#")) {
                jo.addProperty("tag", s.substring(1));
            } else {
                jo.addProperty("item", s);
            }
            return jo;
        } else if (o != null && o.getClass().getName().contains("ItemStackProviderJS")) {
            // Support TFC ItemStackProviderJS if it has a toJson method.
            try {
                var method = o.getClass().getMethod("toJson");
                return (JsonElement) method.invoke(o);
            } catch (Exception ignored) {
            }
        }

        JsonObject jo = new JsonObject();
        jo.addProperty("item", o != null ? o.toString() : "minecraft:air");
        return jo;
    }
}

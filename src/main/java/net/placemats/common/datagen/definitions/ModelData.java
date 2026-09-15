package net.placemats.common.datagen.definitions;

import java.util.Map;

@SuppressWarnings("unused")
public record ModelData(String path, String parent, Map<String, String> textures)
{
    public ModelData(String parent, Map<String, String> textures)
    {
        this(null, parent, textures);
    }

}

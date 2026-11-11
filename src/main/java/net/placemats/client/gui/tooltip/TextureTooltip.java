package net.placemats.client.gui.tooltip;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record TextureTooltip(List<ResourceLocation> textures) implements TooltipComponent {

    public TextureTooltip {
        textures = List.copyOf(textures);
    }
}

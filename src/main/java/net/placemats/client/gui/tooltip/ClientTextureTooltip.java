package net.placemats.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

/**
 * A tooltip that displays a grid of textures.
 * Based on the Bundle tooltip class.
 */
@SuppressWarnings("unused")
public class ClientTextureTooltip implements ClientTooltipComponent {

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft","textures/gui/container/bundle.png");
    private static final int SLOT_SIZE_X = 18;
    private static final int SLOT_SIZE_Y = 20;
    private static final float[] SLOT_COLOR = {0.3F, 0.67F, 0.52F, 1.0F};
    private static final int MARGIN_Y = 4;
    private static final int TEX_SIZE = 128;
    private static final int MAX_COLUMNS = 5;

    private final List<ResourceLocation> textures;

    public ClientTextureTooltip(TextureTooltip tooltip) {
        this.textures = tooltip.textures();
    }

    public ClientTextureTooltip(List<ResourceLocation> textures) {
        this.textures = textures;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return this.gridSizeX() * SLOT_SIZE_X + 2;
    }

    @Override
    public int getHeight() {
        return this.gridSizeY() * SLOT_SIZE_Y + 2 + MARGIN_Y;
    }

    private int gridSizeX() {
        return Math.max(1, Math.min(MAX_COLUMNS, this.textures.size()));
    }

    private int gridSizeY() {
        return Math.max(1, (int) Math.ceil((double) this.textures.size() / (double) this.gridSizeX()));
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
        int gridX = this.gridSizeX();
        int gridY = this.gridSizeY();
        int index = 0;

        for (int row = 0; row < gridY; ++row) {
            for (int col = 0; col < gridX; ++col) {
                int slotX = x + col * SLOT_SIZE_X + 1;
                int slotY = y + row * SLOT_SIZE_Y + 1;
                ResourceLocation tex = index < this.textures.size() ? this.textures.get(index) : null;
                this.renderSlot(slotX, slotY, tex, guiGraphics);
                index++;
            }
        }
        guiGraphics.setColor(SLOT_COLOR[0], SLOT_COLOR[1], SLOT_COLOR[2], SLOT_COLOR[3]);
        this.drawBorder(x, y, gridX, gridY, guiGraphics);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderSlot(int slotX, int slotY, ResourceLocation textureLocation, GuiGraphics guiGraphics) {
        // Background Color
        guiGraphics.setColor(SLOT_COLOR[0], SLOT_COLOR[1], SLOT_COLOR[2], SLOT_COLOR[3]);
        this.blit(guiGraphics, slotX, slotY, Texture.SLOT);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (textureLocation != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLocation);
            if (sprite != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                // Render Textures
                guiGraphics.blit(slotX + 1, slotY + 1, 0, 16, 16, sprite);
            }
        }
    }

    private void drawBorder(int x, int y, int gridX, int gridY, GuiGraphics guiGraphics) {
        // Top Corners
        this.blit(guiGraphics, x, y, Texture.BORDER_CORNER_TOP);
        this.blit(guiGraphics, x + gridX * SLOT_SIZE_X + 1, y, Texture.BORDER_CORNER_TOP);

        // Top & Bottom Border
        for (int col = 0; col < gridX; ++col) {
            this.blit(guiGraphics, x + 1 + col * SLOT_SIZE_X, y, Texture.BORDER_HORIZONTAL_TOP);
            this.blit(guiGraphics, x + 1 + col * SLOT_SIZE_X, y + gridY * SLOT_SIZE_Y, Texture.BORDER_HORIZONTAL_BOTTOM);
        }

        // Left & Right Border
        for (int row = 0; row < gridY; ++row) {
            this.blit(guiGraphics, x, y + 1 + row * SLOT_SIZE_Y, Texture.BORDER_VERTICAL);
            this.blit(guiGraphics, x + gridX * SLOT_SIZE_X + 1, y + row * SLOT_SIZE_Y, Texture.BORDER_VERTICAL);
        }

        // Bottom Corners
        this.blit(guiGraphics, x, y + gridY * SLOT_SIZE_Y, Texture.BORDER_CORNER_BOTTOM);
        this.blit(guiGraphics, x + gridX * SLOT_SIZE_X + 1, y + gridY * SLOT_SIZE_Y, Texture.BORDER_CORNER_BOTTOM);

    }

    private void blit(GuiGraphics guiGraphics, int x, int y, Texture texture) {
        guiGraphics.blit(TEXTURE_LOCATION, x, y, 0, (float) texture.x, (float) texture.y, texture.w, texture.h, TEX_SIZE, TEX_SIZE);
    }

    private enum Texture {
        SLOT(0, 0, 18, 20),
        BLOCKED_SLOT(0, 40, 18, 20),
        BORDER_VERTICAL(0, 18, 1, 20),
        BORDER_HORIZONTAL_TOP(0, 20, 18, 1),
        BORDER_HORIZONTAL_BOTTOM(0, 60, 18, 1),
        BORDER_CORNER_TOP(0, 20, 1, 1),
        BORDER_CORNER_BOTTOM(0, 60, 1, 1);

        public final int x;
        public final int y;
        public final int w;
        public final int h;

        Texture(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }
}

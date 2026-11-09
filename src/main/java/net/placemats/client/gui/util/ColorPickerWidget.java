package net.placemats.client.gui.util;

import java.util.function.Consumer;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.placemats.PlaceMatMain;
import net.placemats.client.gui.CabinetMakerTableScreen;
import org.jetbrains.annotations.NotNull;

/**
 * HSVA Color Picker Widget.
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class ColorPickerWidget extends AbstractWidget {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "textures/gui/widget/color_picker_widget.png");
    public static final int TEXTURE_SIZE = 128;

    public static final int TAB_WIDTH = 68;
    public static final int TAB_HEIGHT = 127;
    public static final int TAB_U = 0;
    public static final int TAB_V = 0;

    private static final int COLOR_PREVIEW_X = 3;
    private static final int COLOR_PREVIEW_Y = 11;
    private static final int COLOR_PREVIEW_SIZE = 8;

    private static final int COLOR_TEXT_X = COLOR_PREVIEW_X + COLOR_PREVIEW_SIZE + 3;
    private static final int COLOR_TEXT_Y = COLOR_PREVIEW_Y;
    private static final int COLOR_TEXT_WIDTH = 48;

    private static final int SLIDER_BAR_U = TAB_WIDTH;
    private static final int SLIDER_BAR_V = 0;
    private static final int SLIDER_BAR_WIDTH = 6;
    private static final int SLIDER_BAR_HEIGHT = 3;

    private static final int COLOR_BAR_WIDTH = 2;
    private static final int COLOR_BAR_HEIGHT = 76;
    private static final int COLOR_BAR_SPACING = COLOR_BAR_WIDTH + 14;

    private static final int H_BAR_X = 8;
    private static final int H_BAR_Y = COLOR_PREVIEW_Y + COLOR_PREVIEW_SIZE + 17;
    private static final int S_BAR_X = H_BAR_X + COLOR_BAR_SPACING;
    private static final int S_BAR_Y = H_BAR_Y;
    private static final int V_BAR_X = S_BAR_X + COLOR_BAR_SPACING;
    private static final int V_BAR_Y = H_BAR_Y;
    private static final int A_BAR_X = V_BAR_X + COLOR_BAR_SPACING;
    private static final int A_BAR_Y = H_BAR_Y;

    public static final int OPEN_TAB_BUTTON_U = SLIDER_BAR_U;
    public static final int OPEN_TAB_BUTTON_V = SLIDER_BAR_V + SLIDER_BAR_HEIGHT;
    public static final int OPEN_TAB_WIDTH = 16;
    public static final int OPEN_TAB_HEIGHT = 7;
    public static final int CLOSE_TAB_BUTTON_U = OPEN_TAB_BUTTON_U;
    public static final int CLOSE_TAB_BUTTON_V = OPEN_TAB_BUTTON_V + OPEN_TAB_HEIGHT;

    private final Font font;
    @Getter private final EditBox hexEditBox;
    @Setter private Consumer<Integer> colorChangeListener;

    @Getter private float hue = 0.0f;
    @Getter private float saturation = 1.0f;
    @Getter private float value = 1.0f;
    @Getter private float alpha = 1.0f;
    @Getter private int currentColor = 0xFFFFFFFF;

    private SliderType activeSlider = SliderType.NONE;
    private boolean updatingFromEditBox = false;
    private boolean isDraggingText = false;
    private long lastTextClickTime = 0L;

    private enum SliderType {
        NONE, HUE, SATURATION, VALUE, ALPHA
    }

    public ColorPickerWidget(Font font, int x, int y, Consumer<Integer> colorChangeListener) {
        super(x, y, TAB_WIDTH, TAB_HEIGHT, Component.empty());
        this.font = font;
        this.colorChangeListener = colorChangeListener;
        this.hexEditBox = new EditBox(font, x + COLOR_TEXT_X, y + COLOR_TEXT_Y, COLOR_TEXT_WIDTH, 10, Component.literal(""));
        this.hexEditBox.setBordered(false);
        this.hexEditBox.setMaxLength(8);
        this.hexEditBox.setTextColor(0xFFFFFF);
        this.hexEditBox.setResponder(this::onHexInputChanged);
        setColor(0xFFFFFFFF, true);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        if (this.hexEditBox != null) {
            this.hexEditBox.setX(x + COLOR_TEXT_X);
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        if (this.hexEditBox != null) {
            this.hexEditBox.setY(y + COLOR_TEXT_Y);
        }
    }

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    public void setColor(int argb) {
        setColor(argb, true);
    }

    public void setColor(int argb, boolean updateEditBox) {
        this.currentColor = argb;
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0.0f;
        if (delta > 1e-5f) {
            if (max == r) {
                h = ((g - b) / delta) % 6.0f;
            } else if (max == g) {
                h = ((b - r) / delta) + 2.0f;
            } else {
                h = ((r - g) / delta) + 4.0f;
            }
            h /= 6.0f;
            if (h < 0.0f) {
                h += 1.0f;
            }
        }
        float s = (max <= 1e-5f) ? 0.0f : (delta / max);
        float v = max;

        this.hue = h;
        this.saturation = s;
        this.value = v;
        this.alpha = a;

        if (updateEditBox && !this.updatingFromEditBox) {
            this.updatingFromEditBox = true;
            this.hexEditBox.setValue(String.format("%08X", argb));
            this.updatingFromEditBox = false;
        }
    }

    private void onHexInputChanged(String text) {
        if (this.updatingFromEditBox) {
            return;
        }
        Integer parsed = CabinetMakerTableScreen.parseHexColor(text);
        if (parsed != null) {
            this.updatingFromEditBox = true;
            setColor(parsed, false);
            this.updatingFromEditBox = false;
            if (this.colorChangeListener != null) {
                this.colorChangeListener.accept(this.currentColor);
            }
        }
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue >= 1.0f || hue < 0.0f) ? (hue % 1.0f + 1.0f) % 1.0f : hue;
        if (h >= 1.0f) {
            h = 0.0f;
        }
        return Mth.hsvToRgb(h, saturation, value);
    }

    private void recalculateColorFromHsv() {
        int rgb = hsvToRgb(this.hue, this.saturation, this.value);
        int a = Math.round(this.alpha * 255.0f);
        this.currentColor = (a << 24) | (rgb & 0xFFFFFF);

        if (!this.updatingFromEditBox) {
            this.updatingFromEditBox = true;
            this.hexEditBox.setValue(String.format("%08X", this.currentColor));
            this.updatingFromEditBox = false;
        }

        if (this.colorChangeListener != null) {
            this.colorChangeListener.accept(this.currentColor);
        }
    }

    private boolean isMouseOverBar(double mouseX, double mouseY, int barRelX) {
        int barX = getX() + barRelX;
        int barY = getY() + H_BAR_Y;
        return mouseX >= barX - 4 && mouseX <= barX + COLOR_BAR_WIDTH + 3 && mouseY >= barY - 1 && mouseY <= barY + COLOR_BAR_HEIGHT + 1;
    }

    private void updateSliderFromMouse(double mouseY, SliderType type) {
        int barY = getY() + H_BAR_Y;
        float val = 1.0f - (float) (mouseY - barY) / (float) (COLOR_BAR_HEIGHT - 1);
        val = Mth.clamp(val, 0.0f, 1.0f);
        switch (type) {
            case HUE -> this.hue = val;
            case SATURATION -> this.saturation = val;
            case VALUE -> this.value = val;
            case ALPHA -> this.alpha = val;
            default -> {}
        }
        recalculateColorFromHsv();
    }

    public void tick() {
        if (this.hexEditBox != null) {
            this.hexEditBox.tick();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) {
            return false;
        }
        if (button == 0) {
            int tbX = getX() + COLOR_TEXT_X;
            int tbY = getY() + COLOR_TEXT_Y;
            boolean isOverTextBox = mouseX >= tbX - 2 && mouseX <= tbX + COLOR_TEXT_WIDTH + 4 && mouseY >= tbY - 2 && mouseY <= tbY + 12;
            if (isOverTextBox) {
                long now = Util.getMillis();
                if (now - this.lastTextClickTime < 300L) {
                    this.hexEditBox.moveCursorToEnd();
                    this.hexEditBox.setHighlightPos(0);
                } else {
                    this.hexEditBox.mouseClicked(mouseX, mouseY, button);
                }
                this.lastTextClickTime = now;
                this.hexEditBox.setFocused(true);
                this.isDraggingText = true;
                return true;
            }
            this.hexEditBox.setFocused(false);
            this.isDraggingText = false;
            if (isMouseOverBar(mouseX, mouseY, H_BAR_X)) {
                this.activeSlider = SliderType.HUE;
                updateSliderFromMouse(mouseY, SliderType.HUE);
                return true;
            }
            if (isMouseOverBar(mouseX, mouseY, S_BAR_X)) {
                this.activeSlider = SliderType.SATURATION;
                updateSliderFromMouse(mouseY, SliderType.SATURATION);
                return true;
            }
            if (isMouseOverBar(mouseX, mouseY, V_BAR_X)) {
                this.activeSlider = SliderType.VALUE;
                updateSliderFromMouse(mouseY, SliderType.VALUE);
                return true;
            }
            if (isMouseOverBar(mouseX, mouseY, A_BAR_X)) {
                this.activeSlider = SliderType.ALPHA;
                updateSliderFromMouse(mouseY, SliderType.ALPHA);
                return true;
            }
            return mouseX >= getX() && mouseX < getX() + TAB_WIDTH && mouseY >= getY() && mouseY < getY() + TAB_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible || !this.active) {
            return false;
        }
        if (this.activeSlider != SliderType.NONE) {
            updateSliderFromMouse(mouseY, this.activeSlider);
            return true;
        }
        if (this.isDraggingText && button == 0) {
            int relX = Mth.floor(mouseX) - this.hexEditBox.getX();
            String val = this.hexEditBox.getValue();
            String visibleText = this.font.plainSubstrByWidth(val, this.hexEditBox.getWidth());
            int newPos = this.font.plainSubstrByWidth(visibleText, relX).length();
            this.hexEditBox.setHighlightPos(Mth.clamp(newPos, 0, val.length()));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) {
            return false;
        }
        this.isDraggingText = false;
        if (this.activeSlider != SliderType.NONE) {
            this.activeSlider = SliderType.NONE;
            return true;
        }
        int tbX = getX() + COLOR_TEXT_X;
        int tbY = getY() + COLOR_TEXT_Y;
        if (mouseX >= tbX - 2 && mouseX <= tbX + COLOR_TEXT_WIDTH + 4 && mouseY >= tbY - 2 && mouseY <= tbY + 12) {
            this.hexEditBox.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.visible || !this.active) {
            return false;
        }
        if (this.hexEditBox.isFocused()) {
            if (keyCode == 256) { // Escape
                this.hexEditBox.setFocused(false);
                return true;
            }
            if (Screen.isCopy(keyCode)) {
                String highlight = this.hexEditBox.getHighlighted();
                if (highlight.isEmpty()) {
                    highlight = this.hexEditBox.getValue();
                }
                Minecraft.getInstance().keyboardHandler.setClipboard(highlight);
                return true;
            }
            if (Screen.isCut(keyCode)) {
                String highlight = this.hexEditBox.getHighlighted();
                if (highlight.isEmpty()) {
                    highlight = this.hexEditBox.getValue();
                    this.hexEditBox.setValue("");
                } else {
                    this.hexEditBox.insertText("");
                }
                Minecraft.getInstance().keyboardHandler.setClipboard(highlight);
                return true;
            }
            if (Screen.isPaste(keyCode)) {
                this.hexEditBox.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                return true;
            }
            if (Screen.isSelectAll(keyCode)) {
                this.hexEditBox.moveCursorToEnd();
                this.hexEditBox.setHighlightPos(0);
                return true;
            }
            if (this.hexEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.visible || !this.active) {
            return false;
        }
        if (this.hexEditBox.isFocused()) {
            if (SharedConstants.isAllowedChatCharacter(codePoint)) {
                this.hexEditBox.insertText(Character.toString(codePoint));
                return true;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        // Tab Background
        graphics.blit(TEXTURE, getX(), getY(), 0, TAB_U, TAB_V, TAB_WIDTH, TAB_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Color Preview
        int previewX = getX() + COLOR_PREVIEW_X;
        int previewY = getY() + COLOR_PREVIEW_Y;
        for (int cx = 0; cx < COLOR_PREVIEW_SIZE; cx += 4) {
            for (int cy = 0; cy < COLOR_PREVIEW_SIZE; cy += 4) {
                int cbColor = ((cx / 4 + cy / 4) % 2 == 0) ? 0xFFFFFFFF : 0xFFCCCCCC;
                graphics.fill(previewX + cx, previewY + cy, previewX + Math.min(cx + 4, COLOR_PREVIEW_SIZE), previewY + Math.min(cy + 4, COLOR_PREVIEW_SIZE), cbColor);
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.fill(previewX, previewY, previewX + COLOR_PREVIEW_SIZE, previewY + COLOR_PREVIEW_SIZE, this.currentColor);
        RenderSystem.disableBlend();

        // Hex Text Box
        this.hexEditBox.render(graphics, mouseX, mouseY, partialTicks);

        // HSVA Text
        int textY = getY() + H_BAR_Y - this.font.lineHeight - 3;
        graphics.drawCenteredString(this.font, "H", getX() + H_BAR_X + (COLOR_BAR_WIDTH / 2), textY, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "S", getX() + S_BAR_X + (COLOR_BAR_WIDTH / 2), textY, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "V", getX() + V_BAR_X + (COLOR_BAR_WIDTH / 2), textY, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "A", getX() + A_BAR_X + (COLOR_BAR_WIDTH / 2), textY, 0xFFFFFFFF);

        // Color Bar Gradients
        int hX = getX() + H_BAR_X;
        int hY = getY() + H_BAR_Y;
        int sX = getX() + S_BAR_X;
        int sY = getY() + S_BAR_Y;
        int vX = getX() + V_BAR_X;
        int vY = getY() + V_BAR_Y;
        int aX = getX() + A_BAR_X;
        int aY = getY() + A_BAR_Y;

        for (int i = 0; i < COLOR_BAR_HEIGHT; i++) {
            float frac = 1.0f - (float) i / (float) (COLOR_BAR_HEIGHT - 1);

            // H
            int hRgb = hsvToRgb(frac, 1.0f, 1.0f);
            graphics.fill(hX, hY + i, hX + COLOR_BAR_WIDTH, hY + i + 1, 0xFF000000 | hRgb);

            // S
            int sRgb = hsvToRgb(this.hue, frac, 1.0f);
            graphics.fill(sX, sY + i, sX + COLOR_BAR_WIDTH, sY + i + 1, 0xFF000000 | sRgb);

            // V
            int vRgb = hsvToRgb(this.hue, 1.0f, frac);
            graphics.fill(vX, vY + i, vX + COLOR_BAR_WIDTH, vY + i + 1, 0xFF000000 | vRgb);
        }

        // A
        int topA = 0xFF000000 | hsvToRgb(this.hue, 1.0f, 1.0f);
        int bottomA = (topA & 0xFFFFFF);
        graphics.fillGradient(aX, aY, aX + COLOR_BAR_WIDTH, aY + COLOR_BAR_HEIGHT, topA, bottomA);

        // Slider Handles
        renderSliderHandle(graphics, hX, hY, this.hue);
        renderSliderHandle(graphics, sX, sY, this.saturation);
        renderSliderHandle(graphics, vX, vY, this.value);
        renderSliderHandle(graphics, aX, aY, this.alpha);
    }

    private void renderSliderHandle(GuiGraphics graphics, int barX, int barY, float val) {
        int indicatorX = barX + (COLOR_BAR_WIDTH - SLIDER_BAR_WIDTH) / 2;
        int indicatorY = barY + Math.round((1.0f - val) * (COLOR_BAR_HEIGHT - 1)) - (SLIDER_BAR_HEIGHT / 2);
        graphics.blit(TEXTURE, indicatorX, indicatorY, 0, SLIDER_BAR_U, SLIDER_BAR_V, SLIDER_BAR_WIDTH, SLIDER_BAR_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == 0;
    }
}

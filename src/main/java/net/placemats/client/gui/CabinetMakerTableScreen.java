package net.placemats.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.placemats.PlaceMatMain;
import net.placemats.client.gui.util.BlockTextureExtractor;
import net.placemats.client.gui.util.ColorPickerWidget;
import net.placemats.client.renderer.DynamicTexturedBakedModel;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.placemats.common.inventory.CabinetMakerTableContainer;
import net.placemats.network.PlaceMatsNetworkHandler;
import net.placemats.network.packet.ApplyCabinetMakerCraftPacket;
import net.placemats.network.packet.SetCabinetMakerGhostSlotPacket;
import net.placemats.common.data.PlaceMatTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CabinetMakerTableScreen extends AbstractContainerScreen<CabinetMakerTableContainer> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PlaceMatMain.MOD_ID, "textures/gui/cabinetmaker_table.png");

    private static final int WIDTH = 188;
    private static final int HEIGHT = 243;
    public static final int SLOT_SIZE = 18;

    public static final int INPUT_SLOT_X = 37;
    public static final int INPUT_SLOT_Y = 17;

    private static final int PREVIEW_BOX_X = 8;
    private static final int PREVIEW_BOX_Y = INPUT_SLOT_Y + SLOT_SIZE + 9;
    private static final int PREVIEW_BOX_SIZE = 64;

    private static final int BUTTON_X = PREVIEW_BOX_X;
    private static final int BUTTON_Y = PREVIEW_BOX_Y + PREVIEW_BOX_SIZE + 1;
    private static final int BUTTON_WIDTH = PREVIEW_BOX_SIZE;
    private static final int BUTTON_HEIGHT = SLOT_SIZE - 2;

    private static final int OUTPUT_SLOT_OVERLAY_U = WIDTH;
    private static final int OUTPUT_SLOT_OVERLAY_V = 0;
    public static final int OUTPUT_SLOT_X = INPUT_SLOT_X;
    public static final int OUTPUT_SLOT_Y = PREVIEW_BOX_Y + PREVIEW_BOX_SIZE + SLOT_SIZE;

    public static final int INVENTORY_X = SLOT_SIZE + 2;
    public static final int INVENTORY_Y = OUTPUT_SLOT_Y + SLOT_SIZE + 17;

    public static final int MATERIAL_SLOT_X = INPUT_SLOT_X + SLOT_SIZE + 25;
    public static final int MATERIAL_SLOT_Y = INPUT_SLOT_Y + 10;
    private static final int ACTIVE_MATERIAL_SLOT_U = OUTPUT_SLOT_OVERLAY_U;
    private static final int ACTIVE_MATERIAL_SLOT_V = OUTPUT_SLOT_OVERLAY_V + SLOT_SIZE;

    private static final int SELECTION_OVERLAY_U = ACTIVE_MATERIAL_SLOT_U;
    private static final int SELECTION_OVERLAY_V = ACTIVE_MATERIAL_SLOT_V + SLOT_SIZE;
    private static final int SELECTION_OVERLAY_SIZE = SLOT_SIZE + 2;

    private static final int BLOCKSTATE_BUTTON_SIZE = 9;
    private static final int BLOCKSTATE_BUTTON_X = PREVIEW_BOX_X + PREVIEW_BOX_SIZE - BLOCKSTATE_BUTTON_SIZE - 1;
    private static final int BLOCKSTATE_BUTTON_Y = PREVIEW_BOX_Y + 1;
    private static final int BLOCKSTATE_BUTTON_U = SELECTION_OVERLAY_U;
    private static final int BLOCKSTATE_BUTTON_V = SELECTION_OVERLAY_V + SELECTION_OVERLAY_SIZE;

    private static final int TEXTURE_SWATCH_SIZE = 16;
    public static final int MAX_TEXTURE_SWATCHES = 5;

    private final Map<String, ResourceLocation> chosenTextures = new HashMap<>();
    private final Map<String, Integer> chosenColors = new HashMap<>();
    private final Map<Integer, List<ResourceLocation>> availableTexturesPerKey = new HashMap<>();
    private final List<BlockState> cycleableBlockStates = new ArrayList<>();
    private int currentBlockStateIndex = 0;
    private ItemStack lastInputStack = ItemStack.EMPTY;
    private final ItemStack[] lastMaterialStacks = new ItemStack[CabinetMakerTableContainer.MATERIAL_SLOT_COUNT];

    private Button confirmButton;
    @Getter private ColorPickerWidget colorPickerWidget;
    private int activeColorKeyIndex = -1;

    public CabinetMakerTableScreen(CabinetMakerTableContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelY = this.topPos + INVENTORY_Y - 10;
        this.inventoryLabelX = this.leftPos + INVENTORY_X - 1;
        Arrays.fill(lastMaterialStacks, ItemStack.EMPTY);
    }

    @Override
    protected void init() {
        super.init();

        this.confirmButton = Button.builder(Component.literal("▼"), btn -> {
            boolean transformAll = hasShiftDown();
            PlaceMatsNetworkHandler.INSTANCE.sendToServer(new ApplyCabinetMakerCraftPacket(new HashMap<>(chosenTextures), new HashMap<>(chosenColors), transformAll));
        })
            .bounds(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.place_mats.cabinet_maker.confirm")))
            .build();

        this.addRenderableWidget(this.confirmButton);

        this.colorPickerWidget = new ColorPickerWidget(this.font, this.leftPos + this.imageWidth, this.topPos + MATERIAL_SLOT_Y, this::onColorPickerChanged);
        this.colorPickerWidget.visible = false;
        this.addRenderableWidget(this.colorPickerWidget);

        updateState();
    }

    private void onColorPickerChanged(int color) {
        int keyCount = this.menu.getDynamicKeyCount();
        IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
        if (dynamicBlock != null && activeColorKeyIndex >= 0 && activeColorKeyIndex < keyCount && activeColorKeyIndex < dynamicBlock.getTextureKeys().size()) {
            String key = dynamicBlock.getTextureKeys().get(activeColorKeyIndex);
            chosenColors.put(key, color);
            updateConfirmButton();
        }
    }

    /**
     * Turns a hex color string into ARGB.
     * @param text the hex color.
     * @return the ARGB color.
     */
    @SuppressWarnings("DuplicateExpressions")
    public static Integer parseHexColor(String text) {
        if (text == null) {
            return null;
        }
        String clean = text.trim();
        if (clean.isEmpty()) {
            return null;
        }
        for (IDynamicTexturedBlock.KeywordColor keywordColor : IDynamicTexturedBlock.KeywordColor.values()) {
            boolean match = keywordColor.getKeywords().stream()
                .anyMatch(clean::equalsIgnoreCase);
            if (match) {
                return keywordColor.getColorCode();
            }
        }
        try {
            if (clean.length() == 3) {
                int r = Integer.parseInt(clean.substring(0, 1), 16) * 17;
                int g = Integer.parseInt(clean.substring(1, 2), 16) * 17;
                int b = Integer.parseInt(clean.substring(2, 3), 16) * 17;
                return 0xFF000000 | (r << 16) | (g << 8) | b;
            } else if (clean.length() == 4) {
                int a = Integer.parseInt(clean.substring(0, 1), 16) * 17;
                int r = Integer.parseInt(clean.substring(1, 2), 16) * 17;
                int g = Integer.parseInt(clean.substring(2, 3), 16) * 17;
                int b = Integer.parseInt(clean.substring(3, 4), 16) * 17;
                return (a << 24) | (r << 16) | (g << 8) | b;
            } else if (clean.length() == 6) {
                int rgb = (int) Long.parseLong(clean, 16);
                return 0xFF000000 | (rgb & 0xFFFFFF);
            } else if (clean.length() == 8) {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public static int getRainbowColor() {
        float hue = (float) ((Util.getMillis() % 4000L) / 4000.0);
        int rgb = Mth.hsvToRgb(hue, 1.0f, 1.0f);
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static boolean isFacingProperty(Property<?> property) {
        if (property instanceof DirectionProperty) {
            return true;
        }
        String name = property.getName().toLowerCase(Locale.ROOT);
        return name.contains("facing") || name.contains("direction") || name.equals("orientation") || name.equals("rotation");
    }

    public static List<BlockState> getCycleableBlockStates(Block block) {
        if (block == null) {
            return List.of();
        }
        List<BlockState> possible = block.getStateDefinition().getPossibleStates();
        if (possible.size() <= 1) {
            return possible;
        }
        BlockState defaultState = block.defaultBlockState();
        List<BlockState> filtered = new ArrayList<>();
        for (BlockState state : possible) {
            boolean match = true;
            for (Property<?> prop : block.getStateDefinition().getProperties()) {
                if (isFacingProperty(prop)) {
                    if (!Objects.equals(state.getValue(prop), defaultState.getValue(prop))) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                filtered.add(state);
            }
        }
        return filtered.isEmpty() ? possible : filtered;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.colorPickerWidget != null) {
            this.colorPickerWidget.tick();
        }
        checkInputsChanged();
    }

    private void checkInputsChanged() {
        ItemStack currentInput = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
        boolean inputChanged = !ItemStack.isSameItemSameTags(currentInput, lastInputStack);

        boolean matsChanged = false;
        for (int i = 0; i < CabinetMakerTableContainer.MATERIAL_SLOT_COUNT; i++) {
            ItemStack curMat = this.menu.getContainer().getItem(CabinetMakerTableContainer.MATERIAL_SLOT_START + i);
            if (!ItemStack.isSameItemSameTags(curMat, lastMaterialStacks[i])) {
                matsChanged = true;
                lastMaterialStacks[i] = curMat.copy();
            }
        }

        if (inputChanged || matsChanged) {
            if (inputChanged) {
                lastInputStack = currentInput.copy();
                chosenTextures.clear();
                chosenColors.clear();
                cycleableBlockStates.clear();
                currentBlockStateIndex = 0;
                if (!currentInput.isEmpty() && currentInput.getItem() instanceof BlockItem bi) {
                    cycleableBlockStates.addAll(getCycleableBlockStates(bi.getBlock()));
                }
                Map<String, ResourceLocation> existing = IDynamicTexturedBlock.getDynamicTextures(currentInput);
                chosenTextures.putAll(existing);
                Map<String, Integer> existingColors = IDynamicTexturedBlock.getDynamicColors(currentInput);
                chosenColors.putAll(existingColors);
            }
            updateState();
        }
    }

    private void updateState() {
        availableTexturesPerKey.clear();
        int keyCount = this.menu.getDynamicKeyCount();
        IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();

        ItemStack input = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
        if (input.isEmpty() || !(input.getItem() instanceof BlockItem)) {
            cycleableBlockStates.clear();
            currentBlockStateIndex = 0;
        } else if (cycleableBlockStates.isEmpty()) {
            cycleableBlockStates.addAll(getCycleableBlockStates(((BlockItem) input.getItem()).getBlock()));
            currentBlockStateIndex = 0;
        }

        if (keyCount > 0 && dynamicBlock != null) {
            Map<String, ResourceLocation> existing = IDynamicTexturedBlock.getDynamicTextures(input);
            List<String> keys = dynamicBlock.getTextureKeys();
            for (int i = 0; i < keyCount && i < keys.size(); i++) {
                String key = keys.get(i);
                ItemStack matStack = this.menu.getContainer().getItem(CabinetMakerTableContainer.MATERIAL_SLOT_START + i);
                List<ResourceLocation> extracted = BlockTextureExtractor.extract16x16Textures(matStack);
                availableTexturesPerKey.put(i, extracted);

                if (!extracted.isEmpty()) {
                    ResourceLocation current = chosenTextures.get(key);
                    if (current == null || !extracted.contains(current)) {
                        chosenTextures.put(key, extracted.get(0));
                    }
                } else {
                    ResourceLocation current = existing.get(key);
                    if (current == null) {
                        current = dynamicBlock.getDefaultTexture(key);
                    }
                    chosenTextures.put(key, current);
                }
            }

            if (colorPickerWidget != null) {
                if (activeColorKeyIndex >= keyCount) {
                    colorPickerWidget.visible = false;
                    colorPickerWidget.getHexEditBox().setFocused(false);
                    activeColorKeyIndex = -1;
                } else if (colorPickerWidget.visible && activeColorKeyIndex >= 0 && activeColorKeyIndex < keys.size()) {
                    String activeKey = keys.get(activeColorKeyIndex);
                    Integer col = chosenColors.get(activeKey);
                    colorPickerWidget.setColor(col != null ? col : 0xFFFFFFFF);
                }
            }
        } else {
            chosenTextures.clear();
            chosenColors.clear();
            if (colorPickerWidget != null) {
                colorPickerWidget.visible = false;
                colorPickerWidget.getHexEditBox().setFocused(false);
                activeColorKeyIndex = -1;
            }
        }

        if (confirmButton != null) {
            updateConfirmButton();
        }
    }

    private void updateConfirmButton() {
        if (confirmButton != null) {
            int keyCount = this.menu.getDynamicKeyCount();
            IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
            ItemStack inputStack = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
            confirmButton.active = keyCount > 0 && !inputStack.isEmpty() && hasCraftChanges(inputStack, dynamicBlock, chosenTextures, chosenColors);
        }
    }

    public static boolean hasCraftChanges(ItemStack input, IDynamicTexturedBlock dynamicBlock, Map<String, ResourceLocation> chosenTextures, Map<String, Integer> chosenColors) {
        if (input.isEmpty() || dynamicBlock == null) {
            return false;
        }
        Map<String, ResourceLocation> existingTextures = IDynamicTexturedBlock.getDynamicTextures(input);
        Map<String, Integer> existingColors = IDynamicTexturedBlock.getDynamicColors(input);

        for (String key : dynamicBlock.getTextureKeys()) {
            ResourceLocation chosen = chosenTextures != null ? chosenTextures.get(key) : null;
            ResourceLocation current = existingTextures.get(key);
            if (current == null) {
                current = dynamicBlock.getDefaultTexture(key);
            }
            if (chosen != null && !chosen.equals(current)) {
                return true;
            }

            Integer chosenCol = chosenColors != null ? chosenColors.get(key) : null;
            Integer currentCol = existingColors.get(key);
            if (!Objects.equals(chosenCol, currentCol)) {
                return true;
            }
        }
        return false;
    }

    public int getMaterialSlotIndexAt(double mouseX, double mouseY) {
        int keyCount = this.menu.getDynamicKeyCount();
        if (keyCount <= 0) {
            return -1;
        }
        int matStartX = this.leftPos + MATERIAL_SLOT_X;
        int matSpacing = SLOT_SIZE + 3;
        int slotY = this.topPos + MATERIAL_SLOT_Y;
        int columnHeight = SLOT_SIZE + 3 + MAX_TEXTURE_SWATCHES * (TEXTURE_SWATCH_SIZE + 3);

        for (int i = 0; i < keyCount; i++) {
            int colX = matStartX + (i * matSpacing);
            if (mouseX >= colX && mouseX < colX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + columnHeight) {
                return i;
            }
        }
        return -1;
    }

    public boolean setGhostMaterial(int keyIndex, ItemStack stack) {
        if (keyIndex < 0 || keyIndex >= this.menu.getDynamicKeyCount()) {
            return false;
        }
        if (stack != null && !stack.isEmpty() && stack.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST)) {
            return false;
        }
        ItemStack ghost = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.menu.setGhostSlot(keyIndex, ghost);
        PlaceMatsNetworkHandler.INSTANCE.sendToServer(new SetCabinetMakerGhostSlotPacket(keyIndex, ghost));
        checkInputsChanged();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.2f);
        }
        return true;
    }

    public void renderGhostDropTargets(GuiGraphics graphics, int mouseX, int mouseY) {
        int keyCount = this.menu.getDynamicKeyCount();
        if (keyCount <= 0) {
            return;
        }
        int hoveredIndex = getMaterialSlotIndexAt(mouseX, mouseY);
        int matStartX = this.leftPos + MATERIAL_SLOT_X;
        int matSpacing = SLOT_SIZE + 3;
        int slotY = this.topPos + MATERIAL_SLOT_Y;

        for (int i = 0; i < keyCount; i++) {
            int colX = matStartX + (i * matSpacing);
            int color = (i == hoveredIndex) ? 0x80ff9d26 : 0x40FFFFFF;
            graphics.fill(colX + 1, slotY + 1, colX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, color);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.colorPickerWidget != null && this.colorPickerWidget.visible) {
            if (this.colorPickerWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.colorPickerWidget != null && this.colorPickerWidget.visible) {
            if (this.colorPickerWidget.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.colorPickerWidget != null && this.colorPickerWidget.visible) {
            if (this.colorPickerWidget.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.colorPickerWidget != null && this.colorPickerWidget.visible) {
            if (this.colorPickerWidget.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.colorPickerWidget != null && this.colorPickerWidget.visible) {
            int wx = this.colorPickerWidget.getX();
            int wy = this.colorPickerWidget.getY();
            if (mouseX < wx || mouseX >= wx + this.colorPickerWidget.getWidth() || mouseY < wy || mouseY >= wy + this.colorPickerWidget.getHeight()) {
                this.colorPickerWidget.getHexEditBox().setFocused(false);
            }
        }

        // Tab Buttons and Texture Selection
        if (button == 0) {
            // Blockstate Cycle Button
            if (cycleableBlockStates.size() > 1) {
                int btnX = this.leftPos + BLOCKSTATE_BUTTON_X;
                int btnY = this.topPos + BLOCKSTATE_BUTTON_Y;
                if (mouseX >= btnX && mouseX < btnX + BLOCKSTATE_BUTTON_SIZE &&
                        mouseY >= btnY && mouseY < btnY + BLOCKSTATE_BUTTON_SIZE) {
                    // If shift is held, cycle back to the default block state.
                    if (hasShiftDown()) {
                        ItemStack input = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
                        if (!input.isEmpty() && input.getItem() instanceof BlockItem bi) {
                            BlockState def = bi.getBlock().defaultBlockState();
                            int defIdx = cycleableBlockStates.indexOf(def);
                            currentBlockStateIndex = Math.max(defIdx, 0);
                        } else {
                            currentBlockStateIndex = 0;
                        }
                    } else {
                        currentBlockStateIndex = (currentBlockStateIndex + 1) % cycleableBlockStates.size();
                    }
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.2f);
                    }
                    return true;
                }
            }

            int keyCount = this.menu.getDynamicKeyCount();
            IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
            if (keyCount > 0 && dynamicBlock != null) {
                List<String> keys = dynamicBlock.getTextureKeys();
                int matSpacing = SLOT_SIZE + 3;

                // Open Tab Buttons
                int btnY = this.topPos + 146;
                for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                    int colX = this.leftPos + MATERIAL_SLOT_X + (keyIdx * matSpacing);
                    int btnX = colX + 1;
                    if (mouseX >= btnX && mouseX < btnX + ColorPickerWidget.OPEN_TAB_WIDTH &&
                            mouseY >= btnY && mouseY < btnY + ColorPickerWidget.OPEN_TAB_HEIGHT) {
                        if (this.colorPickerWidget != null && this.colorPickerWidget.visible && this.activeColorKeyIndex == keyIdx) {
                            this.colorPickerWidget.visible = false;
                            this.colorPickerWidget.getHexEditBox().setFocused(false);
                            this.activeColorKeyIndex = -1;
                        } else if (this.colorPickerWidget != null) {
                            this.activeColorKeyIndex = keyIdx;
                            String key = keys.get(keyIdx);
                            Integer currentTint = this.chosenColors.get(key);
                            this.colorPickerWidget.setColor(currentTint != null ? currentTint : 0xFFFFFFFF);
                            this.colorPickerWidget.visible = true;
                            this.colorPickerWidget.getHexEditBox().setFocused(false);
                        }
                        if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.2f);
                        }
                        return true;
                    }
                }

                // Texture Selection Buttons
                for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                    String key = keys.get(keyIdx);
                    List<ResourceLocation> textures = availableTexturesPerKey.getOrDefault(keyIdx, List.of());
                    int colX = this.leftPos + MATERIAL_SLOT_X + (keyIdx * matSpacing);
                    int startY = this.topPos + MATERIAL_SLOT_Y + SLOT_SIZE + 3;

                    for (int tIdx = 0; tIdx < textures.size() && tIdx < MAX_TEXTURE_SWATCHES; tIdx++) {
                        int swatchY = startY + (tIdx * (TEXTURE_SWATCH_SIZE + 3));
                        if (mouseX >= colX && mouseX < colX + TEXTURE_SWATCH_SIZE &&
                                mouseY >= swatchY && mouseY < swatchY + TEXTURE_SWATCH_SIZE) {
                            ResourceLocation clickedTex = textures.get(tIdx);
                            chosenTextures.put(key, clickedTex);
                            updateConfirmButton();
                            if (Minecraft.getInstance().player != null) {
                                Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.2f);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 0, WIDTH, HEIGHT, 256, 256);

        // Output Slot Animation
        long systemSeconds = Util.getMillis() / 800L;
        int currentFrame = (int) (systemSeconds % 3);

        int outputSlotX = this.leftPos + OUTPUT_SLOT_X - 6;
        int outputSlotY = this.topPos + OUTPUT_SLOT_Y;
        graphics.blit(TEXTURE, outputSlotX, outputSlotY, 0, OUTPUT_SLOT_OVERLAY_U + (currentFrame * SLOT_SIZE), OUTPUT_SLOT_OVERLAY_V, SLOT_SIZE, SLOT_SIZE, 256, 256);

        // Material Slots
        int keyCount = this.menu.getDynamicKeyCount();
        int matStartX = this.leftPos + MATERIAL_SLOT_X;
        int matSpacing = SLOT_SIZE + 3;

        for (int i = 0; i < CabinetMakerTableContainer.MATERIAL_SLOT_COUNT; i++) {
            int slotX = matStartX + (i * matSpacing);
            int slotY = this.topPos + MATERIAL_SLOT_Y;
            if (i < keyCount) {
                graphics.blit(TEXTURE, slotX, slotY, 0, ACTIVE_MATERIAL_SLOT_U, ACTIVE_MATERIAL_SLOT_V, SLOT_SIZE, SLOT_SIZE, 256, 256);
            }
        }

        // Texture Slots
        IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
        if (keyCount > 0 && dynamicBlock != null) {
            List<String> keys = dynamicBlock.getTextureKeys();
            for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                String key = keys.get(keyIdx);
                List<ResourceLocation> textures = availableTexturesPerKey.getOrDefault(keyIdx, List.of());
                int colX = matStartX + (keyIdx * matSpacing);
                int startY = this.topPos + MATERIAL_SLOT_Y + SLOT_SIZE + 3;
                ResourceLocation currentChoice = chosenTextures.get(key);

                // Number Label
                graphics.drawCenteredString(this.font, String.valueOf(keyIdx + 1), colX + 9, this.topPos + MATERIAL_SLOT_Y - 10, 0xFFAA00);

                if (textures.isEmpty()) {
                    // No Texture Label
                    graphics.drawCenteredString(this.font, "⏷", colX + 9, startY + 4, 0xFF888888);
                } else {
                    for (int tIdx = 0; tIdx < textures.size() && tIdx < MAX_TEXTURE_SWATCHES; tIdx++) {
                        ResourceLocation tex = textures.get(tIdx);
                        int swatchY = startY + (tIdx * (TEXTURE_SWATCH_SIZE + 3));
                        boolean isSelected = tex.equals(currentChoice);

                        graphics.blit(TEXTURE, colX, swatchY, 0, ACTIVE_MATERIAL_SLOT_U, ACTIVE_MATERIAL_SLOT_V, SLOT_SIZE, SLOT_SIZE, 256, 256);

                        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
                        if (sprite != null) {
                            RenderSystem.enableBlend();
                            RenderSystem.defaultBlendFunc();
                            Integer tint = chosenColors.get(key);
                            if (tint != null) {
                                float a = ((tint >> 24) & 0xFF) / 255.0f;
                                float r = ((tint >> 16) & 0xFF) / 255.0f;
                                float g = ((tint >> 8) & 0xFF) / 255.0f;
                                float b = (tint & 0xFF) / 255.0f;
                                graphics.blit(colX + 1, swatchY + 1, 0, TEXTURE_SWATCH_SIZE, TEXTURE_SWATCH_SIZE, sprite, r, g, b, a);
                            } else {
                                graphics.blit(colX + 1, swatchY + 1, 0, TEXTURE_SWATCH_SIZE, TEXTURE_SWATCH_SIZE, sprite, 1.0f, 1.0f, 1.0f, 1.0f);
                            }
                            RenderSystem.disableBlend();
                        }

                        if (mouseX >= colX + 1 && mouseX < colX + SLOT_SIZE && mouseY >= swatchY && mouseY + 1 < swatchY + SLOT_SIZE) {
                            graphics.fill(colX + 1, swatchY + 1, colX + SLOT_SIZE - 1, swatchY + SLOT_SIZE - 1, 0x70FFFFFF);
                        }

                        // Highlight Texture
                        if (isSelected) {
                            graphics.blit(TEXTURE, colX - 1, swatchY - 1, 0, SELECTION_OVERLAY_U, SELECTION_OVERLAY_V, SELECTION_OVERLAY_SIZE, SELECTION_OVERLAY_SIZE, 256, 256);
                        }
                    }
                }
            }

            // Color Tab Buttons
            int btnY = this.topPos + 146;
            for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                int colX = matStartX + (keyIdx * matSpacing);
                int btnX = colX + 1;

                // Unpressed
                graphics.blit(ColorPickerWidget.TEXTURE, btnX, btnY, 0, ColorPickerWidget.OPEN_TAB_BUTTON_U, ColorPickerWidget.OPEN_TAB_BUTTON_V, ColorPickerWidget.OPEN_TAB_WIDTH, ColorPickerWidget.OPEN_TAB_HEIGHT, ColorPickerWidget.TEXTURE_SIZE, ColorPickerWidget.TEXTURE_SIZE);
                // Pressed
                if (this.colorPickerWidget != null && this.colorPickerWidget.visible && this.activeColorKeyIndex == keyIdx) {
                    graphics.blit(ColorPickerWidget.TEXTURE, btnX, btnY, 0, ColorPickerWidget.CLOSE_TAB_BUTTON_U, ColorPickerWidget.CLOSE_TAB_BUTTON_V, ColorPickerWidget.OPEN_TAB_WIDTH, ColorPickerWidget.OPEN_TAB_HEIGHT, ColorPickerWidget.TEXTURE_SIZE, ColorPickerWidget.TEXTURE_SIZE);
                }
                // Hover
                if (mouseX >= btnX && mouseX < btnX + ColorPickerWidget.OPEN_TAB_WIDTH && mouseY >= btnY && mouseY < btnY + ColorPickerWidget.OPEN_TAB_HEIGHT) {
                    graphics.fill(btnX, btnY, btnX + ColorPickerWidget.OPEN_TAB_WIDTH, btnY + ColorPickerWidget.OPEN_TAB_HEIGHT, 0x40FFFFFF);
                }
            }
        }

        // Blockstate Cycle Button
        if (cycleableBlockStates.size() > 1) {
            int btnX = this.leftPos + BLOCKSTATE_BUTTON_X;
            int btnY = this.topPos + BLOCKSTATE_BUTTON_Y;
            graphics.blit(TEXTURE, btnX, btnY, 0, BLOCKSTATE_BUTTON_U, BLOCKSTATE_BUTTON_V, BLOCKSTATE_BUTTON_SIZE, BLOCKSTATE_BUTTON_SIZE, 256, 256);
            if (mouseX >= btnX && mouseX < btnX + BLOCKSTATE_BUTTON_SIZE && mouseY >= btnY && mouseY < btnY + BLOCKSTATE_BUTTON_SIZE) {
                graphics.fill(btnX, btnY, btnX + BLOCKSTATE_BUTTON_SIZE, btnY + BLOCKSTATE_BUTTON_SIZE, 0x40FFFFFF);
            }
        }

        // 3D Preview
        ItemStack inputStack = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            float rotY = (Minecraft.getInstance().level != null ? (Minecraft.getInstance().level.getGameTime() + partialTick) : 0.0f) % 360.0f;
            BlockState stateToRender = (!cycleableBlockStates.isEmpty() && currentBlockStateIndex < cycleableBlockStates.size())
                ? cycleableBlockStates.get(currentBlockStateIndex)
                : null;
            render3DPreview(graphics, this.leftPos + PREVIEW_BOX_X + (PREVIEW_BOX_SIZE / 2), this.topPos + PREVIEW_BOX_Y + (PREVIEW_BOX_SIZE / 2), PREVIEW_BOX_SIZE - 10, rotY, inputStack, stateToRender, chosenTextures, chosenColors);
        }
    }


    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 7, 5, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Blockstate Cycle Button Tooltip
        if (cycleableBlockStates.size() > 1) {
            int btnX = this.leftPos + BLOCKSTATE_BUTTON_X;
            int btnY = this.topPos + BLOCKSTATE_BUTTON_Y;
            if (mouseX >= btnX && mouseX < btnX + BLOCKSTATE_BUTTON_SIZE && mouseY >= btnY && mouseY < btnY + BLOCKSTATE_BUTTON_SIZE) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.place_mats.cabinet_maker.cycle_blockstate").withStyle(ChatFormatting.YELLOW));
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }

        int keyCount = this.menu.getDynamicKeyCount();
        IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
        if (keyCount > 0 && dynamicBlock != null) {
            List<String> keys = dynamicBlock.getTextureKeys();

            for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                List<ResourceLocation> textures = availableTexturesPerKey.getOrDefault(keyIdx, List.of());
                int colX = this.leftPos + MATERIAL_SLOT_X + (keyIdx * SLOT_SIZE + 3);
                int startY = this.topPos + MATERIAL_SLOT_Y + SLOT_SIZE + 3;

                // Texture Swatch Tooltips
                for (int tIdx = 0; tIdx < textures.size() && tIdx < MAX_TEXTURE_SWATCHES; tIdx++) {
                    int swatchY = startY + (tIdx * (TEXTURE_SWATCH_SIZE + 3));
                    if (mouseX >= colX && mouseX < colX + TEXTURE_SWATCH_SIZE &&
                            mouseY >= swatchY && mouseY < swatchY + TEXTURE_SWATCH_SIZE) {
                        ResourceLocation tex = textures.get(tIdx);
                        List<Component> tooltip = new ArrayList<>();
                        tooltip.add(Component.literal(tex.getPath()).withStyle(ChatFormatting.GOLD));
                        tooltip.add(Component.literal(tex.getNamespace()).withStyle(ChatFormatting.DARK_GRAY));
                        tooltip.add(Component.translatable("gui.place_mats.cabinet_maker.click_to_apply").withStyle(ChatFormatting.GRAY));
                        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                        return;
                    }
                }

                // Material Slot Tooltips
                if (mouseX >= colX && mouseX < colX + 18 && mouseY >= this.topPos + MATERIAL_SLOT_Y - 10 && mouseY < this.topPos + MATERIAL_SLOT_Y) {
                    Component keyName = dynamicBlock.getTextureKeyName(keyIdx);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("gui.place_mats.cabinet_maker.key_slot_title", keyIdx + 1, keyName).withStyle(ChatFormatting.YELLOW));
                    tooltip.add(Component.translatable("gui.place_mats.cabinet_maker.material_tooltip").withStyle(ChatFormatting.GRAY));
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }

                // Open Tab Button Tooltips
                int btnY = this.topPos + 146;
                int btnX = colX + 1;
                if (mouseX >= btnX && mouseX < btnX + ColorPickerWidget.OPEN_TAB_WIDTH &&
                        mouseY >= btnY && mouseY < btnY + ColorPickerWidget.OPEN_TAB_HEIGHT) {
                    Component keyName = dynamicBlock.getTextureKeyName(keyIdx);
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("gui.place_mats.cabinet_maker.color_picker_tab", keyName).withStyle(ChatFormatting.YELLOW));
                    guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }
        }
    }

    /**
     * Renders a 3D preview of the dynamic block item in the GUI using the supplied
     * textures and color selections.
     *
     * @param guiGraphics the GUI graphics context.
     * @param x the x-cords.
     * @param y the y-cords.
     * @param size the base size used to scale the rendered preview.
     * @param rotationY the rotation angle, in degrees, applied around the Y axis.
     * @param inputStack the item stack to render.
     * @param textures the texture selections to apply.
     * @param colors the color selections to apply.
     */
    public static void render3DPreview(GuiGraphics guiGraphics, int x, int y, int size, float rotationY, ItemStack inputStack, Map<String, ResourceLocation> textures, Map<String, Integer> colors) {
        render3DPreview(guiGraphics, x, y, size, rotationY, inputStack, null, textures, colors);
    }

    /**
     * Renders a 3D preview of the dynamic block item in the GUI using the supplied
     * blockstate, textures, and color selections.
     *
     * @param guiGraphics the GUI graphics context.
     * @param x the x-cords.
     * @param y the y-cords.
     * @param size the base size used to scale the rendered preview.
     * @param rotationY the rotation angle, in degrees, applied around the Y axis.
     * @param inputStack the item stack to render.
     * @param blockState the specific blockstate to render.
     * @param textures the texture selections to apply.
     * @param colors the color selections to apply.
     */
    public static void render3DPreview(GuiGraphics guiGraphics, int x, int y, int size, float rotationY, ItemStack inputStack, @Nullable BlockState blockState, Map<String, ResourceLocation> textures, Map<String, Integer> colors) {
        if (inputStack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BakedModel baseModel = null;
        if (blockState != null) {
            baseModel = mc.getBlockRenderer().getBlockModel(blockState);
        }
        if (baseModel == null) {
            baseModel = mc.getItemRenderer().getModel(inputStack, mc.level, mc.player, 0);
        }

        BakedModel modelToRender = baseModel;
        if (baseModel instanceof DynamicTexturedBakedModel dtbm) {
            modelToRender = new DynamicTexturedBakedModel.DynamicTexturedItemBakedModel(dtbm, blockState, textures, colors);
        } else if (baseModel instanceof DynamicTexturedBakedModel.DynamicTexturedItemBakedModel dtibm) {
            modelToRender = new DynamicTexturedBakedModel.DynamicTexturedItemBakedModel(dtibm.getParent(), blockState, textures, colors);
        } else if (inputStack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof IDynamicTexturedBlock dtb) {
            DynamicTexturedBakedModel tempDtbm = new DynamicTexturedBakedModel(baseModel, dtb);
            modelToRender = new DynamicTexturedBakedModel.DynamicTexturedItemBakedModel(tempDtbm, blockState, textures, colors);
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(x, y, 100.0f);
        poseStack.scale(size * 0.7f, -size * 0.7f, size * 0.7f);

        poseStack.mulPose(Axis.XP.rotationDegrees(25.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Lighting.setupForFlatItems();
        mc.getItemRenderer().render(
            inputStack,
            ItemDisplayContext.NONE,
            false,
            poseStack,
            bufferSource,
            LightTexture.FULL_SKY,
            OverlayTexture.NO_OVERLAY,
            modelToRender
        );

        bufferSource.endBatch(RenderType.translucent());
        bufferSource.endBatch();

        Lighting.setupFor3DItems();
        poseStack.popPose();
    }
}

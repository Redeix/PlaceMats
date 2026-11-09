package net.placemats.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.placemats.PlaceMatMain;
import net.placemats.client.gui.util.BlockTextureExtractor;
import net.placemats.client.renderer.DynamicShelfBakedModel;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.placemats.common.inventory.CabinetMakerTableContainer;
import net.placemats.network.PlaceMatsNetworkHandler;
import net.placemats.network.packet.ApplyCabinetMakerCraftPacket;
import net.placemats.network.packet.SetCabinetMakerGhostSlotPacket;
import net.placemats.common.data.PlaceMatTags;
import org.jetbrains.annotations.NotNull;

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

    private static final int TEXTURE_SWATCH_SIZE = 16;

    private final Map<String, ResourceLocation> chosenTextures = new HashMap<>();
    private final Map<Integer, List<ResourceLocation>> availableTexturesPerKey = new HashMap<>();
    private ItemStack lastInputStack = ItemStack.EMPTY;
    private final ItemStack[] lastMaterialStacks = new ItemStack[CabinetMakerTableContainer.MATERIAL_SLOT_COUNT];

    private Button confirmButton;

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
            PlaceMatsNetworkHandler.INSTANCE.sendToServer(new ApplyCabinetMakerCraftPacket(new HashMap<>(chosenTextures), transformAll));
        })
            .bounds(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
            .tooltip(Tooltip.create(Component.translatable("gui.place_mats.cabinet_maker.confirm")))
            .build();

        this.addRenderableWidget(this.confirmButton);
        updateState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
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
                Map<String, ResourceLocation> existing = IDynamicTexturedBlock.getDynamicTextures(currentInput);
                chosenTextures.putAll(existing);
            }
            updateState();
        }
    }

    private void updateState() {
        availableTexturesPerKey.clear();
        int keyCount = this.menu.getDynamicKeyCount();
        IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();

        if (keyCount > 0 && dynamicBlock != null) {
            ItemStack input = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
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
        } else {
            chosenTextures.clear();
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
            confirmButton.active = keyCount > 0 && !inputStack.isEmpty() && hasTextureChanges(inputStack, dynamicBlock, chosenTextures);
        }
    }

    public static boolean hasTextureChanges(ItemStack input, IDynamicTexturedBlock dynamicBlock, Map<String, ResourceLocation> chosenTextures) {
        if (input.isEmpty() || dynamicBlock == null || chosenTextures == null || chosenTextures.isEmpty()) {
            return false;
        }
        Map<String, ResourceLocation> existing = IDynamicTexturedBlock.getDynamicTextures(input);
        for (String key : dynamicBlock.getTextureKeys()) {
            ResourceLocation chosen = chosenTextures.get(key);
            ResourceLocation current = existing.get(key);
            if (current == null) {
                current = dynamicBlock.getDefaultTexture(key);
            }
            if (chosen != null && !chosen.equals(current)) {
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
        int columnHeight = SLOT_SIZE + 3 + 6 * (TEXTURE_SWATCH_SIZE + 3);

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Texture Selection Buttons
        if (button == 0) {
            int keyCount = this.menu.getDynamicKeyCount();
            IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
            if (keyCount > 0 && dynamicBlock != null) {
                List<String> keys = dynamicBlock.getTextureKeys();
                int matSpacing = SLOT_SIZE + 3;

                for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                    String key = keys.get(keyIdx);
                    List<ResourceLocation> textures = availableTexturesPerKey.getOrDefault(keyIdx, List.of());
                    int colX = this.leftPos + MATERIAL_SLOT_X + (keyIdx * matSpacing);
                    int startY = this.topPos + MATERIAL_SLOT_Y + SLOT_SIZE + 3;

                    for (int tIdx = 0; tIdx < textures.size() && tIdx < 6; tIdx++) {
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
                    for (int tIdx = 0; tIdx < textures.size() && tIdx < 6; tIdx++) {
                        ResourceLocation tex = textures.get(tIdx);
                        int swatchY = startY + (tIdx * (TEXTURE_SWATCH_SIZE + 3));
                        boolean isSelected = tex.equals(currentChoice);

                        graphics.blit(TEXTURE, colX, swatchY, 0, ACTIVE_MATERIAL_SLOT_U, ACTIVE_MATERIAL_SLOT_V, SLOT_SIZE, SLOT_SIZE, 256, 256);

                        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
                        if (sprite != null) {
                            graphics.blit(colX + 1, swatchY + 1, 0, TEXTURE_SWATCH_SIZE, TEXTURE_SWATCH_SIZE, sprite);
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
        }

        // 3D Preview
        ItemStack inputStack = this.menu.getContainer().getItem(CabinetMakerTableContainer.INPUT_SLOT);
        if (!inputStack.isEmpty()) {
            float rotY = (Minecraft.getInstance().level != null ? (Minecraft.getInstance().level.getGameTime() + partialTick) : 0.0f) % 360.0f;
            render3DPreview(graphics, this.leftPos + PREVIEW_BOX_X + (PREVIEW_BOX_SIZE / 2), this.topPos + PREVIEW_BOX_Y + (PREVIEW_BOX_SIZE / 2), PREVIEW_BOX_SIZE - 10, rotY, inputStack, chosenTextures);
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
        int keyCount = this.menu.getDynamicKeyCount();
        IDynamicTexturedBlock dynamicBlock = this.menu.getDynamicBlock();
        if (keyCount > 0 && dynamicBlock != null) {
            List<String> keys = dynamicBlock.getTextureKeys();

            for (int keyIdx = 0; keyIdx < keyCount && keyIdx < keys.size(); keyIdx++) {
                List<ResourceLocation> textures = availableTexturesPerKey.getOrDefault(keyIdx, List.of());
                int colX = this.leftPos + MATERIAL_SLOT_X + (keyIdx * SLOT_SIZE + 3);
                int startY = this.topPos + MATERIAL_SLOT_Y + SLOT_SIZE + 3;

                // Texture Swatch Tooltips
                for (int tIdx = 0; tIdx < textures.size() && tIdx < 6; tIdx++) {
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
            }
        }
    }

    public static void render3DPreview(GuiGraphics guiGraphics, int x, int y, int size, float rotationY, ItemStack shelfStack, Map<String, ResourceLocation> textures) {
        if (shelfStack.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BakedModel baseModel = mc.getItemRenderer().getModel(shelfStack, mc.level, mc.player, 0);

        BakedModel modelToRender = baseModel;
        if (baseModel instanceof DynamicShelfBakedModel dsbm) {
            if (!textures.isEmpty()) {
                modelToRender = new DynamicShelfBakedModel.DynamicShelfItemBakedModel(dsbm, textures);
            }
        } else if (baseModel instanceof DynamicShelfBakedModel.DynamicShelfItemBakedModel dsibm) {
            if (!textures.isEmpty()) {
                modelToRender = new DynamicShelfBakedModel.DynamicShelfItemBakedModel(dsibm.getParent(), textures);
            }
        } else if (shelfStack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof IDynamicTexturedBlock dtb) {
            DynamicShelfBakedModel tempDsbm = new DynamicShelfBakedModel(baseModel, dtb);
            if (!textures.isEmpty()) {
                modelToRender = new DynamicShelfBakedModel.DynamicShelfItemBakedModel(tempDsbm, textures);
            } else {
                modelToRender = tempDsbm;
            }
        }

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        poseStack.translate(x, y, 100.0f);
        poseStack.scale(size * 0.7f, -size * 0.7f, size * 0.7f);

        poseStack.mulPose(Axis.XP.rotationDegrees(25.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        MultiBufferSource.BufferSource bufferSource =  mc.renderBuffers().bufferSource();

        mc.getItemRenderer().render(
            shelfStack,
            ItemDisplayContext.NONE,
            false,
            poseStack,
            bufferSource,
            LightTexture.FULL_BLOCK,
            OverlayTexture.NO_OVERLAY,
            modelToRender
        );

        bufferSource.endBatch(RenderType.translucent());
        bufferSource.endBatch();

        Lighting.setupForFlatItems();
        poseStack.popPose();
    }
}

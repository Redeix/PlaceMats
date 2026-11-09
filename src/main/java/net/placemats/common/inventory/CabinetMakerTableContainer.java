package net.placemats.common.inventory;

import java.util.Map;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.placemats.client.gui.CabinetMakerTableScreen;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.placemats.common.data.PlaceMatBlocks;
import net.placemats.common.data.PlaceMatContainers;
import net.placemats.common.data.PlaceMatTags;
import org.jetbrains.annotations.NotNull;

public class CabinetMakerTableContainer extends AbstractContainerMenu {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int MATERIAL_SLOT_START = 2;
    public static final int MATERIAL_SLOT_COUNT = 5;
    public static final int TOTAL_CONTAINER_SLOTS = 7;

    public static final int INV_SLOT_START = 7;
    public static final int INV_SLOT_END = 43;

    private final ContainerLevelAccess access;
    @Getter private final Container container = new SimpleContainer(TOTAL_CONTAINER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            CabinetMakerTableContainer.this.slotsChanged(this);
        }
    };

    public CabinetMakerTableContainer(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public CabinetMakerTableContainer(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(PlaceMatContainers.CABINET_MAKER_TABLE.get(), containerId);
        this.access = access;

        // Input Shelf
        this.addSlot(new Slot(this.container, INPUT_SLOT, CabinetMakerTableScreen.INPUT_SLOT_X - 5, CabinetMakerTableScreen.INPUT_SLOT_Y + 1) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof BlockItem bi
                        && bi.getBlock() instanceof IDynamicTexturedBlock dtb
                        && dtb.getTextureKeyCount() > 0;
            }
        });

        // Output Customized Shelf
        this.addSlot(new Slot(this.container, OUTPUT_SLOT, CabinetMakerTableScreen.OUTPUT_SLOT_X - 5, CabinetMakerTableScreen.OUTPUT_SLOT_Y + 1) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                super.onTake(player, stack);
                access.execute((level, pos) ->
                        level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f));
            }
        });

        // Material Slots
        int matStartX = CabinetMakerTableScreen.MATERIAL_SLOT_X + 1;
        int matSpacing = CabinetMakerTableScreen.SLOT_SIZE + 3;
        for (int i = 0; i < MATERIAL_SLOT_COUNT; i++) {
            final int keyIndex = i;
            this.addSlot(new Slot(this.container, MATERIAL_SLOT_START + i, matStartX + (i * matSpacing), CabinetMakerTableScreen.MATERIAL_SLOT_Y + 1) {
                @Override
                public boolean isActive() {
                    return keyIndex < getDynamicKeyCount();
                }

                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return !stack.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST);
                }

                @Override
                public boolean mayPickup(@NotNull Player playerIn) {
                    return false;
                }
            });
        }

        // Player Inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, CabinetMakerTableScreen.INVENTORY_X + col * CabinetMakerTableScreen.SLOT_SIZE, CabinetMakerTableScreen.INVENTORY_Y + row * CabinetMakerTableScreen.SLOT_SIZE));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, CabinetMakerTableScreen.INVENTORY_X + col * CabinetMakerTableScreen.SLOT_SIZE, CabinetMakerTableScreen.INVENTORY_Y + (CabinetMakerTableScreen.SLOT_SIZE * 3) + 4));
        }
    }

    public int getDynamicKeyCount() {
        ItemStack input = this.container.getItem(INPUT_SLOT);
        if (input.getItem() instanceof BlockItem bi && bi.getBlock() instanceof IDynamicTexturedBlock dtb) {
            return dtb.getTextureKeyCount();
        }
        return 0;
    }

    public IDynamicTexturedBlock getDynamicBlock() {
        ItemStack input = this.container.getItem(INPUT_SLOT);
        if (input.getItem() instanceof BlockItem bi && bi.getBlock() instanceof IDynamicTexturedBlock dtb) {
            return dtb;
        }
        return null;
    }

    public void setGhostSlot(int slotIndex, ItemStack stack) {
        if (slotIndex >= 0 && slotIndex < getDynamicKeyCount()) {
            if (stack == null || stack.isEmpty()) {
                this.container.setItem(MATERIAL_SLOT_START + slotIndex, ItemStack.EMPTY);
            } else if (!stack.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST)) {
                ItemStack ghost = stack.copy();
                ghost.setCount(1);
                this.container.setItem(MATERIAL_SLOT_START + slotIndex, ghost);
            }
            broadcastChanges();
        }
    }

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= MATERIAL_SLOT_START && slotId < MATERIAL_SLOT_START + MATERIAL_SLOT_COUNT) {
            Slot slot = this.slots.get(slotId);
            if (!slot.isActive()) {
                return;
            }
            ItemStack carried = this.getCarried();
            if (carried.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                if (!carried.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST)) {
                    ItemStack ghost = carried.copy();
                    ghost.setCount(1);
                    slot.set(ghost);
                }
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public void executeCraft(Map<String, ResourceLocation> chosenTextures) {
        executeCraft(chosenTextures, false);
    }

    public void executeCraft(Map<String, ResourceLocation> chosenTextures, boolean transformAll) {
        ItemStack input = this.container.getItem(INPUT_SLOT);
        if (input.isEmpty() || !(input.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof IDynamicTexturedBlock dtb)) {
            return;
        }

        if (!CabinetMakerTableScreen.hasTextureChanges(input, dtb, chosenTextures)) {
            return;
        }

        int toCraft = transformAll ? input.getCount() : 1;

        ItemStack outputTemplate = input.copy();
        outputTemplate.setCount(1);
        IDynamicTexturedBlock.setDynamicTextures(outputTemplate, chosenTextures);

        ItemStack currentOutput = this.container.getItem(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            int craftedAmount = Math.min(toCraft, outputTemplate.getMaxStackSize());
            ItemStack newOutput = outputTemplate.copy();
            newOutput.setCount(craftedAmount);
            this.container.setItem(OUTPUT_SLOT, newOutput);
            input.shrink(craftedAmount);
            this.container.setItem(INPUT_SLOT, input);
            access.execute((level, pos) ->
                    level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f));
            broadcastChanges();
        } else if (ItemStack.isSameItemSameTags(currentOutput, outputTemplate)) {
            int space = currentOutput.getMaxStackSize() - currentOutput.getCount();
            int craftedAmount = Math.min(toCraft, space);
            if (craftedAmount > 0) {
                currentOutput.grow(craftedAmount);
                input.shrink(craftedAmount);
                this.container.setItem(INPUT_SLOT, input);
                access.execute((level, pos) ->
                        level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f));
                broadcastChanges();
            }
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, PlaceMatBlocks.CABINETMAKER_TABLE.get());
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        for (int i = 0; i < MATERIAL_SLOT_COUNT; i++) {
            this.container.setItem(MATERIAL_SLOT_START + i, ItemStack.EMPTY);
        }
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index == OUTPUT_SLOT) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, result);
            } else if (index < TOTAL_CONTAINER_SLOTS) {
                if (!this.moveItemStackTo(slotStack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (slotStack.getItem() instanceof BlockItem bi
                        && bi.getBlock() instanceof IDynamicTexturedBlock dtb
                        && dtb.getTextureKeyCount() > 0) {
                    if (!this.moveItemStackTo(slotStack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!slotStack.is(PlaceMatTags.Items.CABINET_MAKER_MATERIAL_BLACKLIST)) {
                    int activeKeys = getDynamicKeyCount();
                    for (int i = 0; i < activeKeys; i++) {
                        Slot matSlot = this.slots.get(MATERIAL_SLOT_START + i);
                        if (!matSlot.hasItem()) {
                            ItemStack ghost = slotStack.copy();
                            ghost.setCount(1);
                            matSlot.set(ghost);
                            return ItemStack.EMPTY;
                        }
                    }
                    return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return result;
    }
}

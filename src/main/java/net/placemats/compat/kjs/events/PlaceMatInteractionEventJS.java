package net.placemats.compat.kjs.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import lombok.Getter;

import net.placemats.common.block.PlaceMatInteractions;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.blockentity.PlaceMatBlockEntity.PlacedItem;

public class PlaceMatInteractionEventJS extends EventJS {
    @Getter
    private final Player player;
    private final ItemStack itemStack;
    @Getter
    private final int zoneIndex;
    @Getter
    private final BlockContainerJS block;
    @Getter
    private final PlaceMatBlockEntity blockEntity;
    private final PlacedItem placedItem;
    @Getter
    private boolean interactionHandled = false;

    public PlaceMatInteractionEventJS(Player player, PlacedItem placedItem, int zoneIndex, PlaceMatBlockEntity blockEntity) {
        this.player = player;
        this.placedItem = placedItem;
        this.itemStack = placedItem.stack;
        this.zoneIndex = zoneIndex;
        this.blockEntity = blockEntity;
        this.block = new BlockContainerJS(blockEntity.getLevel(), blockEntity.getBlockPos());
    }

    public ItemStack getItem() {
        return itemStack;
    }

    public Level getLevel() {
        return blockEntity.getLevel();
    }

    public void remove() {
        blockEntity.removeItem(placedItem);
        this.interactionHandled = true;
    }

    public void remove(int amount) {
        blockEntity.removeItem(placedItem, amount);
        this.interactionHandled = true;
    }

    public void pickup() {
        PlaceMatInteractions.onPickup(blockEntity, player, placedItem);
        this.interactionHandled = true;
    }

    public void pickupStack() {
        PlaceMatInteractions.onStackPickup(blockEntity, player, placedItem);
        this.interactionHandled = true;
    }

    public void eat() {
        PlaceMatInteractions.handleEatDrink(blockEntity, player, placedItem, blockEntity.getLevel());
        this.interactionHandled = true;
    }

    public void drink() {
        PlaceMatInteractions.handleEatDrink(blockEntity, player, placedItem, blockEntity.getLevel());
        this.interactionHandled = true;
    }

}

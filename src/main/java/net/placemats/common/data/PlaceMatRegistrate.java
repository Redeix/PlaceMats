package net.placemats.common.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.block.PlaceMatCardinalBlock;
import net.placemats.common.block.PlaceMatStairsBlock;
import net.placemats.compat.tfc.TFCCompat;

@SuppressWarnings("unused")
public class PlaceMatRegistrate extends AbstractRegistrate<PlaceMatRegistrate> {

    protected PlaceMatRegistrate(String modid) {
        super(modid);
    }

    public static PlaceMatRegistrate create(String modid) {
        return new PlaceMatRegistrate(modid);
    }

    public void register(IEventBus bus) {
        registerEventListeners(bus);
    }

    public <T> boolean isInCreativeTab(RegistryEntry<T> entry, RegistryEntry<CreativeModeTab> tab) {
        return isInCreativeTab(entry, tab.getKey());
    }

    public <T> boolean isInCreativeTab(RegistryEntry<T> entry, ResourceKey<CreativeModeTab> tab) {
        return false;
    }

    public <T extends Block> BlockBuilder<T, PlaceMatRegistrate> block(String name, NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return block(this, name, factory);
    }

    public <T extends Item> ItemBuilder<T, PlaceMatRegistrate> item(String name, NonNullFunction<Item.Properties, T> factory) {
        return item(this, name, factory);
    }

    public <T extends BlockEntity> BlockEntityBuilder<T, PlaceMatRegistrate> blockEntity(String name, BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return blockEntity(this, name, factory);
    }

    public <T extends PlaceMatBlock> BlockBuilder<T, PlaceMatRegistrate> placeMatBlock(String name, NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return block(name, factory);
    }

    public BlockBuilder<PlaceMatCardinalBlock, PlaceMatRegistrate> storageRack(String name, boolean cardinal) {
        return placeMatBlock(name, p -> (PlaceMatCardinalBlock) TFCCompat.INSTANCE.createPlaceMatBlock(p, cardinal));
    }

    public BlockBuilder<PlaceMatStairsBlock, PlaceMatRegistrate> ornateShelf(String name) {
        return placeMatBlock(name, p -> (PlaceMatStairsBlock) TFCCompat.INSTANCE.createPlaceMatStairsBlock(p));
    }
}

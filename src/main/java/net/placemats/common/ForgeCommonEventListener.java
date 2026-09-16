package net.placemats.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.placemats.PlaceMatMain;
import net.placemats.common.block.PlaceMatBlock;
import net.placemats.common.data.PlaceMatTags;
import net.placemats.common.event.PlaceMatInteractions;
import net.placemats.common.blockentity.PlaceMatBlockEntity;
import net.placemats.common.data.resource.DefinitionManager;

@Mod.EventBusSubscriber(modid = PlaceMatMain.MOD_ID)
public final class ForgeCommonEventListener {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof PlaceMatBlockEntity foodPlacer) {
            if (PlaceMatInteractions.handleLeftClick(foodPlacer, event.getEntity(), null)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickWithKey(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!held.is(PlaceMatTags.Items.KEY)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PlaceMatBlockEntity placeMat) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(PlaceMatBlock.LOCKED)) {
                if (level instanceof ServerLevel serverLevel) {
                    boolean isLocked = state.getValue(PlaceMatBlock.LOCKED);
                    boolean newLocked = !isLocked;
                    Player player = event.getEntity();

                    if (newLocked) {
                        placeMat.setLockedBy(player.getUUID());
                    } else {
                        placeMat.setLockedBy(null);
                    }
                    placeMat.setChanged();

                    level.setBlockAndUpdate(pos, state.setValue(PlaceMatBlock.LOCKED, newLocked));
                    level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 2f, 0.5f);
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            }
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DefinitionManager(false));
    }

}

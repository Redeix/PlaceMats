package net.placemats.common.datagen.definitions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.placemats.compat.tfc.TFCCompat;

public enum HasFluidCondition implements Condition {
    INSTANCE;

    @Override
    public ConditionType getType() {
        return ConditionType.HAS_FLUID;
    }

    @Override
    public boolean test(ConditionContext context) {
        ItemStack stack = context.stack();
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ItemStack single = stack.getCount() == 1 ? stack : stack.copyWithCount(1);

        // TFC isFluidContainer.
        if (TFCCompat.INSTANCE.isFluidContainer(single)) {
            return !TFCCompat.INSTANCE.getContainedFluid(single).isEmpty();
        }

        // FluidUtil.getFluidContained
        try {
            var contained = FluidUtil.getFluidContained(single);
            if (contained.isPresent() && !contained.get().isEmpty()) {
                return true;
            }
        } catch (Exception ignored) {}

        // FLUID_HANDLER_ITEM capability.
        LazyOptional<IFluidHandlerItem> cap = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (cap.isPresent()) {
            return cap.map(handler -> {
                for (int i = 0; i < handler.getTanks(); i++) {
                    if (!handler.getFluidInTank(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
            }).orElse(false);
        }

        // FLUID_HANDLER capability.
        LazyOptional<IFluidHandler> generalCap = single.getCapability(ForgeCapabilities.FLUID_HANDLER);
        if (generalCap.isPresent()) {
            return generalCap.map(handler -> {
                for (int i = 0; i < handler.getTanks(); i++) {
                    if (!handler.getFluidInTank(i).isEmpty()) {
                        return true;
                    }
                }
                return false;
            }).orElse(false);
        }

        // BucketItem.
        if (stack.getItem() instanceof BucketItem bucket) {
            return bucket.getFluid() != Fluids.EMPTY;
        }

        // MilkBucketItem.
        return stack.getItem() instanceof MilkBucketItem || stack.getItem() == Items.MILK_BUCKET;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
    }
}

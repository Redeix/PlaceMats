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
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.placemats.compat.tfc.TFCCompat;

public record IsFluidContainerCondition(boolean requireNotEmpty) implements Condition {

    public static final IsFluidContainerCondition ANY = new IsFluidContainerCondition(false);
    public static final IsFluidContainerCondition NOT_EMPTY = new IsFluidContainerCondition(true);

    @Override
    public ConditionType getType() {
        return ConditionType.IS_FLUID_CONTAINER;
    }

    @Override
    public boolean test(ConditionContext context) {
        ItemStack stack = context.stack();
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ItemStack single = stack.getCount() == 1 ? stack : stack.copyWithCount(1);

        if (TFCCompat.INSTANCE.isFluidContainer(single)) {
            if (requireNotEmpty) {
                return !TFCCompat.INSTANCE.getContainedFluid(single).isEmpty();
            }
            return true;
        }

        LazyOptional<IFluidHandlerItem> cap = single.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
        if (cap.isPresent()) {
            if (requireNotEmpty) {
                return cap.map(handler -> {
                    for (int i = 0; i < handler.getTanks(); i++) {
                        if (!handler.getFluidInTank(i).isEmpty()) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false);
            }
            return true;
        }

        if (stack.getItem() instanceof BucketItem bucket) {
            return !requireNotEmpty || bucket.getFluid() != Fluids.EMPTY;
        }

        return stack.getItem() instanceof MilkBucketItem || stack.getItem() == Items.MILK_BUCKET;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        if (requireNotEmpty) {
            json.addProperty("require_not_empty", true);
        }
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        buffer.writeBoolean(requireNotEmpty);
    }
}

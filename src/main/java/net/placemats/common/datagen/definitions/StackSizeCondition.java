package net.placemats.common.datagen.definitions;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.placemats.common.block.PlaceMatBlock;

public record StackSizeCondition(@Nullable Float percentage, @Nullable Integer count) implements Condition {

    public StackSizeCondition(float percentage) {
        this(percentage, null);
    }

    public StackSizeCondition(int count) {
        this(null, count);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.IS_STACK_SIZE;
    }

    @Override
    public boolean test(ConditionContext context) {
        ItemStack stack = context.stack();
        int currentCount = stack != null ? stack.getCount() : 0;
        if (count != null) {
            return currentCount >= count;
        }
        if (percentage != null) {
            int maxZoneStack = getMaxZoneStack(context, stack);
            float currentPercent = maxZoneStack > 0 ? ((float) currentCount) / (float) maxZoneStack : 0f;
            return currentPercent >= percentage;
        }
        return true;
    }

    public static int getMaxZoneStack(ConditionContext context, ItemStack stack)
    {
        int maxZoneStack = 64;
        if (context.range() != null && context.range().maxStackSize() > 0) {
            maxZoneStack = context.range().maxStackSize();
        } else if (context.state() != null && context.state().getBlock() instanceof PlaceMatBlock pmb) {
            maxZoneStack = pmb.getMaxStackSize();
        } else if (stack != null && !stack.isEmpty()) {
            maxZoneStack = stack.getMaxStackSize();
        }
        if (stack != null && !stack.isEmpty()) {
            maxZoneStack = Math.min(stack.getMaxStackSize(), maxZoneStack);
        }
        return maxZoneStack;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        if (percentage != null) json.addProperty("percentage", percentage);
        if (count != null) json.addProperty("count", count);
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        buffer.writeBoolean(percentage != null);
        if (percentage != null) {
            buffer.writeFloat(percentage);
        } else {
            buffer.writeInt(count != null ? count : 1);
        }
    }
}

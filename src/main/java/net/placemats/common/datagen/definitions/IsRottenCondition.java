package net.placemats.common.datagen.definitions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.placemats.compat.tfc.TFCCompat;

public enum IsRottenCondition implements Condition {
    INSTANCE;

    @Override
    public ConditionType getType() {
        return ConditionType.IS_ROTTEN;
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.stack() == null || context.stack().isEmpty()) {
            return false;
        }
        return TFCCompat.INSTANCE.isRotten(context.stack());
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

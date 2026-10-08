package net.placemats.common.datagen.definitions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

public record NotCondition(Condition condition) implements Condition {
    @Override
    public ConditionType getType() {
        return ConditionType.NOT;
    }

    @Override
    public boolean test(ConditionContext context) {
        return !condition.test(context);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        json.add("value", condition.toJson());
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        condition.encode(buffer);
    }
}

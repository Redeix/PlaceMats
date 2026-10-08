package net.placemats.common.datagen.definitions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

public record IsPlacementRangeCondition(int rangeIndex) implements Condition {

    @Override
    public ConditionType getType() {
        return ConditionType.IS_PLACEMENT_RANGE;
    }

    @Override
    public boolean test(ConditionContext context) {
        return context.rangeIndex() >= 0 && context.rangeIndex() == rangeIndex;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        json.addProperty("range", rangeIndex);
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        buffer.writeInt(rangeIndex);
    }
}

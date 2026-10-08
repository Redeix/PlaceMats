package net.placemats.common.datagen.definitions;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.placemats.compat.tfc.TFCCompat;

public record HasTraitCondition(ResourceLocation traitId) implements Condition {

    @Override
    public ConditionType getType() {
        return ConditionType.HAS_TRAIT;
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.stack() == null || context.stack().isEmpty()) {
            return false;
        }
        return TFCCompat.INSTANCE.hasTrait(context.stack(), traitId);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        json.addProperty("trait", traitId.toString());
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        buffer.writeResourceLocation(traitId);
    }
}

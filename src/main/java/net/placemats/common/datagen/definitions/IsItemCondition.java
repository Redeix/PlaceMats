package net.placemats.common.datagen.definitions;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public record IsItemCondition(
        @Nullable ResourceLocation itemId,
        @Nullable TagKey<Item> itemTag) implements Condition {

    @Override
    public ConditionType getType() {
        return ConditionType.IS_ITEM;
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.stack() == null || context.stack().isEmpty()) {
            return false;
        }
        if (itemId != null) {
            ResourceLocation actualId = ForgeRegistries.ITEMS.getKey(context.stack().getItem());
            return itemId.equals(actualId);
        }
        if (itemTag != null) {
            return context.stack().is(itemTag);
        }
        return false;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        if (itemTag != null) {
            json.addProperty("tag", itemTag.location().toString());
        } else if (itemId != null) {
            json.addProperty("item", itemId.toString());
        }
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        buffer.writeBoolean(itemTag != null);
        if (itemTag != null) {
            buffer.writeResourceLocation(itemTag.location());
        } else {
            buffer.writeResourceLocation(itemId != null ? itemId : ResourceLocation.fromNamespaceAndPath("minecraft", "air"));
        }
    }
}

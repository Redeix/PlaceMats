package net.placemats.common.datagen.definitions;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

public record IsPlacematBlockCondition(
        @Nullable ResourceLocation blockId,
        @Nullable TagKey<Block> blockTag) implements Condition {

    @Override
    public ConditionType getType() {
        return ConditionType.IS_PLACEMAT_BLOCK;
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.state() == null) {
            return false;
        }
        if (blockId != null) {
            ResourceLocation actualId = ForgeRegistries.BLOCKS.getKey(context.state().getBlock());
            return blockId.equals(actualId);
        }
        if (blockTag != null) {
            return context.state().is(blockTag);
        }
        return false;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("condition", getType().getId().toString());
        if (blockTag != null) {
            json.addProperty("tag", blockTag.location().toString());
        } else if (blockId != null) {
            json.addProperty("block", blockId.toString());
        }
        return json;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(getType());
        buffer.writeBoolean(blockTag != null);
        if (blockTag != null) {
            buffer.writeResourceLocation(blockTag.location());
        } else {
            buffer.writeResourceLocation(blockId != null ? blockId : ResourceLocation.fromNamespaceAndPath("minecraft", "air"));
        }
    }
}

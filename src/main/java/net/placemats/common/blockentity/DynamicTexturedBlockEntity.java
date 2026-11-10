package net.placemats.common.blockentity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base BlockEntity implementation for blocks with dynamic textures and colors.
 */
@SuppressWarnings("unused")
public abstract class DynamicTexturedBlockEntity extends BlockEntity implements IDynamicTexturedBlockEntity {

    protected final Map<String, ResourceLocation> dynamicTextures = new HashMap<>();
    protected final Map<String, Integer> dynamicColors = new HashMap<>();

    public DynamicTexturedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Map<String, ResourceLocation> getDynamicTextures() {
        return Collections.unmodifiableMap(this.dynamicTextures);
    }

    @Override
    public void setDynamicTextures(Map<String, ResourceLocation> textures) {
        this.dynamicTextures.clear();
        if (textures != null) {
            this.dynamicTextures.putAll(textures);
        }
        setChanged();
        if (this.level != null && this.level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public Map<String, Integer> getDynamicColors() {
        return Collections.unmodifiableMap(this.dynamicColors);
    }

    @Override
    public void setDynamicColors(Map<String, Integer> colors) {
        this.dynamicColors.clear();
        if (colors != null) {
            this.dynamicColors.putAll(colors);
        }
        setChanged();
        if (this.level != null && this.level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public @NotNull ModelData getModelData() {
        return getDynamicModelData(super.getModelData());
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        saveDynamicData(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        loadDynamicData(tag);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
            if (this.level != null && this.level.isClientSide) {
                requestModelDataUpdate();
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
}

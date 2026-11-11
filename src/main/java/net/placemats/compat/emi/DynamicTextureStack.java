package net.placemats.compat.emi;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.placemats.client.gui.util.BlockTextureExtractor;
import net.placemats.common.block.IDynamicTexturedBlock;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/**
 * Custom EmiStack implementation for displaying dynamic texture blocks in EMI recipes.
 */
public class DynamicTextureStack extends EmiStack {
    private final EmiIngredient sourceBaseBlocks;
    private final EmiIngredient sourceTextures;
    private final Map<Item, List<ResourceLocation>> textureCache = new HashMap<>();

    private Item lastBaseItem = null;
    private Item lastTextureItem = null;
    private ItemStack currentDisplayStack = ItemStack.EMPTY;
    private EmiStack cachedRenderStack = null;

    public DynamicTextureStack(EmiIngredient sourceBaseBlocks, EmiIngredient sourceTextures) {
        this.sourceBaseBlocks = sourceBaseBlocks;
        this.sourceTextures = sourceTextures;
    }

    private EmiStack getCurrentStack(EmiIngredient ingredient) {
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks.isEmpty()) {
            return EmiStack.EMPTY;
        }
        int index = (int) ((System.currentTimeMillis() / 1000L) % stacks.size());
        return stacks.get(index);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
        EmiStack activeBase = getCurrentStack(sourceBaseBlocks);
        EmiStack activeTexture = getCurrentStack(sourceTextures);

        Item baseItem = activeBase.isEmpty() ? null : activeBase.getItemStack().getItem();
        Item textureItem = activeTexture.isEmpty() ? null : activeTexture.getItemStack().getItem();

        if (cachedRenderStack == null || baseItem != lastBaseItem || textureItem != lastTextureItem) {
            lastBaseItem = baseItem;
            lastTextureItem = textureItem;

            if (baseItem != null) {
                currentDisplayStack = activeBase.getItemStack().copy();

                if (textureItem != null) {
                    ItemStack materialStack = activeTexture.getItemStack();
                    List<ResourceLocation> extracted = textureCache.computeIfAbsent(
                        textureItem,
                        item -> BlockTextureExtractor.extract16x16Textures(materialStack)
                    );

                    if (!extracted.isEmpty()) {
                        ResourceLocation primary = extracted.get(0);
                        ResourceLocation secondary = extracted.size() > 1 ? extracted.get(1) : primary;

                        Map<String, ResourceLocation> textures = new LinkedHashMap<>();
                        textures.put("0", primary);
                        textures.put("1", secondary);
                        IDynamicTexturedBlock.setDynamicTextures(currentDisplayStack, textures);
                    }
                }
            } else {
                currentDisplayStack = ItemStack.EMPTY;
            }

            cachedRenderStack = EmiStack.of(currentDisplayStack, this.amount);
        }

        cachedRenderStack.render(graphics, x, y, delta, flags);
    }

    @Override
    public EmiStack copy() {
        DynamicTextureStack copy = new DynamicTextureStack(sourceBaseBlocks, sourceTextures);
        copy.setAmount(this.amount);
        copy.setChance(this.chance);
        return copy;
    }

    @Override
    public boolean isEmpty() {
        return getItemStack().isEmpty();
    }

    @Override
    public CompoundTag getNbt() {
        return getItemStack().getTag();
    }

    @Override
    public Object getKey() {
        return getItemStack().getItem();
    }

    @Override
    public ResourceLocation getId() {
        return EmiStack.of(getItemStack()).getId();
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        return cachedRenderStack != null ? cachedRenderStack.getTooltip() : EmiStack.of(getItemStack(), this.amount).getTooltip();
    }

    @Override
    public Component getName() {
        return getItemStack().getHoverName();
    }

    @Override
    public ItemStack getItemStack() {
        if (!currentDisplayStack.isEmpty()) {
            return currentDisplayStack;
        }
        EmiStack fallback = getCurrentStack(sourceBaseBlocks);
        return fallback.isEmpty() ? ItemStack.EMPTY : fallback.getItemStack();
    }

    @Override
    public List<Component> getTooltipText()
    {
        return List.of();
    }
}

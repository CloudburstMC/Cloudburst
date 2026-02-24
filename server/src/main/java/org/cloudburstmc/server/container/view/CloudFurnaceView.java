package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.FurnaceBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link BlockFurnaceView} for furnace-like containers
 * (furnace, blast furnace, smoker).
 * Wraps a 3-slot container: slot 0 = smelting ingredient, slot 1 = fuel, slot 2 = result.
 *
 * <p>The {@code slotGroupType} parameter must be the correct variant token for the block
 * being opened ({@code SlotGroupTypes.FURNACE}, {@code SlotGroupTypes.BLAST_FURNACE}, or
 * {@code SlotGroupTypes.SMOKER}) so that {@link org.cloudburstmc.api.inventory.InventoryScreen#getSlots}
 * resolves correctly for each variant.</p>
 */
public class CloudFurnaceView extends CloudSlotGroupBase implements BlockFurnaceView {

    private static final int COOK_DURATION_TICKS = 200;
    private static final int COOK_DURATION_TICKS_FAST = 100;

    private final Block block;

    public CloudFurnaceView(SlotGroupType<BlockFurnaceView> slotGroupType, Block block, CloudContainer container) {
        super(slotGroupType, container);
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public Furnace getBlockEntity() {
        return (Furnace) block.getLevel().getBlockEntity(block.getPosition());
    }

    private FurnaceBlockEntity furnaceEntity() {
        return (FurnaceBlockEntity) block.getLevel().getBlockEntity(block.getPosition());
    }

    @Override
    public ItemStack getSmelting() {
        return getItem(0);
    }

    @Override
    public void setSmelting(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getFuel() {
        return getItem(1);
    }

    @Override
    public void setFuel(ItemStack item) {
        setItem(1, item);
    }

    @Override
    public ItemStack getResult() {
        return getItem(2);
    }

    @Override
    public void setResult(ItemStack item) {
        setItem(2, item);
    }

    @Override
    public int getCookProgress() {
        FurnaceBlockEntity entity = furnaceEntity();
        return entity != null ? entity.getCookTime() : 0;
    }

    @Override
    public void setCookProgress(int ticks) {
        FurnaceBlockEntity entity = furnaceEntity();
        if (entity != null) {
            entity.setCookTime(Math.max(0, Math.min(ticks, getCookDuration())));
        }
    }

    @Override
    public int getCookDuration() {
        if (type == SlotGroupTypes.BLAST_FURNACE || type == SlotGroupTypes.SMOKER) {
            return COOK_DURATION_TICKS_FAST;
        }
        return COOK_DURATION_TICKS;
    }

    @Override
    public int getBurnProgress() {
        FurnaceBlockEntity entity = furnaceEntity();
        return entity != null ? entity.getBurnTime() : 0;
    }

    @Override
    public void setBurnProgress(int ticks) {
        FurnaceBlockEntity entity = furnaceEntity();
        if (entity != null) {
            entity.setBurnTime(Math.max(0, ticks));
        }
    }

    @Override
    public int getBurnDuration() {
        FurnaceBlockEntity entity = furnaceEntity();
        return entity != null ? entity.getBurnDuration() : 0;
    }

    @Override
    public void setBurnDuration(int ticks) {
        FurnaceBlockEntity entity = furnaceEntity();
        if (entity != null) {
            entity.setBurnDuration(Math.max(0, ticks));
        }
    }
}

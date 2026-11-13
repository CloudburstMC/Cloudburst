package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.inventory.view.BlockFurnaceView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.FurnaceBlockEntity;

public class CloudFurnaceView extends CloudSlotGroupBase implements BlockFurnaceView {

    private final FurnaceBlockEntity furnace;

    public CloudFurnaceView(FurnaceBlockEntity furnace) {
        super(furnace.getSlotGroupType(), furnace.getContainer());
        this.furnace = furnace;
    }

    @Override
    public Block getBlock() {
        return this.furnace.getBlock();
    }

    @Override
    public Furnace getBlockEntity() {
        return this.furnace;
    }

    @Override
    public ItemStack getSmelting() {
        return this.furnace.getSmelting();
    }

    @Override
    public void setSmelting(ItemStack item) {
        this.furnace.setSmelting(item);
    }

    @Override
    public ItemStack getFuel() {
        return this.furnace.getFuel();
    }

    @Override
    public void setFuel(ItemStack item) {
        this.furnace.setFuel(item);
    }

    @Override
    public ItemStack getResult() {
        return this.furnace.getResult();
    }

    @Override
    public void setResult(ItemStack item) {
        this.furnace.setResult(item);
    }

    @Override
    public int getCookProgress() {
        return this.furnace.getCookProgress();
    }

    @Override
    public void setCookProgress(int ticks) {
        this.furnace.setCookProgress(ticks);
    }

    @Override
    public int getCookDuration() {
        return this.furnace.getCookDuration();
    }

    @Override
    public int getBurnProgress() {
        return this.furnace.getBurnProgress();
    }

    @Override
    public void setBurnProgress(int ticks) {
        this.furnace.setBurnProgress(ticks);
    }

    @Override
    public int getBurnDuration() {
        return this.furnace.getBurnDuration();
    }

    @Override
    public void setBurnDuration(int ticks) {
        this.furnace.setBurnDuration(ticks);
    }
}

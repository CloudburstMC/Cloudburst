package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.inventory.view.BlockBrewingStandView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.BrewingStandBlockEntity;

public class CloudBrewingStandView extends CloudSlotGroupBase implements BlockBrewingStandView {

    private final BrewingStandBlockEntity stand;

    public CloudBrewingStandView(BrewingStandBlockEntity stand) {
        super(SlotGroupTypes.BREWING_STAND, stand.getContainer());
        this.stand = stand;
    }

    @Override
    public Block getBlock() {
        return this.stand.getBlock();
    }

    @Override
    public BrewingStand getBlockEntity() {
        return this.stand;
    }

    @Override
    public ItemStack getIngredient() {
        return this.stand.getIngredient();
    }

    @Override
    public void setIngredient(ItemStack item) {
        this.stand.setIngredient(item);
    }

    @Override
    public ItemStack getFuel() {
        return this.stand.getFuel();
    }

    @Override
    public void setFuel(ItemStack item) {
        this.stand.setFuel(item);
    }

    @Override
    public ItemStack getBottle(int slot) {
        return this.stand.getBottle(slot);
    }

    @Override
    public void setBottle(int slot, ItemStack item) {
        this.stand.setBottle(slot, item);
    }

    @Override
    public int getMaxStackSize(int slot) {
        return this.stand.getMaxStackSize(slot);
    }

    @Override
    public int getBrewProgress() {
        return this.stand.getBrewProgress();
    }

    @Override
    public void setBrewProgress(int ticks) {
        this.stand.setBrewProgress(ticks);
    }

    @Override
    public int getBrewDuration() {
        return this.stand.getBrewDuration();
    }

    @Override
    public int getFuelLevel() {
        return this.stand.getFuelLevel();
    }

    @Override
    public void setFuelLevel(int level) {
        this.stand.setFuelLevel(level);
    }

    @Override
    public int getMaxFuelLevel() {
        return this.stand.getMaxFuelLevel();
    }
}

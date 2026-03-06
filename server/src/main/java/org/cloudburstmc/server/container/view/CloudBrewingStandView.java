package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BrewingStand;
import org.cloudburstmc.api.inventory.view.BlockBrewingStandView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.blockentity.BrewingStandBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link BlockBrewingStandView} for the brewing stand container.
 *
 * <p>Wraps the 5-slot container in the block entity's canonical internal layout:</p>
 * <ul>
 *   <li>slot 0: ingredient (the item being brewed)</li>
 *   <li>slots 1-3: potion bottles (bottle 0 at index 1, bottle 1 at index 2, bottle 2 at index 3)</li>
 *   <li>slot 4: fuel (blaze powder)</li>
 * </ul>
 */
public class CloudBrewingStandView extends CloudSlotGroupBase implements BlockBrewingStandView {

    private final Block block;

    public CloudBrewingStandView(Block block, CloudContainer container) {
        super(SlotGroupTypes.BREWING_STAND, container);
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BrewingStand getBlockEntity() {
        return (BrewingStand) block.getLevel().getBlockEntity(block.getPosition());
    }

    @Override
    public ItemStack getIngredient() {
        return getItem(0);
    }

    @Override
    public void setIngredient(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getFuel() {
        return getItem(4);
    }

    @Override
    public void setFuel(ItemStack item) {
        setItem(4, item);
    }

    @Override
    public ItemStack getBottle(int slot) {
        if (slot < 0 || slot > 2) {
            throw new IndexOutOfBoundsException("Bottle slot must be 0–2, got " + slot);
        }
        return getItem(slot + 1);
    }

    @Override
    public void setBottle(int slot, ItemStack item) {
        if (slot < 0 || slot > 2) {
            throw new IndexOutOfBoundsException("Bottle slot must be 0–2, got " + slot);
        }
        setItem(slot + 1, item);
    }

    private BrewingStandBlockEntity brewingBlockEntity() {
        BrewingStand be = getBlockEntity();
        if (be instanceof BrewingStandBlockEntity entity) {
            return entity;
        }
        return null;
    }

    @Override
    public int getBrewProgress() {
        BrewingStandBlockEntity be = brewingBlockEntity();
        if (be == null) return 0;
        return BrewingStandBlockEntity.MAX_COOK_TIME - be.getCookTime();
    }

    @Override
    public void setBrewProgress(int ticks) {
        BrewingStandBlockEntity be = brewingBlockEntity();
        if (be != null) {
            int clamped = Math.max(0, Math.min(ticks, BrewingStandBlockEntity.MAX_COOK_TIME));
            be.setCookTime(BrewingStandBlockEntity.MAX_COOK_TIME - clamped);
        }
    }

    @Override
    public int getBrewDuration() {
        return BrewingStandBlockEntity.MAX_COOK_TIME;
    }

    @Override
    public int getFuelLevel() {
        BrewingStandBlockEntity be = brewingBlockEntity();
        return be != null ? be.getFuelAmount() : 0;
    }

    @Override
    public void setFuelLevel(int level) {
        BrewingStandBlockEntity be = brewingBlockEntity();
        if (be != null) {
            be.setFuelAmount(Math.max(0, level));
        }
    }

    @Override
    public int getMaxFuelLevel() {
        return 20;
    }
}

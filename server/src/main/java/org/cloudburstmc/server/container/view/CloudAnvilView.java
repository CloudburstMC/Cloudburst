package org.cloudburstmc.server.container.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.view.AnvilView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudAnvilView extends CloudSlotGroupBase implements AnvilView {

    private static final int DEFAULT_MAX_REPAIR_COST = 40;

    private int cost;
    private int repairItemCountCost;
    @Nullable
    private String renameText;
    private int maxRepairCost = DEFAULT_MAX_REPAIR_COST;
    private boolean bypassEnchantmentLevelRestriction;

    public CloudAnvilView() {
        super(SlotGroupTypes.ANVIL, new CloudContainer(3));
    }

    @Override
    public ItemStack getInput() {
        return getItem(0);
    }

    @Override
    public void setInput(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getMaterial() {
        return getItem(1);
    }

    @Override
    public void setMaterial(ItemStack item) {
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
    public int getRepairCost() {
        return cost;
    }

    @Override
    public void setRepairCost(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Repair cost must be non-negative, got: " + cost);
        }
        this.cost = cost;
    }

    @Override
    public int getRepairItemCountCost() {
        return repairItemCountCost;
    }

    @Override
    public void setRepairItemCountCost(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Repair item count cost must be non-negative, got: " + count);
        }
        this.repairItemCountCost = count;
    }

    @Override
    public @Nullable String getRenameText() {
        return renameText;
    }

    @Override
    public void setRenameText(@Nullable String text) {
        this.renameText = text;
    }

    @Override
    public int getMaximumRepairCost() {
        return maxRepairCost;
    }

    @Override
    public void setMaximumRepairCost(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Maximum repair cost must be non-negative, got: " + cost);
        }
        this.maxRepairCost = cost;
    }

    @Override
    public boolean bypassesEnchantmentLevelRestriction() {
        return bypassEnchantmentLevelRestriction;
    }

    @Override
    public void bypassEnchantmentLevelRestriction(boolean bypass) {
        this.bypassEnchantmentLevelRestriction = bypass;
    }
}

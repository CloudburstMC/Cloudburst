package org.cloudburstmc.server.container.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.view.EnchantingView;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

public class CloudEnchantingView extends CloudSlotGroupBase implements EnchantingView {

    public static final int INPUT_SLOT = 0;
    public static final int REAGENT_SLOT = 1;

    private final Runnable inputChanged;
    private final IntSupplier seed;
    private List<@Nullable EnchantmentOffer> offers = Collections.nCopies(3, null);

    public CloudEnchantingView(Runnable inputChanged, IntSupplier seed) {
        super(SlotGroupTypes.ENCHANTING, new CloudContainer(2));
        this.inputChanged = inputChanged;
        this.seed = seed;
    }

    @Override
    public ItemStack getInput() {
        return this.getItem(INPUT_SLOT);
    }

    @Override
    public void setInput(ItemStack item) {
        this.setItem(INPUT_SLOT, item);
    }

    @Override
    public ItemStack getReagent() {
        return this.getItem(REAGENT_SLOT);
    }

    @Override
    public void setReagent(ItemStack item) {
        this.setItem(REAGENT_SLOT, item);
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        ItemStack previous = this.getItem(slot);
        super.setItem(slot, item);
        if (slot == INPUT_SLOT && !previous.equals(this.getItem(slot))) {
            this.inputChanged.run();
        }
    }

    @Override
    public int getMaxStackSize(int slot) {
        return slot == INPUT_SLOT ? 1 : super.getMaxStackSize(slot);
    }

    @Override
    public List<@Nullable EnchantmentOffer> getOffers() {
        return this.offers;
    }

    public void updateOffers(List<@Nullable EnchantmentOffer> offers) {
        this.offers = Collections.unmodifiableList(new ArrayList<>(offers));
    }

    @Override
    public int getSeed() {
        return this.seed.getAsInt();
    }
}

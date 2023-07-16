package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.container.view.BlockContainerView;
import org.cloudburstmc.api.item.ItemStack;

public interface Furnace extends BlockEntity, BlockContainerView {

    ItemStack getResult();

    void setResult(ItemStack item);

    ItemStack getFuel();

    void setFuel(ItemStack item);

    ItemStack getSmelting();

    void setSmelting(ItemStack item);
}

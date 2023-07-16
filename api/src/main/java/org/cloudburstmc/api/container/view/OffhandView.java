package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.item.ItemStack;

public interface OffhandView extends EntityContainerView {

    ItemStack getOffhandItem();

    void setOffhandItem(ItemStack item);
}

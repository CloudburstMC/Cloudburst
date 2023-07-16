package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.item.ItemStack;

public interface EnchantView extends BlockContainerView {

    ItemStack getInput();

    void setInput(ItemStack item);

    ItemStack getOutput();

    void setOutput(ItemStack item);

    ItemStack getReagent();

    void setReagent(ItemStack item);
}

package org.cloudburstmc.server.item.randomitem;

import org.cloudburstmc.api.item.ItemStack;

/**
 * Created by Snake1999 on 2016/1/15.
 * Package cn.nukkit.item.randomitem in project nukkit.
 */
public class ConstantItemSelector extends Selector {

    protected final ItemStack item;

    public ConstantItemSelector(ItemStack item, Selector parent) {
        super(parent);
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public Object select() {
        return getItem();
    }
}

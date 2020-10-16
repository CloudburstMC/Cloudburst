package org.cloudburstmc.server.item.randomitem;

import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemType;

/**
 * Created by Snake1999 on 2016/1/15.
 * Package cn.nukkit.item.randomitem in project nukkit.
 */
public class ConstantItemSelector extends Selector {

    protected final ItemStack item;

    public ConstantItemSelector(ItemType id, Selector parent) {
        this(id, 0, parent);
    }

    public ConstantItemSelector(ItemType id, Selector parent, Object... metadata) {
        this(id, 1, parent, metadata);
    }

    public ConstantItemSelector(ItemType id, int count, Selector parent, Object... metadata) {
        this(ItemStack.get(id, count, metadata), parent);
    }

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

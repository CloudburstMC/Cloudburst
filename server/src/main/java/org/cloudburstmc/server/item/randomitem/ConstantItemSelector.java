package org.cloudburstmc.server.item.randomitem;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.server.registry.CloudItemRegistry;

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
        this(CloudItemRegistry.get().getItem(id, count, metadata), parent);
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

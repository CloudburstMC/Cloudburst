package org.cloudburstmc.server.item.randomitem;

import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by Snake1999 on 2016/1/15.
 * Package cn.nukkit.item.randomitem in project nukkit.
 */
public class ConstantItemSelector extends Selector {

    protected final ItemStack item;

    public ConstantItemSelector(Identifier id, Selector parent) {
        this(id, 0, parent);
    }

    public ConstantItemSelector(Identifier id, Integer meta, Selector parent) {
        this(id, meta, 1, parent);
    }

    public ConstantItemSelector(Identifier id, Integer meta, int count, Selector parent) {
        this(ItemStack.get(id, meta, count), parent);
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

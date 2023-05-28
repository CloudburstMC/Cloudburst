package org.cloudburstmc.api.registry;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.behavior.ItemBehavior;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;

public interface ItemRegistry extends BehaviorRegistry<ItemType> {

    void register(ItemType type, ItemBehavior behavior, Identifier... identifiers) throws RegistryException;

    void registerCreativeItem(ItemStack item);

    BehaviorCollection getBehaviors(ItemType type);

    default <T> T getBehavior(ItemType type, BehaviorKey<?, T> key) {
        return getBehaviors(type).get(key);
    }

    Identifier getIdentifier(int runtimeId) throws RegistryException;

    ItemType getType(Identifier runtimeId, int data);

    ItemType getType(int runtimeId, int data);

    ImmutableList<Identifier> getItems();

}

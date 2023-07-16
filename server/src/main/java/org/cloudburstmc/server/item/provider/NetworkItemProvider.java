package org.cloudburstmc.server.item.provider;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;

import java.util.HashSet;
import java.util.Set;

public class NetworkItemProvider extends ItemDataProvider {

    private final ItemData data;

    public NetworkItemProvider(ItemStack item, ItemData data) {
        super(item, data.getTag());
        this.data = data;
    }

    @Override
    public Set<Identifier> getCanDestroy() {
        Set<Identifier> canDestroy = new HashSet<>();

        for (String type : data.getCanBreak()) {
            canDestroy.add(Identifier.parse(type));
        }

        return canDestroy;
    }

    @Override
    public Set<Identifier> getCanPlaceOn() {
        Set<Identifier> canDestroy = new HashSet<>();

        for (String type : data.getCanPlace()) {
            canDestroy.add(Identifier.parse(type));
        }

        return canDestroy;
    }
}

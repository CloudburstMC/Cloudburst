package org.cloudburstmc.server.item.provider;

import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.utils.Identifier;

import java.util.HashSet;
import java.util.Set;

public class NetworkItemProvider extends ItemDataProvider {

    private final ItemData data;

    public NetworkItemProvider(CloudItemStack item, ItemData data) {
        super(item, data.getTag());
        this.data = data;
    }

    @Override
    public Set<Identifier> getCanDestroy() {
        Set<Identifier> canDestroy = new HashSet<>();

        for (String type : data.getCanBreak()) {
            canDestroy.add(Identifier.fromString(type));
        }

        return canDestroy;
    }

    @Override
    public Set<Identifier> getCanPlaceOn() {
        Set<Identifier> canDestroy = new HashSet<>();

        for (String type : data.getCanPlace()) {
            canDestroy.add(Identifier.fromString(type));
        }

        return canDestroy;
    }
}

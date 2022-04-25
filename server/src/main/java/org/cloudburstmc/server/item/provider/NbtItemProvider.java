package org.cloudburstmc.server.item.provider;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class NbtItemProvider extends ItemDataProvider {

    private final Integer meta;

    public NbtItemProvider(ItemStack item, Integer meta, NbtMap nbt) {
        super(item, nbt);
        this.meta = meta;
    }

    @Override
    public Set<Identifier> getCanDestroy() {
        Set<Identifier> canDestroy = new HashSet<>();

        tag.listenForList("CanDestroy", NbtType.STRING, (types) -> {
            for (String type : types) {
                canDestroy.add(Identifier.fromString(type));
            }
        });

        return canDestroy;
    }

    @Override
    public Set<Identifier> getCanPlaceOn() {
        Set<Identifier> canDestroy = new HashSet<>();

        tag.listenForList("CanPlaceOn", NbtType.STRING, (types) -> {
            for (String type : types) {
                canDestroy.add(Identifier.fromString(type));
            }
        });

        return canDestroy;
    }
}

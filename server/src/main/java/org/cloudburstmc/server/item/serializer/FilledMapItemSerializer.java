package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.CloudServer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Resolves map presentation from the stored map rather than duplicating it on each item.
 */
public class FilledMapItemSerializer extends DefaultItemSerializer {

    private static final int LOCKED_VARIANT = 6;

    private final LongFunction<Optional<MapView>> maps;

    public FilledMapItemSerializer() {
        this(id -> {
            CloudServer server = CloudServer.getInstance();
            return server == null ? Optional.empty() : server.maps().get(id);
        });
    }

    public FilledMapItemSerializer(LongFunction<Optional<MapView>> maps) {
        this.maps = Objects.requireNonNull(maps, "maps");
    }

    @Override
    public int getAuxValue(ItemStack item) {
        return this.findMap(item).filter(MapView::isLocked).isPresent() ? LOCKED_VARIANT : 0;
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag) {
        super.serialize(item, tag);
        this.findMap(item).ifPresent(map -> tag.putByte("map_display_players", (byte) (map.isTrackingPosition() ? 1 : 0)));
    }

    private Optional<MapView> findMap(ItemStack item) {
        Long id = item.get(ItemDataComponents.MAP_ID);
        return id == null ? Optional.empty() : this.maps.apply(id);
    }
}

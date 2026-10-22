package org.cloudburstmc.server.map;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.event.server.MapInitializeEvent;
import org.cloudburstmc.api.inventory.view.CartographyView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.api.map.Maps;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.screen.CloudCartographyContainerScreen;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class CloudMapManager implements Maps {

    private static final Logger log = LoggerFactory.getLogger(CloudMapManager.class);
    private final CloudServer server;
    private final Path directory;
    private final Map<Long, CloudMapView> maps = new HashMap<>();
    private final Map<CloudPlayer, Set<Long>> subscriptions = new WeakHashMap<>();
    private final Map<CloudPlayer, Set<Long>> pendingRequests = new WeakHashMap<>();
    private final MapIdAllocator ids;

    public CloudMapManager(CloudServer server, Path directory) {
        this.server = server;
        this.directory = Objects.requireNonNull(directory, "directory");

        try {
            long minimumNextId = 0;
            if (!Files.notExists(this.directory)) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(this.directory, "map_*.dat")) {
                    for (Path file : files) {
                        String name = file.getFileName().toString();
                        try {
                            long id = Long.parseLong(name.substring(4, name.length() - 4));
                            if (id >= 0) {
                                NbtMap stored = MapDataFiles.read(file, id);
                                long maximumReservedId = Arrays.stream(stored.getLongArray("mapIds")).max().orElseThrow();
                                minimumNextId = Math.max(minimumNextId, Math.addExact(maximumReservedId, 1));
                            }
                        } catch (NumberFormatException ignored) {
                            log.warn("Ignoring map file with an invalid identifier: {}", file);
                        }
                    }
                }
            }

            this.ids = new MapIdAllocator(this.directory, minimumNextId);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize map storage", e);
        }
    }

    @Override
    public CloudMapView create(Level level, int x, int z, MapScale scale) {
        return this.create(level, x, z, scale, true);
    }

    public CloudMapView create(Level level, int x, int z, MapScale scale, boolean tracking) {
        Objects.requireNonNull(scale, "scale");
        if (!(level instanceof CloudLevel mappedLevel) || level.getServer() != this.server) {
            throw new IllegalArgumentException("Level belongs to a different server");
        }

        long[] mapIds = this.ids.allocate(MapScale.values().length - scale.ordinal());
        CloudMapView map = new CloudMapView(this, mapIds, level.getName(), mappedLevel.getDimension(),
                alignCenter(x, scale), alignCenter(z, scale), scale, new int[128 * 128]);
        this.maps.put(map.getId(), map);
        map.setTrackingPosition(tracking);
        map.changed();
        this.server.getEventManager().fire(new MapInitializeEvent(map));
        this.save(map);
        return map;
    }

    @Override
    public Optional<MapView> get(long id) {
        if (id < 0) {
            return Optional.empty();
        }

        CloudMapView existing = this.maps.get(id);
        if (existing != null) {
            return Optional.of(existing);
        }

        Path path = this.path(id);
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            NbtMap data = MapDataFiles.read(path, id);
            int[] pixels = data.getIntArray("pixels");
            int scale = data.getInt("scale");
            long[] mapIds = data.getLongArray("mapIds");

            CloudMapView map = new CloudMapView(this, mapIds, data.getString("level"), data.getInt("dimension"),
                    data.getInt("centerX"), data.getInt("centerZ"), MapScale.values()[scale], pixels);
            map.setLocked(data.getBoolean("locked"));
            map.setTrackingPosition(data.getBoolean("tracking"));
            map.setUnlimitedTracking(data.getBoolean("unlimitedTracking"));
            map.saved();

            this.maps.put(id, map);
            this.server.getEventManager().fire(new MapInitializeEvent(map));
            return Optional.of(map);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Could not load map " + id, e);
        }
    }

    @Override
    public CloudMapView copy(MapView original) {
        return this.copy(original, original.getScale(), original.isLocked(), original.isTrackingPosition());
    }

    public CloudMapView copy(MapView original, MapScale scale, boolean locked, boolean tracking) {
        Objects.requireNonNull(scale, "scale");
        if (!(original instanceof CloudMapView source) || this.maps.get(source.getId()) != source) {
            throw new IllegalArgumentException("Map belongs to a different server");
        }

        long[] mapIds = source.mapIds();
        int scaleDifference = scale.ordinal() - source.getScale().ordinal();
        if (scaleDifference > 0 && scaleDifference < mapIds.length
                && !this.maps.containsKey(mapIds[scaleDifference]) && !Files.exists(this.path(mapIds[scaleDifference]))) {
            mapIds = Arrays.copyOfRange(mapIds, scaleDifference, mapIds.length);
        } else {
            mapIds = this.ids.allocate(MapScale.values().length - scale.ordinal());
        }

        boolean scaling = scale != source.getScale();
        CloudMapView copy = new CloudMapView(this, mapIds, source.levelName(), source.dimension(),
                scaling ? alignCenter(source.getCenterX(), scale) : source.getCenterX(),
                scaling ? alignCenter(source.getCenterZ(), scale) : source.getCenterZ(), scale,
                scaling ? new int[128 * 128] : source.terrain().clone());
        copy.setLocked(locked);
        copy.setTrackingPosition(tracking);
        copy.setUnlimitedTracking(source.isUnlimitedTracking());

        this.maps.put(copy.getId(), copy);
        this.server.getEventManager().fire(new MapInitializeEvent(copy));
        this.save(copy);

        return copy;
    }

    public void request(CloudPlayer player, long id) {
        if (!player.spawned || !player.isOnline() || this.cannotView(player, id)) {
            return;
        }

        this.get(id).ifPresent(map -> {
            this.subscriptions.computeIfAbsent(player, ignored -> new HashSet<>()).add(id);
            this.pendingRequests.computeIfAbsent(player, ignored -> new HashSet<>()).add(id);
        });
    }

    public void sendLockedPreview(CloudPlayer player, long originalId, long previewId) {
        if (!player.spawned || !player.isOnline() || !player.isAlive() || player.isSpectator()
                || this.ids.isReserved(previewId)
                || !(player.getItemStackNetManager().getScreen() instanceof CloudCartographyContainerScreen screen)) {
            return;
        }

        CartographyView inputs = screen.getCartography();
        if (!Objects.equals(mapId(inputs.getInput()), originalId) && !Objects.equals(mapId(inputs.getAdditional()), originalId)) {
            return;
        }

        this.get(originalId).filter(map -> !map.isLocked()).ifPresent(map -> ((CloudMapView) map).sendLockedPreview(player, previewId));
    }

    public void tick() {
        for (CloudPlayer player : this.server.getOnlinePlayers().values()) {
            if (!player.spawned) {
                continue;
            }

            Set<Long> held = new HashSet<>();
            Long mainHand = mapId(player.getInventory().getSelectedItem());
            Long offhand = mapId(player.getOffhand().getOffhandItem());
            if (mainHand != null) {
                held.add(mainHand);
            }

            if (offhand != null) {
                held.add(offhand);
            }

            for (long id : held) {
                this.get(id).ifPresent(map -> ((CloudMapView) map).updateTerrain(player));
            }

            Set<Long> carried = visibleMapIds(player);
            Set<Long> requested = this.subscriptions.get(player);
            if (requested != null) {
                requested.removeIf(id -> !carried.contains(id) && this.cannotView(player, id));
                carried.addAll(requested);
            }

            for (long id : carried) {
                Set<Long> pending = this.pendingRequests.get(player);
                boolean force = pending != null && pending.remove(id);
                this.get(id).ifPresent(map -> ((CloudMapView) map).send(player, force));
            }

            this.pendingRequests.remove(player);
        }

        this.subscriptions.keySet().removeIf(player -> !player.isOnline());
        this.pendingRequests.keySet().removeIf(player -> !player.isOnline());
    }

    public void save() {
        for (CloudMapView map : this.maps.values()) {
            if (map.dirty()) {
                this.save(map);
            }
        }
    }

    private void save(CloudMapView map) {
        NbtMap data = NbtMap.builder()
                .putString("level", map.levelName())
                .putInt("dimension", map.dimension())
                .putInt("centerX", map.getCenterX())
                .putInt("centerZ", map.getCenterZ())
                .putInt("scale", Integer.numberOfTrailingZeros(map.getScale().getBlocksPerPixel()))
                .putLongArray("mapIds", map.mapIds())
                .putBoolean("locked", map.isLocked())
                .putBoolean("tracking", map.isTrackingPosition())
                .putBoolean("unlimitedTracking", map.isUnlimitedTracking())
                .putIntArray("pixels", map.terrain())
                .build();

        try {
            MapDataFiles.write(this.path(map.getId()), map.getId(), data);
            map.saved();
        } catch (IOException e) {
            throw new IllegalStateException("Could not save map " + map.getId(), e);
        }
    }

    private boolean cannotView(CloudPlayer player, long id) {
        if (visibleMapIds(player).contains(id)) {
            return false;
        }

        for (BlockEntity blockEntity : player.getLevel().getBlockEntities()) {
            if (blockEntity instanceof ItemFrame frame && Objects.equals(mapId(frame.getItem()), id)
                    && frame.getPosition().toFloat().distanceSquared(player.getPosition()) <= 128 * 128) {
                return false;
            }
        }

        return true;
    }

    public static boolean carries(Player player, long id) {
        if (Objects.equals(mapId(player.getOffhand().getOffhandItem()), id)) {
            return true;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (Objects.equals(mapId(item), id)) {
                return true;
            }
        }

        return false;
    }

    private static Set<Long> visibleMapIds(CloudPlayer player) {
        Set<Container> inventories = player.getItemStackNetManager().getAllInventories();
        inventories.add(player.getContainer());

        Set<Long> ids = new HashSet<>();
        Long offhandId = mapId(player.getOffhand().getOffhandItem());
        if (offhandId != null) {
            ids.add(offhandId);
        }

        for (Container inventory : inventories) {
            for (ItemStack item : inventory.getContents()) {
                Long id = mapId(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }

        return ids;
    }

    public static Long mapId(ItemStack item) {
        return item.getType() == ItemTypes.FILLED_MAP ? item.get(ItemDataComponents.MAP_ID) : null;
    }

    public static int alignCenter(int coordinate, MapScale scale) {
        int size = 128 * scale.getBlocksPerPixel();
        long cell = Math.floorDiv((long) coordinate + 64, size);
        return Math.toIntExact(cell * size + size / 2 - 64);
    }

    public CloudServer server() {
        return this.server;
    }

    public long[] allocateMapIds(long id, MapScale scale) {
        long[] ancestors = this.ids.allocate(MapScale.values().length - scale.ordinal() - 1);
        long[] mapIds = new long[ancestors.length + 1];
        mapIds[0] = id;
        System.arraycopy(ancestors, 0, mapIds, 1, ancestors.length);
        return mapIds;
    }

    private Path path(long id) {
        return this.directory.resolve("map_" + id + ".dat");
    }
}

package org.cloudburstmc.server.map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.map.MapCursor;
import org.cloudburstmc.api.map.MapRenderer;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import java.util.*;

public class CloudMapView implements MapView {

    private static final Logger log = LoggerFactory.getLogger(CloudMapView.class);

    private final CloudMapManager manager;
    private final long id;
    private final int[] terrain;

    private final List<MapRenderer> renderers = new ArrayList<>();
    private final Map<MapRenderer, Map<Player, CloudMapCanvas>> canvases = new IdentityHashMap<>();
    private final Map<CloudPlayer, SentMap> sent = new WeakHashMap<>();

    private long[] mapIds;
    private String levelName;
    private int dimension;
    private int centerX;
    private int centerZ;
    private MapScale scale;
    private boolean locked;
    private boolean tracking = true;
    private boolean unlimitedTracking;
    private boolean dirty;

    public CloudMapView(CloudMapManager manager, long[] mapIds, String levelName, int dimension, int centerX, int centerZ, MapScale scale, int[] terrain) {
        this.manager = manager;
        this.id = mapIds[0];
        this.mapIds = mapIds.clone();
        this.levelName = levelName;
        this.dimension = dimension;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.scale = scale;
        this.terrain = terrain;
        this.addRenderer(new MapTerrainRenderer());
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public ItemStack createItem() {
        return ItemStack.builder(ItemTypes.FILLED_MAP)
                .setData(ItemDataComponents.MAP_ID, this.id)
                .setData(ItemDataComponents.MAP_TRACKING_POSITION, this.tracking)
                .build();
    }

    @Override
    public @Nullable CloudLevel getLevel() {
        return this.manager.server().getLevelByName(this.levelName);
    }

    @Override
    public void setLevel(Level level) {
        Objects.requireNonNull(level, "level");
        if (!(level instanceof CloudLevel cloudLevel) || level.getServer() != this.manager.server()) {
            throw new IllegalArgumentException("Level belongs to a different server");
        }

        this.mapIds = this.manager.allocateMapIds(this.id, this.scale);
        this.levelName = level.getName();
        this.dimension = cloudLevel.getDimension();
        this.dirty = true;
        this.sent.clear();
    }

    @Override
    public int getCenterX() {
        return this.centerX;
    }

    @Override
    public int getCenterZ() {
        return this.centerZ;
    }

    @Override
    public void setCenterX(int x) {
        if (this.centerX == x) {
            return;
        }

        this.mapIds = this.manager.allocateMapIds(this.id, this.scale);
        this.centerX = x;
        this.changed();
    }

    @Override
    public void setCenterZ(int z) {
        if (this.centerZ == z) {
            return;
        }

        this.mapIds = this.manager.allocateMapIds(this.id, this.scale);
        this.centerZ = z;
        this.changed();
    }

    @Override
    public MapScale getScale() {
        return this.scale;
    }

    @Override
    public void setScale(MapScale scale) {
        if (this.scale == scale) {
            return;
        }

        Objects.requireNonNull(scale, "scale");
        long[] mapIds = this.manager.allocateMapIds(this.id, scale);
        this.scale = scale;
        this.mapIds = mapIds;
        this.changed();
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public void setLocked(boolean locked) {
        if (this.locked == locked) {
            return;
        }

        this.locked = locked;
        this.changed();
    }

    @Override
    public boolean isTrackingPosition() {
        return this.tracking;
    }

    @Override
    public void setTrackingPosition(boolean tracking) {
        if (this.tracking == tracking) {
            return;
        }

        this.tracking = tracking;
        this.changed();
    }

    @Override
    public boolean isUnlimitedTracking() {
        return this.unlimitedTracking;
    }

    @Override
    public void setUnlimitedTracking(boolean unlimited) {
        if (this.unlimitedTracking == unlimited) {
            return;
        }

        this.unlimitedTracking = unlimited;
        this.changed();
    }

    @Override
    public List<MapRenderer> getRenderers() {
        return List.copyOf(this.renderers);
    }

    @Override
    public void addRenderer(MapRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        if (this.canvases.containsKey(renderer)) {
            return;
        }

        this.renderers.add(renderer);
        this.canvases.put(renderer, new WeakHashMap<>());
        renderer.initialize(this);
    }

    @Override
    public boolean removeRenderer(MapRenderer renderer) {
        if (this.canvases.remove(renderer) == null) {
            return false;
        }

        this.renderers.removeIf(existing -> existing == renderer);
        return true;
    }

    @Override
    public void send(Player player) {
        if (!(player instanceof CloudPlayer recipient) || player.getServer() != this.manager.server()) {
            throw new IllegalArgumentException("Player belongs to a different server");
        }

        this.send(recipient, true);
    }

    public void initializeTerrain(CloudPlayer player) {
        this.updateTerrain(player, true);
    }

    public void updateTerrain(CloudPlayer player) {
        this.updateTerrain(player, false);
    }

    private void updateTerrain(CloudPlayer player, boolean initial) {
        CloudLevel level = this.getLevel();
        if (this.locked || level == null || player.getLevel() != level) {
            return;
        }

        for (MapRenderer renderer : List.copyOf(this.renderers)) {
            if (renderer instanceof MapTerrainRenderer terrainRenderer) {
                terrainRenderer.updateTerrain(this, level, player, initial);
            }
        }
    }

    public void send(CloudPlayer player, boolean force) {
        if (!player.isOnline() || !player.spawned) {
            return;
        }

        SentMap rendered = this.render(player);
        SentMap previous = force ? null : this.sent.get(player);
        ClientboundMapItemDataPacket packet = MapPacketFactory.create(this, this.dimension, this.mapIds, rendered.pixels(), rendered.cursors(), previous);
        if (packet != null) {
            player.sendPacket(packet);
        }
        this.sent.put(player, rendered);
    }

    public void sendLockedPreview(CloudPlayer player, long previewId) {
        SentMap rendered = this.render(player);
        player.sendPacket(MapPacketFactory.createLockedCopy(this, this.dimension, this.mapIds, rendered.pixels(), previewId));
    }

    private SentMap render(CloudPlayer player) {
        int[] pixels = new int[128 * 128];
        List<MapCursor> cursors = new ArrayList<>();

        for (MapRenderer renderer : List.copyOf(this.renderers)) {
            Map<Player, CloudMapCanvas> contexts = this.canvases.get(renderer);
            if (contexts == null) {
                continue;
            }

            CloudMapCanvas canvas = contexts.computeIfAbsent(renderer.isContextual() ? player : null, ignored -> new CloudMapCanvas(this));
            canvas.setBase(pixels.clone());

            try {
                renderer.render(this, canvas, player);
            } catch (RuntimeException e) {
                log.error("Could not render map {} using {}", this.id, renderer.getClass().getName(), e);
                continue;
            }

            canvas.compositeInto(pixels);
            cursors.addAll(canvas.getCursors());
        }

        return new SentMap(pixels, List.copyOf(cursors));
    }

    public void changed() {
        this.dirty = true;
        this.sent.clear();
    }

    public int[] terrain() {
        return this.terrain;
    }

    public long[] mapIds() {
        return this.mapIds.clone();
    }

    public boolean updatePixel(int x, int y, int color) {
        int index = y * 128 + x;
        if (this.terrain[index] == color) {
            return false;
        }

        this.terrain[index] = color;
        this.dirty = true;
        return true;
    }

    public String levelName() {
        return this.levelName;
    }

    public int dimension() {
        return this.dimension;
    }

    public boolean dirty() {
        return this.dirty;
    }

    public void saved() {
        this.dirty = false;
    }

    public record SentMap(int[] pixels, List<MapCursor> cursors) {
    }
}

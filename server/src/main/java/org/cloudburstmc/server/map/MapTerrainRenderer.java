package org.cloudburstmc.server.map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.item.ItemTags;
import org.cloudburstmc.api.map.*;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public class MapTerrainRenderer extends MapRenderer {

    private final Map<Player, Integer> steps = new WeakHashMap<>();

    public MapTerrainRenderer() {
        super(true);
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player viewer) {
        CloudMapView map = (CloudMapView) view;
        CloudLevel level = map.getLevel();

        ((CloudMapCanvas) canvas).copyTerrain(map.terrain());
        canvas.getCursors().clear();

        if (level == null || !map.isTrackingPosition()) {
            return;
        }

        for (CloudPlayer holder : level.getServer().getOnlinePlayers().values()) {
            boolean carriesCopy = CloudMapManager.carries(holder, map.getId());
            boolean concealed = CloudItemRegistry.get().getTag(ItemTags.MAP_INVISIBILITY_EQUIPMENT).isTagged(holder.getArmor().getHelmet().getType());
            if (!((CloudPlayer) viewer).canSee(holder) || (!carriesCopy && (holder.isSpectator() || concealed))) {
                continue;
            }

            int holderDimension = holder.getLevel().getDimension();
            if (holderDimension == map.dimension() && holder.getLevel() != level) {
                continue;
            }

            MarkerPosition position = projectPlayer(map.dimension(), holderDimension, holder.getX(), holder.getZ(), level.getSpawnLocation());
            if (position == null) {
                continue;
            }

            float yaw = holder.getYaw();
            if (map.dimension() == CloudLevel.DIMENSION_NETHER) {
                int time = level.getServer().getTick() / 10;
                yaw = ((time * time * 34187121 + time * 121) >> 15 & 15) * 22.5f;
            }

            Color color = holder != viewer && carriesCopy ? Color.WHITE : position.color();
            MapCursor cursor = cursor(map, position.x(), position.z(), yaw, holder.getName(), MapCursorType.PLAYER, color, holder);
            if (cursor != null) {
                canvas.getCursors().add(cursor);
            }
        }

        for (BlockEntity blockEntity : level.getBlockEntities()) {
            if (blockEntity instanceof ItemFrame frame && Objects.equals(CloudMapManager.mapId(frame.getItem()), map.getId())) {
                MapCursor cursor = cursor(
                        map, frame.getPosition().getX(), frame.getPosition().getZ(),
                        Math.max(0, frame.getFacing().getHorizontalIndex()) * 90f,
                        "", MapCursorType.FRAME, Color.WHITE, null
                );

                if (cursor != null) {
                    canvas.getCursors().add(cursor);
                }
            }
        }
    }

    public void updateTerrain(CloudMapView map, CloudLevel level, CloudPlayer player, boolean initial) {
        int scale = map.getScale().getBlocksPerPixel();
        int playerX = (int) Math.floor((player.getX() - map.getCenterX()) / scale) + 64;
        int playerZ = (int) Math.floor((player.getZ() - map.getCenterZ()) / scale) + 64;
        int radius = 128 / scale;

        boolean ceiling = level.getDimension() == CloudLevel.DIMENSION_NETHER;
        if (ceiling) {
            radius /= 2;
        }

        int step = this.steps.merge(player, 1, Integer::sum);
        boolean consecutive = false;

        for (int x = Math.max(0, playerX - radius + 1); x < Math.min(128, playerX + radius); x++) {
            if (!initial && (x & 15) != (step & 15) && !consecutive) {
                continue;
            }

            consecutive = false;
            double previousHeight = 0;

            for (int y = Math.max(-1, playerZ - radius - 1); y < Math.min(128, playerZ + radius); y++) {
                int worldX = (map.getCenterX() / scale + x - 64) * scale;
                int worldZ = (map.getCenterZ() / scale + y - 64) * scale;
                Sample sample = sample(level, worldX, worldZ, scale, ceiling);
                if (sample == null) {
                    continue;
                }

                double difference = sample.water()
                        ? sample.waterDepth() * 0.1 + ((x + y) & 1) * 0.2
                        : (sample.height() - previousHeight) * 4 / (scale + 4) + (((x + y) & 1) - 0.5) * 0.4;
                int brightness = sample.water()
                        ? difference < 0.5 ? 255 : difference > 0.9 ? 180 : 220
                        : difference > 0.6 ? 255 : difference < -0.6 ? 180 : 220;

                previousHeight = sample.height();

                int distance = (x - playerX) * (x - playerX) + (y - playerZ) * (y - playerZ);
                boolean edge = distance > (radius - 2) * (radius - 2);
                if (y >= 0 && distance < radius * radius && (!edge || ((x + y) & 1) != 0)) {
                    consecutive |= map.updatePixel(x, y, shade(sample.color(), brightness));
                }
            }
        }
    }

    private static Sample sample(CloudLevel level, int x, int z, int scale, boolean ceiling) {
        if (level.getLoadedChunk(x >> 4, z >> 4) == null) {
            return null;
        }

        if (ceiling) {
            int noise = x + z * 231871;
            noise = noise * noise * 31287121 + noise * 11;
            Color color = ((noise >>> 20) & 1) == 0 ? BlockStates.DIRT.getMapColor() : BlockStates.STONE.getMapColor();

            return new Sample(color.getRGB(), 100, 0, false);
        }

        Map<Integer, Integer> counts = new LinkedHashMap<>();
        double height = 0;
        int depth = 0;
        int waterColor = BlockStates.WATER.getMapColor().getRGB();

        for (int dx = 0; dx < scale; dx++) {
            for (int dz = 0; dz < scale; dz++) {
                int wx = x + dx;
                int wz = z + dz;

                CloudChunk chunk = level.getLoadedChunk(wx >> 4, wz >> 4);
                if (chunk == null) {
                    return null;
                }

                int y = Math.clamp(chunk.getHighestBlock(wx & 15, wz & 15), level.getMinHeight(), level.getMaxHeight() - 1);
                BlockState state;

                do {
                    state = chunk.getBlockState(wx & 15, y, wz & 15, BlockLayer.PRIMARY);
                    BlockState liquid = chunk.getBlockState(wx & 15, y, wz & 15, BlockLayer.SECONDARY);

                    if (liquid.getType().isLiquid()
                            && !BlockSupport.isFaceSturdy(level, Vector3i.from(wx, y, wz), Direction.UP)) {
                        state = liquid;
                    }

                    if (state.getMapColor().getAlpha() != 0 || y <= level.getMinHeight()) {
                        break;
                    }

                    y--;
                } while (true);

                if (state.getType().isLiquid()) {
                    for (int below = y - 1; below > level.getMinHeight(); below--) {
                        depth++;

                        if (!chunk.getBlockState(wx & 15, below, wz & 15, BlockLayer.PRIMARY).getType().isLiquid()
                                && !chunk.getBlockState(wx & 15, below, wz & 15, BlockLayer.SECONDARY).getType().isLiquid()) {
                            break;
                        }
                    }
                }

                height += (double) y / (scale * scale);
                counts.merge(state.getMapColor().getRGB(), 1, Integer::sum);
            }
        }

        int color = 0;
        int maximum = 0;

        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maximum) {
                maximum = entry.getValue();
                color = entry.getKey();
            }
        }

        return new Sample(color, height, depth / (scale * scale), color == waterColor);
    }

    public static int shade(int color, int brightness) {
        return (color & 0xff000000)
                | (((color >>> 16) & 255) * brightness / 255 << 16)
                | (((color >>> 8) & 255) * brightness / 255 << 8)
                | ((color & 255) * brightness / 255);
    }

    public static @Nullable MarkerPosition projectPlayer(int mapDimension, int playerDimension, double x, double z, Vector3f spawn) {
        if (mapDimension == CloudLevel.DIMENSION_THE_END) {
            return playerDimension == CloudLevel.DIMENSION_THE_END ? new MarkerPosition(x, z, Color.MAGENTA) : null;
        }

        if (playerDimension == CloudLevel.DIMENSION_THE_END) {
            return mapDimension == CloudLevel.DIMENSION_OVERWORLD ? new MarkerPosition(spawn.getX(), spawn.getZ(), Color.MAGENTA) : null;
        }

        if (mapDimension == CloudLevel.DIMENSION_OVERWORLD && playerDimension == CloudLevel.DIMENSION_NETHER) {
            return new MarkerPosition(x * 8, z * 8, Color.RED);
        }

        if (mapDimension == CloudLevel.DIMENSION_NETHER && playerDimension == CloudLevel.DIMENSION_OVERWORLD) {
            return new MarkerPosition(x / 8, z / 8, Color.WHITE);
        }

        return new MarkerPosition(x, z, playerDimension == CloudLevel.DIMENSION_NETHER ? Color.RED : Color.WHITE);
    }

    private static MapCursor cursor(CloudMapView map, double worldX, double worldZ, float yaw,
                                   String name, MapCursorType type, Color color, @Nullable Player trackedPlayer) {
        double x = (worldX - map.getCenterX()) / map.getScale().getBlocksPerPixel();
        double y = (worldZ - map.getCenterZ()) / map.getScale().getBlocksPerPixel();
        int rotation = Math.floorMod((int) Math.floor(yaw * 16 / 360 + 0.5), 16);

        if (x < -63 || x > 63 || y < -63 || y > 63) {
            if (type == MapCursorType.FRAME) {
                return null;
            }

            type = MapCursorType.PLAYER_OFF_MAP;
            rotation = 0;

            if (Math.abs(x) >= 320 || Math.abs(y) >= 320) {
                if (!map.isUnlimitedTracking()) {
                    return null;
                }

                type = MapCursorType.PLAYER_OFF_LIMITS;
            }
        }

        return new MapCursor(
                (int) Math.clamp(Math.floor(x * 2 + 0.5), -128, 127),
                (int) Math.clamp(Math.floor(y * 2 + 0.5), -128, 127),
                rotation, type, name, color, trackedPlayer
        );
    }

    public record MarkerPosition(double x, double z, Color color) {
    }

    private record Sample(int color, double height, int waterDepth, boolean water) {
    }
}

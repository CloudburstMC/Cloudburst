package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.event.entity.EntityTeleportEndGatewayEvent;
import org.cloudburstmc.api.event.player.PlayerTeleportEndGatewayEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.EndGatewayBlockEntity;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.feature.EndGatewayFeature;
import org.cloudburstmc.server.level.feature.EndIslandFeature;

import java.util.Random;

@UtilityClass
public class EndGateways {

    private static final int OUTER_GATEWAY_DISTANCE = 1024;
    private static final int SAFE_EXIT_RADIUS = 5;

    public static boolean transfer(CloudEntity entity, Vector3i gateway) {
        CloudLevel level = entity.getLevel();
        EndGatewayBlockEntity blockEntity = getBlockEntity(level, gateway);
        if (blockEntity == null || blockEntity.isCoolingDown()) {
            return false;
        }

        blockEntity.startCooldown();
        Location exitLocation = blockEntity.getExitLocation();
        Vector3i exit = exitLocation == null ? null : exitLocation.getPosition().toInt();
        if (blockEntity.isExactTeleport() && level.getDimension() == CloudLevel.DIMENSION_THE_END && EndFightManager.isCentralGateway(gateway)) {
            blockEntity.setExitLocation(null);
            blockEntity.setExactTeleport(false);
            exit = null;
        }

        if (exit == null) {
            if (level.getDimension() != CloudLevel.DIMENSION_THE_END) {
                return false;
            }

            exit = outerDestination(level, gateway);
            blockEntity.setExitLocation(Location.from(exit, level));
        }

        Vector3i destination = blockEntity.isExactTeleport() ? exit : findSafeExit(level, exit);
        Location requested = Location.from(destination.toFloat().add(0.5f, 0, 0.5f), entity.getYaw(), entity.getPitch(), level);
        Location location = callTeleportEvent(entity, requested, blockEntity);
        if (location == null) {
            return false;
        }

        if (!entity.teleportWithoutEvent(location)) {
            return false;
        }

        entity.setMotion(Vector3f.ZERO);
        return true;
    }

    public static boolean isOutsideCentralIsland(Vector3i position) {
        long x = position.getX();
        long z = position.getZ();
        return x * x + z * z > (long) OUTER_GATEWAY_DISTANCE * OUTER_GATEWAY_DISTANCE;
    }

    private static Vector3i outerDestination(CloudLevel level, Vector3i gateway) {
        double length = Math.hypot(gateway.getX(), gateway.getZ());
        if (length == 0) {
            length = 1;
        }

        double directionX = gateway.getX() / length;
        double directionZ = gateway.getZ() / length;
        double x = directionX * OUTER_GATEWAY_DISTANCE;
        double z = directionZ * OUTER_GATEWAY_DISTANCE;

        for (int remaining = 16; remaining > 0 && !isChunkEmpty(level, x, z); remaining--) {
            x -= directionX * 16;
            z -= directionZ * 16;
        }

        for (int remaining = 16; remaining > 0 && isChunkEmpty(level, x, z); remaining--) {
            x += directionX * 16;
            z += directionZ * 16;
        }

        Vector3i surface = findValidSurfaceInChunk(level, x, z);
        if (surface == null) {
            surface = Vector3i.from((int) Math.floor(x + 0.5), 75, (int) Math.floor(z + 0.5));
            Random random = new Random(blockPositionSeed(surface));
            EndIslandFeature.place(level, surface, BlockStates.END_STONE, random, random.nextInt(3) + 4);
        }

        Vector3i gatewayPosition = findTallestBlock(level, surface, 16, true).add(0, 10, 0);
        EndGatewayFeature.place(level, gatewayPosition, gateway);
        return gatewayPosition;
    }

    private static Vector3i findSafeExit(CloudLevel level, Vector3i exit) {
        return findTallestBlock(level, exit.add(0, 2, 0), SAFE_EXIT_RADIUS, false).add(0, 1, 0);
    }

    private static EndGatewayBlockEntity getBlockEntity(CloudLevel level, Vector3i position) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof EndGatewayBlockEntity gateway) {
            return gateway;
        }
        return null;
    }

    private static boolean isChunkEmpty(CloudLevel level, double x, double z) {
        int chunkX = (int) Math.floor(x) >> 4;
        int chunkZ = (int) Math.floor(z) >> 4;
        level.getChunk(chunkX, chunkZ);

        for (int blockX = chunkX << 4; blockX < (chunkX << 4) + 16; blockX++) {
            for (int blockZ = chunkZ << 4; blockZ < (chunkZ << 4) + 16; blockZ++) {
                int y = level.getHighestBlock(blockX, blockZ);
                if (y >= level.getMinHeight()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static Vector3i findValidSurfaceInChunk(CloudLevel level, double x, double z) {
        int chunkX = (int) Math.floor(x) >> 4;
        int chunkZ = (int) Math.floor(z) >> 4;

        Vector3i closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int blockX = chunkX << 4; blockX < (chunkX << 4) + 16; blockX++) {
            for (int blockZ = chunkZ << 4; blockZ < (chunkZ << 4) + 16; blockZ++) {
                for (int y = level.getHighestBlock(blockX, blockZ); y >= 30; y--) {
                    Vector3i position = Vector3i.from(blockX, y, blockZ);
                    if (level.getBlockState(position).getType() != BlockTypes.END_STONE || !hasHeadroom(level,
                            position)) {
                        continue;
                    }

                    double centeredX = blockX + 0.5;
                    double centeredY = y + 0.5;
                    double centeredZ = blockZ + 0.5;
                    double distance = centeredX * centeredX + centeredY * centeredY + centeredZ * centeredZ;
                    if (distance < closestDistance) {
                        closest = position;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }

    private static Vector3i findTallestBlock(CloudLevel level, Vector3i origin, int radius, boolean allowBedrock) {
        Vector3i tallest = null;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0 && !allowBedrock) {
                    continue;
                }

                int blockX = origin.getX() + x;
                int blockZ = origin.getZ() + z;

                int minimumY = tallest == null ? level.getMinHeight() : tallest.getY();
                for (int y = level.getMaxHeight() - 1; y > minimumY; y--) {
                    Vector3i position = Vector3i.from(blockX, y, blockZ);
                    if (level.isFullBlock(position, level.getBlockState(position))
                            && (allowBedrock || level.getBlockState(position).getType() != BlockTypes.BEDROCK)) {
                        tallest = position;
                        break;
                    }
                }
            }
        }

        return tallest == null ? origin : tallest;
    }

    private static boolean hasHeadroom(CloudLevel level, Vector3i floor) {
        Vector3i above = floor.add(0, 1, 0);
        Vector3i aboveTwo = floor.add(0, 2, 0);
        return !level.isFullBlock(above, level.getBlockState(above)) && !level.isFullBlock(aboveTwo, level.getBlockState(aboveTwo));
    }

    private static long blockPositionSeed(Vector3i position) {
        return ((long) position.getX() & 0x3ffffffL) << 38
                | ((long) position.getZ() & 0x3ffffffL) << 12
                | (long) position.getY() & 0xfffL;
    }

    private static Location callTeleportEvent(CloudEntity entity, Location destination, EndGatewayBlockEntity gateway) {
        if (entity instanceof Player player) {
            PlayerTeleportEndGatewayEvent event = new PlayerTeleportEndGatewayEvent(player, entity.getLocation(), destination, gateway);
            entity.getServer().getEventManager().fire(event);
            return event.isCancelled() ? null : event.getTo();
        }

        EntityTeleportEndGatewayEvent event = new EntityTeleportEndGatewayEvent(entity, entity.getLocation(), destination, gateway);
        entity.getServer().getEventManager().fire(event);
        return event.isCancelled() ? null : event.getTo();
    }
}

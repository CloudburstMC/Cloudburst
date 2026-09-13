package org.cloudburstmc.server.level;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.event.player.PlayerRespawnEvent;
import org.cloudburstmc.api.event.player.PlayerRespawnFlag;
import org.cloudburstmc.api.event.player.PlayerRespawnReason;
import org.cloudburstmc.api.event.player.PlayerTeleportCause;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.feature.EndPlatformFeature;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.EnumSet;

@UtilityClass
public class EndPortals {

    public static boolean transfer(CloudEntity entity) {
        if (entity.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END && entity instanceof CloudPlayer player) {
            if (player.beginEndCredits()) {
                return true;
            }

            return returnPlayer(player);
        }

        Location destination = destination(entity);
        if (destination == null) {
            return false;
        }

        return teleport(entity, destination);
    }

    public static boolean returnPlayer(CloudPlayer player) {
        Location destination = destination(player);
        if (destination == null) {
            return false;
        }

        PlayerRespawnEvent event = new PlayerRespawnEvent(player, destination, PlayerRespawnReason.END_PORTAL, EnumSet.noneOf(PlayerRespawnFlag.class));
        player.getServer().getEventManager().fire(event);
        return teleport(player, event.getRespawnLocation());
    }

    private static boolean teleport(CloudEntity entity, Location destination) {
        if (!entity.teleport(destination, PlayerTeleportCause.END_PORTAL)) {
            return false;
        }

        entity.setMotion(Vector3f.ZERO);
        entity.getLevel().addSound(entity.getPosition(), Sound.PORTAL_TRAVEL);
        return true;
    }

    private static @Nullable Location destination(CloudEntity entity) {
        if (entity.getLevel().getDimension() == CloudLevel.DIMENSION_THE_END) {
            if (entity instanceof CloudPlayer player) {
                Location respawn = player.findRespawnPosition(false);
                if (respawn != null) {
                    return respawn;
                }
            }

            return entity.getServer().getDefaultLevel().getSafeSpawn();
        }

        CloudLevel end = EnumLevel.THE_END.getLevel();
        if (end == null) {
            return null;
        }

        loadPlatformChunks(end);
        EndPlatformFeature.rebuild(end);
        end.getEndFight().ensureStarted();
        float spawnOffsetY = entity instanceof CloudPlayer ? 0 : 1;
        return Location.from(EndPlatformFeature.SPAWN.toFloat().add(0.5f, spawnOffsetY, 0.5f), 90, 0, end);
    }

    private static void loadPlatformChunks(CloudLevel level) {
        int minChunkX = (EndPlatformFeature.SPAWN.getX() - EndPlatformFeature.RADIUS) >> 4;
        int maxChunkX = (EndPlatformFeature.SPAWN.getX() + EndPlatformFeature.RADIUS) >> 4;
        int minChunkZ = (EndPlatformFeature.SPAWN.getZ() - EndPlatformFeature.RADIUS) >> 4;
        int maxChunkZ = (EndPlatformFeature.SPAWN.getZ() + EndPlatformFeature.RADIUS) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }
}

package org.cloudburstmc.server.level;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.CloudServer;

@Log4j2
public enum EnumLevel {
    OVERWORLD,
    NETHER,
    THE_END;

    private static final double MAX_COORD = 29_999_984.0;
    private static final double NETHER_SCALE = 8.0;

    CloudLevel level;

    public static void initLevels() {
        OVERWORLD.level = CloudServer.getInstance().getDefaultLevel();

        CloudLevel netherLevel = CloudServer.getInstance().getLevelByName("nether");
        if (netherLevel != null && CloudServer.getInstance().isNetherAllowed()) {
            NETHER.level = netherLevel;
            NETHER.level.setDimension(CloudLevel.DIMENSION_NETHER);
        } else {
            log.warn("No level called \"nether\" found or nether is disabled in server properties! Nether functionality will be disabled.");
        }

        CloudLevel endLevel = CloudServer.getInstance().getLevelByName("the_end");
        if (endLevel != null && CloudServer.getInstance().isEndAllowed()) {
            THE_END.level = endLevel;
            THE_END.level.setDimension(CloudLevel.DIMENSION_THE_END);
        } else {
            log.warn("No level called \"the_end\" found or The End is disabled in server properties! The End functionality will be disabled.");
        }
    }

    /**
     * Returns the paired overworld/nether level for the given level.
     * Returns {@code null} if the pair is unavailable (nether disabled or
     * overworld not yet loaded), rather than throwing.
     *
     * @param current the level to look up the pair for
     * @return the paired level, or {@code null} if unavailable
     * @throws IllegalArgumentException if {@code current} is neither the
     *                                  overworld nor the nether
     */
    public static CloudLevel getOtherNetherPair(CloudLevel current) {
        if (current == OVERWORLD.level) {
            return NETHER.level;
        } else if (current == NETHER.level) {
            return OVERWORLD.level;
        } else {
            throw new IllegalArgumentException("Neither overworld nor nether given: " + current);
        }
    }

    /**
     * Compute the portal destination for an entity at the given world coordinates.
     * Scales X and Z by the 1:8 overworld/nether ratio, clamps the result to
     * {@link #MAX_COORD} on each axis, and clamps Y to the destination
     * dimension's valid build range.
     *
     * <p>Returns {@code null} if the nether is unavailable (disabled or not yet
     * loaded), or if {@code currentLevel} is not the overworld or nether.
     *
     * @param x            entity X position in the source level
     * @param y            entity Y position in the source level
     * @param z            entity Z position in the source level
     * @param yaw          entity yaw
     * @param pitch        entity pitch
     * @param currentLevel the level the entity is currently in
     * @return scaled destination Location, or {@code null} if unavailable
     */
    public static Location moveToNether(double x, double y, double z, float yaw, float pitch, CloudLevel currentLevel) {
        if (NETHER.level == null || OVERWORLD.level == null) {
            return null;
        }

        CloudLevel destLevel;
        double destX;
        double destZ;

        if (currentLevel == OVERWORLD.level) {
            destX = x / NETHER_SCALE;
            destZ = z / NETHER_SCALE;
            destLevel = NETHER.level;
        } else if (currentLevel == NETHER.level) {
            destX = x * NETHER_SCALE;
            destZ = z * NETHER_SCALE;
            destLevel = OVERWORLD.level;
        } else {
            return null;
        }

        destX = Math.max(-MAX_COORD, Math.min(MAX_COORD, destX));
        destZ = Math.max(-MAX_COORD, Math.min(MAX_COORD, destZ));

        int minY = destLevel.getMinHeight();
        int maxY = destLevel.getMaxHeight() - 1;
        double destY = Math.max(minY, Math.min(maxY, Math.floor(y)));

        return Location.from(Vector3f.from((float) destX, (float) destY, (float) destZ), yaw, pitch, destLevel);
    }

    public CloudLevel getLevel() {
        return level;
    }
}

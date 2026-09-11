package org.cloudburstmc.server.command.argument;

import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;

/**
 * Selector volume offset from an origin.
 *
 * <p>Negative dimensions extend below the origin on that axis. Positive dimensions extend above it.</p>
 *
 * @param dx x-axis extent
 * @param dy y-axis extent
 * @param dz z-axis extent
 */
public record CloudSelectorBox(float dx, float dy, float dz) {

    /**
     * Converts this relative selector volume to an absolute bounding box.
     *
     * @param origin selector origin
     * @return absolute bounding box covered by this selector volume
     */
    public BoundingBox absolute(Vector3f origin) {
        float minX = origin.getX() + Math.min(0, this.dx);
        float minY = origin.getY() + Math.min(0, this.dy);
        float minZ = origin.getZ() + Math.min(0, this.dz);
        float maxX = origin.getX() + Math.max(0, this.dx) + 1;
        float maxY = origin.getY() + Math.max(0, this.dy) + 1;
        float maxZ = origin.getZ() + Math.max(0, this.dz) + 1;
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
}

package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;

public record EntityDimensions(float width, float height, float eyeHeight) {
    public BoundingBox boundingBox(Vector3f position, float scale) {
        float radius = this.width * scale / 2;
        return new BoundingBox(
                position.getX() - radius, position.getY(), position.getZ() - radius,
                position.getX() + radius, position.getY() + this.height * scale, position.getZ() + radius
        );
    }
}

package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.util.BoundingBox;

public record EntityMovementSegment(BoundingBox fromBox, BoundingBox toBox) {

    public BoundingBox interpolate(float progress) {
        return new BoundingBox(
                interpolate(this.fromBox.getMinX(), this.toBox.getMinX(), progress),
                interpolate(this.fromBox.getMinY(), this.toBox.getMinY(), progress),
                interpolate(this.fromBox.getMinZ(), this.toBox.getMinZ(), progress),
                interpolate(this.fromBox.getMaxX(), this.toBox.getMaxX(), progress),
                interpolate(this.fromBox.getMaxY(), this.toBox.getMaxY(), progress),
                interpolate(this.fromBox.getMaxZ(), this.toBox.getMaxZ(), progress)
        );
    }

    public int steps() {
        float distance = Math.max(
                Math.max(Math.abs(this.toBox.getMinX() - this.fromBox.getMinX()), Math.abs(this.toBox.getMinY() - this.fromBox.getMinY())),
                Math.abs(this.toBox.getMinZ() - this.fromBox.getMinZ())
        );
        return Math.clamp((int) Math.ceil(distance * 4), 1, 16);
    }

    private static float interpolate(float from, float to, float progress) {
        return from + (to - from) * progress;
    }
}

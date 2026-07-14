package org.cloudburstmc.server.entity;

import org.cloudburstmc.math.vector.Vector3f;

public record EntityMovementResult(Vector3f requestedMovement, Vector3f resolvedMovement) {

    public boolean collidedHorizontally() {
        return this.requestedMovement.getX() != this.resolvedMovement.getX()
                || this.requestedMovement.getZ() != this.resolvedMovement.getZ();
    }

    public boolean collidedVertically() {
        return this.requestedMovement.getY() != this.resolvedMovement.getY();
    }

    public boolean collidedBelow() {
        return this.collidedVertically() && this.requestedMovement.getY() < 0;
    }
}

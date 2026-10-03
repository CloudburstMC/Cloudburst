package org.cloudburstmc.api.event.vehicle;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.math.vector.Vector3f;

import static java.util.Objects.requireNonNull;

/**
 * Reports a horizontal block collision after movement has been resolved.
 */
public class VehicleBlockCollisionEvent extends VehicleEvent {

    private final Block block;
    private final Vector3f velocity;

    /**
     * @param vehicle  the colliding vehicle
     * @param block    the block that stopped movement
     * @param velocity the velocity before collision resolution
     */
    public VehicleBlockCollisionEvent(Vehicle vehicle, Block block, Vector3f velocity) {
        super(vehicle);
        this.block = requireNonNull(block, "block");
        this.velocity = requireNonNull(velocity, "velocity");
    }

    /**
     * @return the block that stopped movement
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * @return the pre-collision velocity
     */
    public Vector3f getVelocity() {
        return this.velocity;
    }
}

package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.List;

/**
 * author: Angelic47
 * Nukkit Project
 */
public final class EntityExplodeEvent extends EntityEvent implements Cancellable {

    protected final Vector3f position;
    protected List<Block> blockStates;
    protected double yield;

    public EntityExplodeEvent(Entity entity, Vector3f position, List<Block> blockStates, double yield) {
        this.entity = entity;
        this.position = position;
        this.blockStates = blockStates;
        this.yield = yield;
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public List<Block> getBlockList() {
        return this.blockStates;
    }

    public void setBlockList(List<Block> blockStates) {
        this.blockStates = blockStates;
    }

    public double getYield() {
        return this.yield;
    }

    public void setYield(double yield) {
        this.yield = yield;
    }

}

package org.cloudburstmc.server.event.entity;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

import java.util.List;

/**
 * author: Angelic47
 * Nukkit Project
 */
public class EntityExplodeEvent extends EntityEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

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

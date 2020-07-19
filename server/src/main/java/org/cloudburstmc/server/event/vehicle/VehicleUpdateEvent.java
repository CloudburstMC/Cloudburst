package org.cloudburstmc.server.event.vehicle;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.HandlerList;

public class VehicleUpdateEvent extends VehicleEvent {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public VehicleUpdateEvent(Entity vehicle) {
        super(vehicle);
    }

}

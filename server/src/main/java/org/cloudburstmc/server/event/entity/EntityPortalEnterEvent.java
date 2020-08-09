package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

public class EntityPortalEnterEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final PortalType type;

    public static HandlerList getHandlers() {
        return handlers;
    }

    public EntityPortalEnterEvent(Entity entity, PortalType type) {
        this.entity = entity;
        this.type = type;
    }

    public PortalType getPortalType() {
        return type;
    }

    public enum PortalType {
        NETHER,
        END
    }
}

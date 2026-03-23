package org.cloudburstmc.api.event;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled();

    void setCancelled(boolean forceCancel);
}

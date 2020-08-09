package org.cloudburstmc.server.entity;

/**
 * Author: BeYkeRYkt
 * Nukkit Project
 */
public interface EntityOwnable {
    long getOwnerId();

    void setOwnerId(long ownerId);
}

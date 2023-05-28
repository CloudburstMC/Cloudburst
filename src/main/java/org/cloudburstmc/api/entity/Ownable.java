package org.cloudburstmc.api.entity;

/**
 * Author: BeYkeRYkt
 * Nukkit Project
 */
public interface Ownable {
    long getOwnerId();

    void setOwnerId(long ownerId);
}

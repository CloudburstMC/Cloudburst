package org.cloudburstmc.api.persistence;

/**
 * An object that can retain custom data across saves.
 */
public interface PersistentDataHolder {

    /**
     * Returns the live container for custom data on this object.
     *
     * @return the persistent data container
     */
    PersistentDataContainer getPersistentDataContainer();
}

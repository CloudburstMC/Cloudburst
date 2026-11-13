package org.cloudburstmc.api.persistence;

/**
 * An object that can retain custom data across saves.
 */
public interface PersistentDataHolder extends PersistentDataViewHolder {

    /**
     * Returns the container for custom data on this object. Changes made through
     * the container are saved with the object.
     *
     * @return the persistent data container
     */
    @Override
    PersistentDataContainer getPersistentDataContainer();
}

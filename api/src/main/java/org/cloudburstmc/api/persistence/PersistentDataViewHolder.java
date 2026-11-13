package org.cloudburstmc.api.persistence;

/**
 * An object whose custom persistent data can be read.
 */
public interface PersistentDataViewHolder {

    /**
     * Returns a view of the custom data on this object.
     *
     * @return the persistent data view
     */
    PersistentDataContainerView getPersistentDataContainer();
}

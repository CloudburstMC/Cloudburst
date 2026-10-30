package org.cloudburstmc.api.blockentity.data;

import lombok.EqualsAndHashCode;

/**
 * Persistent data of a dropper: exactly its container.
 */
@EqualsAndHashCode(callSuper = true)
public class DropperData extends ContainerData {

    public DropperData() {
    }
}

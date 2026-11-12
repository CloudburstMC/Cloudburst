package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;

/**
 * Called when a beacon becomes active.
 */
public class BeaconActivatedEvent extends BlockEvent {

    public BeaconActivatedEvent(Block beacon) {
        super(beacon);
    }
}

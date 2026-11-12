package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;

/**
 * Called when an active beacon stops working.
 */
public class BeaconDeactivatedEvent extends BlockEvent {

    public BeaconDeactivatedEvent(Block beacon) {
        super(beacon);
    }
}

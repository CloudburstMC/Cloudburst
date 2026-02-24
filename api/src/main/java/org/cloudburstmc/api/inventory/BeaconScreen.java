package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockBeaconView;

/**
 * Represents the beacon container screen.
 * Use {@link #getBeacon()} to access the payment slot section.
 */
public interface BeaconScreen extends ContainerScreen {

    BlockBeaconView getBeacon();
}

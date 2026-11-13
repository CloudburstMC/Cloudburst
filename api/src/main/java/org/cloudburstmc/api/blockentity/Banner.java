package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.item.data.BannerData;

/**
 * A placed banner with a base color and ordered pattern layers.
 */
public interface Banner extends BlockEntity {

    /**
     * Returns the banner's current design.
     */
    BannerData getBannerData();

    /**
     * Replaces the banner's design.
     *
     * @param data the new variant, base color, and pattern layers
     */
    void setBannerData(BannerData data);
}

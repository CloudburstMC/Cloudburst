package org.cloudburstmc.api.entity.vehicle;

/**
 * A storage minecart that collects items when enabled.
 */
public interface HopperMinecart extends ContainerMinecart {

    /**
     * Returns whether this minecart can collect items.
     */
    boolean isEnabled();

    /**
     * Sets whether this minecart can collect items. An activator rail overrides this state:
     * a powered rail disables collection, and an unpowered rail enables it.
     */
    void setEnabled(boolean enabled);
}

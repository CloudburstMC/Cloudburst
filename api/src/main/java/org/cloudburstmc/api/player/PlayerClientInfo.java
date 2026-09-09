package org.cloudburstmc.api.player;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * Client settings reported by a player during login.
 */
public interface PlayerClientInfo {

    /**
     * Returns the address the client used to join the server.
     *
     * @return the requested server address, or {@code null} when not reported
     */
    @Nullable
    String getServerAddress();

    /**
     * Returns the client-reported device model.
     *
     * @return the device model, or {@code null} when not reported
     */
    @Nullable
    String getDeviceModel();

    /**
     * Returns the platform running the client.
     *
     * @return the client platform
     */
    DevicePlatform getDevicePlatform();

    /**
     * Returns the client's game version.
     *
     * @return the game version, or {@code null} when not reported
     */
    @Nullable
    String getGameVersion();

    /**
     * Returns the GUI scale selected by the client.
     *
     * @return the GUI scale
     */
    int getGuiScale();

    /**
     * Returns the locale selected by the client.
     *
     * @return the client locale
     */
    Locale getLocale();

    /**
     * Returns the input method currently in use.
     *
     * @return the current input method
     */
    InputMode getCurrentInputMode();

    /**
     * Returns the device's default input method.
     *
     * @return the default input method
     */
    InputMode getDefaultInputMode();

    /**
     * Returns the user-interface layout selected by the client.
     *
     * @return the user-interface layout
     */
    UiProfile getUiProfile();
}

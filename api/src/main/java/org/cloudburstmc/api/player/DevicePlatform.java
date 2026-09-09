package org.cloudburstmc.api.player;

/**
 * A platform reported by a Bedrock client.
 */
public enum DevicePlatform {
    UNKNOWN("Unknown"),
    ANDROID("Android"),
    IOS("iOS"),
    MACOS("macOS"),
    AMAZON("Amazon"),
    GEAR_VR("Gear VR"),
    HOLOLENS("HoloLens"),
    WINDOWS("Windows"),
    WINDOWS_X86("Windows x86"),
    DEDICATED("Dedicated server"),
    TVOS("Apple TV"),
    PLAYSTATION("PlayStation"),
    SWITCH("Nintendo Switch"),
    XBOX("Xbox"),
    WINDOWS_PHONE("Windows Phone"),
    LINUX("Linux");

    private final String displayName;

    DevicePlatform(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns a human-readable platform name.
     *
     * @return the platform name
     */
    public String getDisplayName() {
        return displayName;
    }
}

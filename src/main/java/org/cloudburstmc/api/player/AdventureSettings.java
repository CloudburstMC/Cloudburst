package org.cloudburstmc.api.player;

public interface AdventureSettings {
    AdventureSettings set(AdventureSetting setting, boolean value);

    boolean get(AdventureSetting setting);

    void update();
}

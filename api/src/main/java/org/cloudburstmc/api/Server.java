package org.cloudburstmc.api;


import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.GameRuleRegistry;

public interface Server {

    String getName();

    String getVersion();

    String getImplementationVersion();

    void shutdown();

    boolean isRunning();

    PluginManager getPluginManager();

    GameRuleRegistry getGameRuleRegistry();

    int getTick();

    EventManager getEventManager();

    boolean getAllowFlight();
}

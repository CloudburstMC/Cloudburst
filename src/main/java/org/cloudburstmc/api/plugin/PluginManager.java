package org.cloudburstmc.api.plugin;

import java.util.Collection;
import java.util.Optional;

public interface PluginManager {

    <T extends PluginLoader> boolean registerLoader(Class<T> clazz, T loader);

    <T extends PluginLoader> boolean deregisterLoader(Class<T> clazz);

    Collection<PluginContainer> getAllPlugins();

    /**
     * Checks if the given plugin is loaded and returns it when applicable
     * <p>
     * Please note that the name of the plugin is case-sensitive
     *
     * @param id Name of the plugin to check
     * @return Plugin if it exists, otherwise null
     */
    Optional<PluginContainer> getPlugin(String id);

    Optional<PluginContainer> fromInstance(Object instance);

    /**
     * Checks if the given plugin is enabled or not
     *
     * @param id Plugin to check
     * @return true if the plugin is enabled, otherwise false
     */
    boolean isLoaded(String id);
}

package org.cloudburstmc.api.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface PluginDescription {

    /**
     * The ID for this plugin. This should be an alphanumeric name. Slashes are also allowed.
     *
     * @return the ID for this plugin
     */
    String getId();

    String getName();

    Optional<String> getDescription();

    /**
     * The path where the plugin is located on the file system.
     *
     * @return the path of this plugin
     */
    Optional<Path> getPath();

    /**
     * The author of this plugin.
     *
     * @return the plugin's author
     */
    List<String> getAuthors();

    /**
     * The version of this plugin.
     *
     * @return the version of this plugin
     */
    String getVersion();

    /**
     * The array of plugin IDs that this plugin requires in order to function fully.
     *
     * @return the dependencies
     */
    List<PluginDependency> getDependencies();

    /**
     * Plugin's website specified in the plugin.yml.
     *
     * @return website url
     */
    Optional<String> getUrl();

    PluginLoader getPluginLoader();
}

package org.cloudburstmc.server.plugin;

public interface PluginDependency {

    String getId();

    String getVersion();

    boolean isOptional();
}

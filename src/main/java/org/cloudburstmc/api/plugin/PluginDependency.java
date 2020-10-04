package org.cloudburstmc.api.plugin;

public interface PluginDependency {

    String getId();

    String getVersion();

    boolean isOptional();
}

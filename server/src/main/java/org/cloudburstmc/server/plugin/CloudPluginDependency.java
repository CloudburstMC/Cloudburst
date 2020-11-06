package org.cloudburstmc.server.plugin;

import lombok.NonNull;
import lombok.Value;
import org.cloudburstmc.api.plugin.PluginDependency;

@Value
public class CloudPluginDependency implements PluginDependency {
    @NonNull
    String id;
    @NonNull
    String version;
    boolean optional;
}

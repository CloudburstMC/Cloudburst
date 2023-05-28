package org.cloudburstmc.api.plugin;

import com.google.inject.Injector;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

public interface PluginLoader {

    PluginDescription loadPlugin(Path path) throws Exception;

    PluginContainer createPlugin(Injector injector, PluginDescription description) throws Exception;

    PathMatcher getPathMatcher();
}

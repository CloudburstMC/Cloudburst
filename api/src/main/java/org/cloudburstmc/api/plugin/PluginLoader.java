package org.cloudburstmc.api.plugin;

import com.google.inject.Injector;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

@ParametersAreNonnullByDefault
public interface PluginLoader {

    @Nonnull
    PluginDescription loadPlugin(Path path) throws Exception;

    @Nonnull
    PluginContainer createPlugin(Injector injector, PluginDescription description) throws Exception;

    @Nonnull
    PathMatcher getPathMatcher();
}

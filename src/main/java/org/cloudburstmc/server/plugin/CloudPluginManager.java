package org.cloudburstmc.server.plugin;

import com.google.inject.Injector;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.*;
import org.cloudburstmc.server.event.EventManager;
import org.cloudburstmc.server.plugin.util.DirectedAcyclicGraph;
import org.cloudburstmc.server.plugin.util.GraphException;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;

@Log4j2
@ParametersAreNonnullByDefault
@Singleton
public class CloudPluginManager implements PluginManager {
    private final Map<String, PluginContainer> plugins = new HashMap<>();
    private final Map<Object, PluginContainer> pluginInstances = new IdentityHashMap<>();
    private final Map<Class<?>, PluginLoader> loaders = new IdentityHashMap<>();

    private final Injector injector;
    private final EventManager eventManager;

    @Inject
    public CloudPluginManager(Injector injector, EventManager eventManager) {
        this.injector = injector;
        this.eventManager = eventManager;
    }

    public <T extends PluginLoader> boolean registerLoader(Class<T> clazz, T loader) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(loader, "loader");

        synchronized (loaders) {
            if (loaders.containsKey(clazz)) {
                return false;
            }

            loaders.put(clazz, loader);
        }
        return true;
    }

    @Override
    public <T extends PluginLoader> boolean deregisterLoader(Class<T> clazz) {
        synchronized (loaders) {
            return loaders.remove(clazz) != null;
        }
    }

    @Override
    public Collection<PluginContainer> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    @Override
    public Optional<PluginContainer> getPlugin(String id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(plugins.get(id));
    }

    @Override
    public Optional<PluginContainer> fromInstance(Object instance) {
        Objects.requireNonNull(instance, "instance");
        return Optional.ofNullable(pluginInstances.get(instance));
    }

    @Override
    public boolean isLoaded(String id) {
        Objects.requireNonNull(id, "id");
        return plugins.containsKey(id);
    }

    public void loadPlugins(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Path provided is not a directory");
        }

        Deque<PluginDescription> found = new ArrayDeque<>();

        synchronized (loaders) {
            for (PluginLoader loader : loaders.values()) {
                PathMatcher matcher = loader.getPathMatcher();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, p -> Files.isRegularFile(p) && matcher.matches(p))) {
                    for (Path path : stream) {
                        try {
                            found.add(loader.loadPlugin(path));
                        } catch (Exception e) {
                            log.error("Unable to load description plugin {}", path, e);
                        }
                    }
                }
            }
        }

        if (found.isEmpty()) {
            return;
        }

        Collection<PluginDescription> sorted = sortDescriptions(found);

        load:
        for (PluginDescription description : sorted) {

            for (PluginDependency dependency : description.getDependencies()) {
                Optional<PluginContainer> loadedPlugin = getPlugin(dependency.getId());
                if ((!loadedPlugin.isPresent() && !dependency.isOptional()) ||
                        (loadedPlugin.isPresent() && !loadedPlugin.get().getDescription().getVersion().equals(dependency.getVersion()))) {
                    log.error("Cannot load plugin {} due to missing dependency {}", description.getId(), dependency.getId());
                    continue load;
                }
            }

            PluginContainer plugin;
            try {
                plugin = description.getPluginLoader().createPlugin(this.injector, description);
            } catch (Exception e) {
                log.error("Cannot instantiate plugin {}", description.getId(), e);
                continue;
            }

            Object instance = plugin.getPlugin();

            plugins.put(description.getId(), plugin);
            pluginInstances.put(instance, plugin);

            // Register main class as listener
            eventManager.registerListeners(instance, instance);
        }
    }

    protected Collection<PluginDescription> sortDescriptions(Deque<PluginDescription> unsorted) {
        DirectedAcyclicGraph<PluginDescription> graph = new DirectedAcyclicGraph<>();

        for (PluginDescription description : unsorted) {
            graph.add(description);

            for (PluginDependency dependency : description.getDependencies()) {
                Optional<PluginDescription> in = unsorted.stream().filter(desc -> desc.getId().equals(dependency.getId())).findFirst();
                in.ifPresent(desc -> graph.addEdges(description, desc));
            }
        }

        Collection<PluginDescription> sorted;
        try {
            sorted = graph.sort();
        } catch (GraphException e) {
            throw new IllegalStateException("Circular dependency found", e);
        }

        return sorted;
    }
}

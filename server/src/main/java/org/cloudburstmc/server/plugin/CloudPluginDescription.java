package org.cloudburstmc.server.plugin;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.plugin.PluginDependency;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.api.plugin.PluginLoader;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CloudPluginDescription implements PluginDescription {
    private final String id;
    private final String name;
    private final String version;
    private final List<String> authors;
    private final String description;
    private final List<PluginDependency> dependencies;
    private final String url;
    private final Path path;
    private final PluginLoader loader;

    public CloudPluginDescription(String id, String name, String version, Collection<String> authors, String description,
                                  Collection<PluginDependency> dependencies, String url, Path path, PluginLoader loader) {
        this.id = Objects.requireNonNull(id, "Missing id");
        this.name = Strings.isNullOrEmpty(name) ? id : name;
        this.version = Objects.requireNonNull(version, "Missing version");
        this.authors = ImmutableList.copyOf(authors);
        this.description = description;
        this.dependencies = ImmutableList.copyOf(dependencies);
        this.url = url;
        this.path = path;
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public CloudPluginDescription(@NonNull PluginDescription description) {
        this.id = Objects.requireNonNull(description.getId(), "id");
        this.name = Objects.requireNonNull(description.getName(), "name");
        this.version = Objects.requireNonNull(description.getVersion(), "version");
        this.authors = ImmutableList.copyOf(description.getAuthors());
        this.description = description.getDescription().orElse(null);
        this.dependencies = ImmutableList.copyOf(description.getDependencies());
        this.url = description.getUrl().orElse(null);
        this.path = description.getPath().orElse(null);
        this.loader = Objects.requireNonNull(description.getPluginLoader(), "loader");
    }

    @NonNull
    @Override
    public String getId() {
        return id;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @NonNull
    @Override
    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    @NonNull
    @Override
    public List<String> getAuthors() {
        return authors;
    }

    @NonNull
    @Override
    public String getVersion() {
        return version;
    }

    @NonNull
    @Override
    public List<PluginDependency> getDependencies() {
        return dependencies;
    }

    @NonNull
    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @NonNull
    @Override
    public PluginLoader getPluginLoader() {
        return loader;
    }
}

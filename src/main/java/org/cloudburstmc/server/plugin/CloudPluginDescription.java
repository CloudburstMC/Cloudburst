package org.cloudburstmc.server.plugin;

import com.google.common.base.Strings;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.*;

public class CloudPluginDescription implements PluginDescription {
    private final String id;
    private final String name;
    private final String version;
    private final List<String> authors = new ArrayList<>();
    private final String description;
    private final List<PluginDependency> dependencies = new ArrayList<>();
    private final String url;
    private final Path path;
    private final PluginLoader loader;

    public CloudPluginDescription(String id, String name, String version, Collection<String> authors, String description,
                                  Collection<PluginDependency> dependencies, String url, Path path, PluginLoader loader) {
        this.id = Objects.requireNonNull(id, "Missing id");
        this.name = Strings.isNullOrEmpty(name) ? id : name;
        this.version = Objects.requireNonNull(version, "Missing version");
        this.authors.addAll(authors);
        this.description = description;
        this.dependencies.addAll(dependencies);
        this.url = url;
        this.path = path;
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public CloudPluginDescription(@Nonnull PluginDescription description) {
        this.id = Objects.requireNonNull(description.getId(), "id");
        this.name = Objects.requireNonNull(description.getName(), "name");
        this.version = Objects.requireNonNull(description.getVersion(), "version");
        this.authors.addAll(description.getAuthors());
        this.description = description.getDescription().orElse(null);
        this.dependencies.addAll(description.getDependencies());
        this.url = description.getUrl().orElse(null);
        this.path = description.getPath().orElse(null);
        this.loader = Objects.requireNonNull(description.getPluginLoader(), "loader");
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Nonnull
    @Override
    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    @Nonnull
    @Override
    public Collection<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    @Nonnull
    @Override
    public String getVersion() {
        return version;
    }

    @Nonnull
    @Override
    public Collection<PluginDependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    @Nonnull
    @Override
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    @Nonnull
    @Override
    public PluginLoader getPluginLoader() {
        return loader;
    }
}

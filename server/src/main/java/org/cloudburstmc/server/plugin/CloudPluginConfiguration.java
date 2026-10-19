package org.cloudburstmc.server.plugin;

import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.api.configuration.ConfigurationLoader;
import org.cloudburstmc.api.plugin.PluginConfiguration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Owns a plugin's configuration and bundled defaults.
 */
public class CloudPluginConfiguration implements PluginConfiguration {

    private final ConfigurationLoader loader;
    private final Class<?> pluginClass;
    private final Path path;
    private Configuration configuration;

    public CloudPluginConfiguration(ConfigurationLoader loader, Class<?> pluginClass, Path directory) {
        this.loader = loader;
        this.pluginClass = pluginClass;
        this.path = directory.resolve("config.yml");
    }

    @Override
    public InputStream getResource(String resource) throws IOException {
        String normalized = resource.replace('\\', '/');
        Path relative = Path.of(normalized);

        if (normalized.isBlank() || relative.isAbsolute() || relative.normalize().startsWith("..")) {
            throw new IllegalArgumentException("Resource path must stay inside the plugin data directory");
        }

        return pluginClass.getResourceAsStream("/" + normalized);
    }

    @Override
    public void saveResource(String resource, boolean replace) throws IOException {
        Path directory = path.toAbsolutePath().getParent();
        Path destination = directory.resolve(resource.replace('\\', '/')).normalize();

        if (!destination.startsWith(directory)) {
            throw new IllegalArgumentException("Resource path escapes the plugin data directory");
        }

        try (InputStream stream = getResource(resource)) {
            if (stream == null) {
                throw new FileNotFoundException("Plugin does not bundle " + resource);
            }

            if (!replace && Files.exists(destination)) {
                return;
            }

            Files.createDirectories(destination.getParent());

            Path temporary = Files.createTempFile(destination.getParent(), "resource-", ".tmp");
            try {
                Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING);

                if (replace) {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(temporary, destination);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    @Override
    public Configuration getConfig() throws IOException {
        if (configuration == null) {
            reloadConfig();
        }

        return configuration;
    }

    @Override
    public void reloadConfig() throws IOException {
        Configuration replacement = loader.load(path);

        try (InputStream stream = pluginClass.getResourceAsStream("/config.yml")) {
            if (stream != null) {
                Configuration defaults = loader.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
                replacement.setDefaults(defaults);
            }
        }

        configuration = replacement;
    }

    @Override
    public void saveConfig() throws IOException {
        loader.save(getConfig(), path);
    }

    @Override
    public void saveDefaultConfig() throws IOException {
        saveResource("config.yml", false);
    }
}

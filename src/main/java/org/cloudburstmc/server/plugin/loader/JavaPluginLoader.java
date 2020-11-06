package org.cloudburstmc.server.plugin.loader;

import com.google.inject.Injector;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.plugin.*;
import lombok.val;
import org.cloudburstmc.server.inject.PluginModule;
import org.cloudburstmc.server.plugin.CloudPluginContainer;
import org.cloudburstmc.server.plugin.CloudPluginDependency;
import org.cloudburstmc.server.plugin.loader.java.JavaPluginClassLoader;
import org.cloudburstmc.server.plugin.loader.java.JavaPluginDescription;
import org.cloudburstmc.server.plugin.loader.java.PluginClassVisitor;
import org.cloudburstmc.server.plugin.loader.java.PluginInformation;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JavaPluginLoader implements PluginLoader {
    private static final PathMatcher PATH_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.jar");
    private final Map<Class<?>, Object> dependencies = new HashMap<>();

    private JavaPluginLoader(Map<Class<?>, Object> dependencies) {
        this.dependencies.putAll(dependencies);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nonnull
    @Override
    public PluginDescription loadPlugin(@Nonnull Path path) throws Exception {
        Objects.requireNonNull(path, "path");

        try (JarInputStream jis = new JarInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                    continue;
                }

                Optional<PluginInformation> optionalInfo = getInformation(jis);

                if (optionalInfo.isPresent()) {
                    PluginInformation information = optionalInfo.get();

                    List<PluginDependency> dependencies = new ArrayList<>();
                    for (Dependency dependency : information.getDependencies()) {
                        dependencies.add(new CloudPluginDependency(dependency.id(), dependency.version(), dependency.optional()));
                    }

                    try {
                        return new JavaPluginDescription(information.getId(), information.getName(), information.getVersion(),
                                information.getAuthors(), information.getDescription(), dependencies, information.getUrl(),
                                path, this, information.getClassName());
                    } catch (NullPointerException e) {
                        throw new IllegalArgumentException("Plugin does not contain the correct information", e);
                    }
                }
            }
        }
        throw new PluginException("No main class found");
    }

    @Nonnull
    @Override
    public PluginContainer createPlugin(@Nonnull Injector injector, @Nonnull PluginDescription description) throws Exception {
        Objects.requireNonNull(description, "description");
        if (!(description instanceof JavaPluginDescription)) {
            throw new IllegalArgumentException("Description provided is not of JavaPluginDescription");
        }

        Path path = description.getPath().orElseThrow(() -> new IllegalArgumentException("No path in plugin description"));
        Path dataDirectory = path.getParent().resolve(description.getId());
        Logger logger = LoggerFactory.getLogger(description.getId());

        val pluginClass = getPluginClass(path, (JavaPluginDescription) description);
        Injector inj = injector.createChildInjector(new PluginModule(description, logger, dataDirectory, pluginClass));

        return new CloudPluginContainer(inj.getInstance(pluginClass), description, logger, dataDirectory);
    }

    @Nonnull
    @Override
    public PathMatcher getPathMatcher() {
        return PATH_MATCHER;
    }

    private Class<?> getPluginClass(Path path, JavaPluginDescription description) throws MalformedURLException, ClassNotFoundException {
        JavaPluginClassLoader loader = new JavaPluginClassLoader(new URL[]{path.toUri().toURL()});
        String className = description.getClassName().replace('/', '.');

        return loader.loadClass(className);
    }

    private Optional<PluginInformation> getInformation(JarInputStream jis) throws IOException {
        ClassReader reader = new ClassReader(jis);
        PluginClassVisitor visitor = new PluginClassVisitor();

        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return visitor.getInformation();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Builder {
        private final Map<Class<?>, Object> dependencies = new HashMap<>();

        public Builder registerDependency(Class<?> clazz, Object instance) {
            Objects.requireNonNull(clazz, "clazz");
            Objects.requireNonNull(instance, "instance");
            if (dependencies.containsKey(clazz)) {
                throw new IllegalArgumentException("Dependency is already registered");
            }

            dependencies.put(clazz, instance);
            return this;
        }

        public JavaPluginLoader build() {
            return new JavaPluginLoader(dependencies);
        }
    }
}

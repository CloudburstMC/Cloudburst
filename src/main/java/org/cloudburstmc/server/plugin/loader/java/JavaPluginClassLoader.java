package org.cloudburstmc.server.plugin.loader.java;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class JavaPluginClassLoader extends URLClassLoader {
    private static final Set<JavaPluginClassLoader> loaders = new CopyOnWriteArraySet<>();

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public JavaPluginClassLoader(URL[] urls) {
        super(urls);
        loaders.add(this);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true);
    }

    private Class<?> loadClass0(String name, boolean resolve, boolean checkOther) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException ignored) {
            // Ignored: we'll try others
        }
        if (checkOther) {
            for (JavaPluginClassLoader loader : loaders) {
                if (loader != this) {
                    try {
                        return loader.loadClass0(name, resolve, false);
                    } catch (ClassNotFoundException ignored) {
                        // We're trying others, safe to ignore
                    }
                }
            }
        }
        throw new ClassNotFoundException(name);
    }
}

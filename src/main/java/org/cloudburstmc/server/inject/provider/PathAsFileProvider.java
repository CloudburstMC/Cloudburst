package org.cloudburstmc.server.inject.provider;

import com.google.inject.Provider;

import java.io.File;
import java.nio.file.Path;

public abstract class PathAsFileProvider implements Provider<File> {

    protected Provider<Path> path;

    @Override
    public File get() {
        return this.path.get().toFile();
    }
}

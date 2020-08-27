package org.cloudburstmc.server.inject.provider.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.utils.DataDir;

import java.lang.annotation.Annotation;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataDirProvider implements DataDir {

    public static final DataDir FILE = new DataDirProvider(Type.FILE);
    public static final DataDir DATA = new DataDirProvider(Type.FILE);
    public static final DataDir PLUGIN = new DataDirProvider(Type.PLUGIN);
    public static final DataDir LEVEL = new DataDirProvider(Type.LEVEL);

    private final Type type;

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return DataDir.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataDirProvider that = (DataDirProvider) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return (127 * "type".hashCode()) ^ type.hashCode();
    }
}

package org.cloudburstmc.server.level.provider;

import java.io.IOException;
import java.nio.file.Path;

public interface LevelProviderFactory {

    /**
     * Opens the provider for a level, importing another compatible format when required.
     *
     * @param context provider creation context
     * @return opened provider
     * @throws IOException if the provider cannot be opened or imported
     */
    LevelProvider create(CloudLevelProviderContext context) throws IOException;

    /**
     * Checks whether this factory can open or import the stored level.
     *
     * @param levelsPath root levels directory
     * @param levelId    level ID
     * @return {@code true} when the stored level is compatible
     */
    boolean isCompatible(String levelId, Path levelsPath);
}

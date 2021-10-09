package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.CloudServer;

import java.io.InputStream;

@UtilityClass
public class RegistryUtils {

    public static InputStream getOrAssertResource(String resourcePath) {
        InputStream stream = CloudServer.class.getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new AssertionError("Unable to locate " + resourcePath);
        }
        return stream;
    }
}

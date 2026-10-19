package org.cloudburstmc.server.configuration.codec;

import org.cloudburstmc.api.configuration.ConfigurationCodec;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Stores level identity and coordinates without loading levels during decoding.
 */
public class LocationConfigurationCodec implements ConfigurationCodec<Location> {
    private final Function<String, ? extends Level> resolver;

    public LocationConfigurationCodec(Function<String, ? extends Level> resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public Map<String, Object> encode(Location value) {
        return Map.of("level", value.getLevel().getId(), "x", finite(value.getX()),
                "y", finite(value.getY()), "z", finite(value.getZ()),
                "yaw", finite(value.getYaw()), "pitch", finite(value.getPitch()));
    }

    @Override
    public Location decode(Map<String, Object> values) {
        if (!(values.get("level") instanceof String id)) {
            throw new IllegalArgumentException("Location requires a level ID");
        }

        Level level = resolver.apply(id);
        if (level == null) {
            throw new IllegalArgumentException("Location level is unavailable: " + id);
        }

        return Location.from(number(values, "x"), number(values, "y"), number(values, "z"),
                number(values, "yaw"), number(values, "pitch"), level);
    }

    private static float number(Map<String, Object> values, String key) {
        return ConfigurationNumbers.floatValue(values.get(key), key);
    }

    private static float finite(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Location coordinates and rotation must be finite");
        }

        return value;
    }
}

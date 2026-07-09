package org.cloudburstmc.api.util;

import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Value
public class PlayerDataKey {
    @Nullable
    UUID uniqueId;
    @Nullable
    String name;

    private PlayerDataKey(@Nullable UUID uniqueId, @Nullable String name) {
        if (uniqueId == null && (name == null || name.isBlank())) {
            throw new IllegalArgumentException("Unique id and name cannot both be empty");
        }
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public static PlayerDataKey of(UUID uniqueId) {
        return new PlayerDataKey(Objects.requireNonNull(uniqueId, "uniqueId"), null);
    }

    public static PlayerDataKey of(UUID uniqueId, @Nullable String name) {
        return new PlayerDataKey(Objects.requireNonNull(uniqueId, "uniqueId"), name);
    }

    public static PlayerDataKey named(String name) {
        return new PlayerDataKey(null, Objects.requireNonNull(name, "name"));
    }

    public String getStorageId() {
        if (this.uniqueId != null) {
            return this.uniqueId.toString();
        }
        return Objects.requireNonNull(this.name, "name").toLowerCase(Locale.ROOT);
    }
}

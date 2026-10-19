package org.cloudburstmc.api.ban;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable snapshot of a ban. Replace it through its ban list to change it.
 *
 * @param target the player UUID or IP address
 * @param created when the ban was created
 * @param source the operator or plugin that issued the ban
 * @param expiration when the ban expires, or null for a permanent ban
 * @param reason the reason for the ban
 * @param <T> the target type
 */
public record BanEntry<T>(T target, Instant created, String source, @Nullable Instant expiration, String reason) {
    public BanEntry {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns whether the expiration has been reached. Permanent bans never expire.
     */
    public boolean isExpired() {
        return expiration != null && !Instant.now().isBefore(expiration);
    }
}

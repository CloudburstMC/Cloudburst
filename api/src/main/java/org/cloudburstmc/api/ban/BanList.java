package org.cloudburstmc.api.ban;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Persistent bans for one target type. Modify bans on the server thread.
 * Failed saves leave the active bans unchanged.
 *
 * <p>Methods that modify stored bans may throw
 * {@link java.io.UncheckedIOException} when storage cannot be accessed.</p>
 *
 * @param <T> the target type
 */
public interface BanList<T> {
    /**
     * Returns the active ban for a target, or null when absent or expired.
     */
    @Nullable
    BanEntry<T> getBanEntry(T target);

    /**
     * Returns whether the target has an unexpired ban.
     */
    default boolean isBanned(T target) {
        return getBanEntry(target) != null;
    }

    /**
     * Returns an immutable snapshot of all unexpired bans.
     */
    Set<BanEntry<T>> getBanEntries();

    /**
     * Adds or replaces a ban. Null reason or source selects the server's default text.
     * Null expiration makes the ban permanent. This does not disconnect online players.
     *
     * @param target the player UUID or IP address to ban
     * @param reason the reason, or null for the default text
     * @param expiration the expiration time, or null for a permanent ban
     * @param source the issuer, or null for the default text
     * @return the saved entry
     */
    BanEntry<T> addBan(T target, @Nullable String reason, @Nullable Instant expiration, @Nullable String source);

    /**
     * Adds a ban lasting for the supplied positive duration.
     * Like {@link #addBan}, this does not disconnect online players.
     *
     * @param target the player UUID or IP address to ban
     * @param reason the reason, or null for the default text
     * @param duration how long the ban remains active, starting now
     * @param source the issuer, or null for the default text
     * @return the saved entry
     * @throws IllegalArgumentException if the duration is zero or negative
     */
    default BanEntry<T> addBanFor(T target, @Nullable String reason, Duration duration, @Nullable String source) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Ban duration must be positive");
        }

        return addBan(target, reason, Instant.now().plus(duration), source);
    }

    /**
     * Removes a ban. An absent target has no effect.
     */
    void pardon(T target);
}

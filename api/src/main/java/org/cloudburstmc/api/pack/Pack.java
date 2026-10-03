package org.cloudburstmc.api.pack;

import org.cloudburstmc.api.util.SemVersion;

import java.util.UUID;

/**
 * Read-only metadata for a registered pack. The server owns its contents and lifetime.
 */
public interface Pack {

    /**
     * Returns the UUID declared in the pack header.
     *
     * @return the pack UUID
     */
    UUID getId();

    /**
     * Returns the display name declared in the pack header.
     *
     * @return the pack name
     */
    String getName();

    /**
     * Returns the version declared in the pack header.
     *
     * @return the pack version
     */
    SemVersion getVersion();

    /**
     * Returns the content category of the pack.
     *
     * @return the pack type
     */
    PackType getType();

    /**
     * Returns the size of the archive sent to clients.
     *
     * @return the archive size in bytes
     */
    long getSize();

    /**
     * Returns the SHA-256 digest of the archive sent to clients.
     * Changes to the returned array do not affect the pack.
     *
     * @return a copy of the archive digest
     */
    byte[] getHash();
}

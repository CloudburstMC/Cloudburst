package org.cloudburstmc.api.event.server;

import com.google.common.base.Preconditions;
import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.api.util.PlayerDataSerializer;

/**
 * Called when the serializer for a player's stored data is selected.
 */
public final class PlayerDataSerializeEvent extends ServerEvent {

    private final PlayerDataKey key;
    private PlayerDataSerializer serializer;

    /**
     * Creates a player-data serialization event.
     *
     * @param key the player data key
     * @param serializer the serializer that will be used
     */
    public PlayerDataSerializeEvent(PlayerDataKey key, PlayerDataSerializer serializer) {
        this.key = Preconditions.checkNotNull(key, "key");
        this.serializer = Preconditions.checkNotNull(serializer, "serializer");
    }

    /**
     * Returns the key identifying the player data.
     *
     * @return the player data key
     */
    public PlayerDataKey getKey() {
        return this.key;
    }

    /**
     * Returns the serializer that will be used.
     *
     * @return the player data serializer
     */
    public PlayerDataSerializer getSerializer() {
        return this.serializer;
    }

    /**
     * Sets the serializer that will be used.
     *
     * @param serializer the new serializer
     */
    public void setSerializer(PlayerDataSerializer serializer) {
        this.serializer = Preconditions.checkNotNull(serializer, "serializer");
    }
}

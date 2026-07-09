package org.cloudburstmc.api.event.server;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.cloudburstmc.api.util.PlayerDataKey;
import org.cloudburstmc.api.util.PlayerDataSerializer;

public final class PlayerDataSerializeEvent extends ServerEvent {

    @Getter
    private final PlayerDataKey key;
    @Getter
    private PlayerDataSerializer serializer;

    public PlayerDataSerializeEvent(PlayerDataKey key, PlayerDataSerializer serializer) {
        this.key = Preconditions.checkNotNull(key, "key");
        this.serializer = Preconditions.checkNotNull(serializer);
    }

    public void setSerializer(PlayerDataSerializer serializer) {
        this.serializer = Preconditions.checkNotNull(serializer, "serializer");
    }
}

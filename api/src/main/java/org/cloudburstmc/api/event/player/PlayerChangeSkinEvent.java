package org.cloudburstmc.api.event.player;

import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.player.Player;

/**
 * author: KCodeYT
 * Nukkit Project
 */
public class PlayerChangeSkinEvent extends PlayerEvent implements Cancellable {

    private final SerializedSkin skin;

    public PlayerChangeSkinEvent(Player player, SerializedSkin skin) {
        super(player);
        this.skin = skin;
    }

    public SerializedSkin getSkin() {
        return this.skin;
    }

}

package org.cloudburstmc.server.event.player;

import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.server.event.Cancellable;
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

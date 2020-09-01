package org.cloudburstmc.server.metadata;

import org.cloudburstmc.server.player.IPlayer;

import javax.inject.Singleton;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Singleton
public class PlayerMetadataStore extends MetadataStore {

    @Override
    protected String disambiguate(Metadatable player, String metadataKey) {
        if (!(player instanceof IPlayer)) {
            throw new IllegalArgumentException("Argument must be an IPlayer instance");
        }
        return (((IPlayer) player).getName() + ":" + metadataKey).toLowerCase();
    }
}

package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.player.Player;

public interface PlayerContainerView extends EntityContainerView {

    @Override
    Player getHolder();
}

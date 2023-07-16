package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.container.view.PlayerContainerView;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

public class CloudPlayerContainerView extends CloudContainerView implements PlayerContainerView {

    protected final CloudPlayer holder;

    protected CloudPlayerContainerView(ContainerViewType<?> type, CloudPlayer holder, CloudContainer container) {
        super(type, container);
        this.holder = holder;
    }

    protected CloudPlayerContainerView(ContainerViewType<?> type, CloudPlayer holder, CloudContainer container, int size, int offset) {
        super(type, container, size, offset);
        this.holder = holder;
    }

    @Override
    public CloudPlayer getHolder() {
        return holder;
    }
}

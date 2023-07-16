package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.container.view.EntityContainerView;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.CloudEntity;

public class CloudEntityContainerView extends CloudContainerView implements EntityContainerView {

    protected final CloudEntity holder;

    protected CloudEntityContainerView(ContainerViewType<?> type, CloudEntity holder, CloudContainer container) {
        super(type, container);
        this.holder = holder;
    }

    protected CloudEntityContainerView(ContainerViewType<?> type, CloudEntity holder, CloudContainer container, int size, int offset) {
        super(type, container, size, offset);
        this.holder = holder;
    }

    @Override
    public CloudEntity getHolder() {
        return holder;
    }
}

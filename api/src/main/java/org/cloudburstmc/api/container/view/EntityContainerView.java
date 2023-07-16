package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.entity.Entity;

public interface EntityContainerView extends ContainerView {

    Entity getHolder();
}

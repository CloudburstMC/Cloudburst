package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudCreatedOutputView extends CloudContainerView {
    public CloudCreatedOutputView() {
        super(ContainerViewTypes.CREATED_OUTPUT, new CloudContainer(1));
    }
}

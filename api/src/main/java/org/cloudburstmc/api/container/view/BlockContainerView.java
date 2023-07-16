package org.cloudburstmc.api.container.view;

import org.cloudburstmc.api.block.Block;

public interface BlockContainerView extends ContainerView {

    Block getBlock();
}

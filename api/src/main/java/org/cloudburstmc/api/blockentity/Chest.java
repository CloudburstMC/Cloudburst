package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.container.view.BlockContainerView;

public interface Chest extends BlockEntity, BlockContainerView {

    boolean isFindable();

    void setFindable(boolean findable);

    boolean isPaired();

    Chest getPair();

    boolean pairWith(Chest chest);

    boolean unpair();
}

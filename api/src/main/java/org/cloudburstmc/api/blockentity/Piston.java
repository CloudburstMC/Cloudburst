package org.cloudburstmc.api.blockentity;

public interface Piston extends BlockEntity {

    boolean isSticky();

    void setSticky(boolean sticky);

    boolean isPowered();

    void setPowered(boolean powered);
}

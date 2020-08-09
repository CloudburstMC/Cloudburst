package org.cloudburstmc.server.blockentity;

public interface Piston extends BlockEntity {

    boolean isSticky();

    void setSticky(boolean sticky);

    boolean isPowered();

    void setPowered(boolean powered);
}

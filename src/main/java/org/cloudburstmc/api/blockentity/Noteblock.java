package org.cloudburstmc.api.blockentity;

public interface Noteblock extends BlockEntity {

    byte getNote();

    void setNote(int note);

    void changeNote();

    boolean isPowered();

    void setPowered(boolean powered);
}

package org.cloudburstmc.api.blockentity;

public interface Sign extends BlockEntity {

    String getTextOwner();

    void setTextOwner(String textOwner);

    String[] getText();

    void setText(String... lines);
}

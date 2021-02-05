package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.util.data.DyeColor;

public interface Bed extends BlockEntity {

    DyeColor getColor();

    void setColor(DyeColor color);
}

package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;

public interface LightningBolt extends Entity {

    boolean isEffect();

    void setEffect(boolean effect);
}

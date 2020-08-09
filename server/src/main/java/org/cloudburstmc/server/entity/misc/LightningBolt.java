package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.server.entity.Entity;

public interface LightningBolt extends Entity {

    boolean isEffect();

    void setEffect(boolean effect);
}

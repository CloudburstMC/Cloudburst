package org.cloudburstmc.server.level;

import lombok.extern.log4j.Log4j2;

@Log4j2

public abstract class ExplosiveA {
    public abstract boolean shouldCauseDamageInWater();
}

package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.level.Location;

public class EntityXpOrb extends EntityExperienceOrb {

    public EntityXpOrb(EntityType<ExperienceOrb> type, Location location) {
        super(type, location);
    }
}

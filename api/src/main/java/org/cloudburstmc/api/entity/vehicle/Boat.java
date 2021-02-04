package org.cloudburstmc.api.entity.vehicle;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.server.utils.data.TreeSpecies;

public interface Boat extends Entity {

    void setWoodType(TreeSpecies woodType);
}

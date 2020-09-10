package org.cloudburstmc.server.entity.vehicle;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.utils.data.TreeSpecies;

public interface Boat extends Entity {

    void setWoodType(TreeSpecies woodType);
}

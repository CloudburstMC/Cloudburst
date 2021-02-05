package org.cloudburstmc.api.entity.vehicle;

import org.cloudburstmc.api.util.data.TreeSpecies;

public interface Boat extends Vehicle {

    void setWoodType(TreeSpecies woodType);
}

package org.cloudburstmc.api.entity.passive;

import org.cloudburstmc.api.entity.Breedable;
import org.cloudburstmc.api.entity.Shearable;
import org.cloudburstmc.api.util.data.DyeColor;

/**
 * A dyeable animal that produces wool when sheared.
 */
public interface Sheep extends Breedable, Shearable {

    /**
     * Returns the color of this sheep's wool.
     */
    DyeColor getColor();

    /**
     * Changes the wool color, including the color when wool regrows.
     */
    void setColor(DyeColor color);

    /**
     * Returns whether this sheep is missing its wool.
     */
    boolean isSheared();

    /**
     * Changes whether the sheep has wool without performing a shearing interaction.
     */
    void setSheared(boolean sheared);
}

package org.cloudburstmc.api.entity.hostile;

import org.cloudburstmc.api.entity.Mob;

/**
 * The monster branch of {@link Enemy}, excluding hostile mobs such as {@link Ghast}.
 */
public interface Monster extends Mob, Enemy {
}

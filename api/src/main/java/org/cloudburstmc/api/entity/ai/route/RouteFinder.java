package org.cloudburstmc.api.entity.ai.route;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.List;

/**
 * Finds a route from the entity's current position to a target position.
 *
 * @author daoge_cmd
 */
public interface RouteFinder {

    /**
     * Search for a route from the entity's position to the target.
     *
     * @param entity the entity
     * @param target the target location
     * @return the list of route nodes (empty if no path found)
     */
    List<Node> search(EntityIntelligent entity, Vector3f target);
}

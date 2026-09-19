package org.cloudburstmc.api.entity.ai.behaviorgroup;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.behavior.Behavior;
import org.cloudburstmc.api.entity.ai.controller.Controller;
import org.cloudburstmc.api.entity.ai.memory.MemoryStorage;
import org.cloudburstmc.api.entity.ai.route.RouteFinder;
import org.cloudburstmc.api.entity.ai.sensor.Sensor;

import java.util.Set;

/**
 * @author daoge_cmd
 */
public interface BehaviorGroup {

    /**
     * Get all core behaviors.
     *
     * @return the set of core behaviors
     */
    Set<Behavior> getCoreBehaviors();

    /**
     * Get all non-core behaviors.
     *
     * @return the set of behaviors
     */
    Set<Behavior> getBehaviors();

    /**
     * Get all sensors.
     *
     * @return the set of sensors
     */
    Set<Sensor> getSensors();

    /**
     * Get all controllers.
     *
     * @return the set of controllers
     */
    Set<Controller> getControllers();

    /**
     * Get the route finder.
     *
     * @return the route finder, or {@code null} if none
     */
    RouteFinder getRouteFinder();

    /**
     * Get the memory storage.
     *
     * @return the memory storage
     */
    MemoryStorage getMemoryStorage();

    /**
     * Check if the route needs to be updated on the next tick.
     *
     * @return {@code true} if a route update is needed
     */
    boolean isRouteUpdateRequired();

    /**
     * Set whether the route needs to be updated on the next tick.
     *
     * @param routeUpdateRequired whether a route update is needed
     */
    void setRouteUpdateRequired(boolean routeUpdateRequired);

    /**
     * @param action the action to queue
     */
    void addSyncedAction(Runnable action);

    void tick();

    void setEntity(EntityIntelligent entity);

    void processSyncedActions();
}

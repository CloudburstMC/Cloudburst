package org.cloudburstmc.server.entity.ai.controller;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.api.entity.ai.controller.Controller;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.utils.Utils;

public class LookController implements Controller {

    protected final boolean lookAtTarget;
    protected final boolean lookAtRoute;

    public LookController(boolean lookAtTarget, boolean lookAtRoute) {
        this.lookAtTarget = lookAtTarget;
        this.lookAtRoute = lookAtRoute;
    }

    @Override
    public boolean control(EntityIntelligent entity) {
        Location location = entity.getLocation();
        Vector3f lookTarget = entity.getLookTarget();
        double bodyYaw = location.getYaw();
        if (lookAtRoute && entity.hasMoveDirection()) {
            var end = entity.getMoveDirectionEnd();
            if (end != null) {
                var routeDirection = Vector3d.from(end.getX() - location.getX(), end.getY() - location.getY(), end.getZ() - location.getZ());
                bodyYaw = Utils.getYawFromVector(routeDirection);
                if (!lookAtTarget) {
                    entity.setHeadYaw(bodyYaw);
                }
            }
        }
        if (lookAtTarget && lookTarget != null) {
            var toTarget = Vector3d.from(lookTarget.getX() - location.getX(), lookTarget.getY() - (location.getY() + entity.getEyeHeight()), lookTarget.getZ() - location.getZ());
            entity.setHeadYaw(Utils.getYawFromVector(toTarget));
        }
        entity.setRotation((float) bodyYaw, location.getPitch());
        return true;
    }
}

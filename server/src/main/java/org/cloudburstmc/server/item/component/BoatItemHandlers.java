package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityPlaceEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntitySnapshot;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.entity.vehicle.VanillaBoats;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class BoatItemHandlers {

    public static UseHandler use(VanillaBoats.Definition definition, boolean chest) {
        return (item, entity) -> place(item, entity, definition, chest);
    }

    public static UseOnHandler useOn(VanillaBoats.Definition definition, boolean chest) {
        return (item, entity, position, face, click) -> place(item, entity, definition, chest);
    }

    private static ItemStack place(ItemStack item, Entity user, VanillaBoats.Definition definition, boolean chest) {
        if (user instanceof Player player && player.isSpectator()) {
            return item;
        }

        CloudLevel level = (CloudLevel) user.getLevel();
        Vector3f eye = user.getPosition().add(0, user.getEyeHeight(), 0);
        Vector3f end = eye.add(user.getDirectionVector().mul(5));
        if (!(level.rayTraceBlocks(new RayTraceContext(eye, end, BlockShapeMode.OUTLINE,
                FluidCollisionMode.ANY, CollisionContext.of(user))) instanceof BlockHitResult hit)) {
            return item;
        }

        if (hit.liquid()) {
            Vector3i position = hit.block().getPosition();
            BoxIntersection intersection = new BoundingBox(position.getX(), position.getY(), position.getZ(),
                    position.getX() + 1, position.getY() + 1, position.getZ() + 1).intersectSegment(eye, end);
            if (intersection == null) {
                return item;
            }

            hit = new BlockHitResult(intersection.position(), hit.block(), intersection.face(), true);
        }

        for (Entity nearby : level.getNearbyEntities(user.getBoundingBox().expandTowards(end.sub(eye)).inflate(1, 1, 1))) {
            if (nearby != user && nearby.canBeCollidedWith(user) && nearby.getBoundingBox().inflate(0.1f, 0.1f, 0.1f).contains(eye)) {
                return item;
            }
        }

        EntityBoat boat = (EntityBoat) CloudEntitySnapshot.createFromItem(item,
                chest ? EntityTypes.CHEST_BOAT : EntityTypes.BOAT,
                Location.from(hit.position(), user.getYaw(), 0, level));
        boat.setBoatType(definition.type());
        if (hit.liquid()) {
            boat.setPosition(hit.position().sub(0, boat.getBaseOffset(), 0));
        }

        if (level.hasCollision(boat)) {
            boat.close();
            return item;
        }

        EntityPlaceEvent event = new EntityPlaceEvent(boat, user instanceof Player player ? player : null,
                hit.block(), hit.face() == null ? Direction.UP : hit.face());
        user.getServer().getEventManager().fire(event);
        if (event.isCancelled() || boat.isClosed() || boat.getLevel().hasCollision(boat) || !boat.spawn()) {
            boat.close();
            return item;
        }

        boat.spawnToAll();
        return user instanceof Player player && player.isCreative() ? item : item.decreaseCount();
    }
}

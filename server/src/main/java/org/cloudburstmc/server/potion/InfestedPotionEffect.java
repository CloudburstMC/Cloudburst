package org.cloudburstmc.server.potion;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.Silverfish;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.random.RandomGenerator;

@UtilityClass
public class InfestedPotionEffect {

    private static final float SPAWN_CHANCE = 0.1f;

    public static void onHurt(CloudLivingEntity entity) {
        CloudLevel level = entity.getLevel();
        if (level == null || entity.isClosed()) {
            return;
        }

        RandomGenerator random = level.getRandom();
        if (random.nextFloat() >= SPAWN_CHANCE) {
            return;
        }

        Vector3f position = entity.getPosition().add(0, entity.getHeight() * 0.5f, 0);
        Vector3f launch = entity.getDirectionVector().mul(0.3f, 0.45f, 0.3f);
        int count = random.nextInt(1, 3);
        for (int i = 0; i < count && !entity.isClosed() && entity.getLevel() == level; i++) {
            Location location = Location.from(position, random.nextFloat() * 360f, 0f, level);
            Silverfish silverfish = CloudEntityRegistry.get().newEntity(EntityTypes.SILVERFISH, location);
            CloudEntity spawned = (CloudEntity) silverfish;
            spawned.setMotion(rotateLaunch(launch, random.nextDouble(-Math.PI / 2, Math.PI / 2)));
            if (spawned.spawn(new CreatureSpawnEvent(silverfish, CreatureSpawnReason.POTION_EFFECT))) {
                level.playSound(position, SoundTypes.MOB_SILVERFISH_HIT);
            }
        }
    }

    private static Vector3f rotateLaunch(Vector3f launch, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return Vector3f.from(launch.getX() * cos + launch.getZ() * sin, launch.getY(), launch.getZ() * cos - launch.getX() * sin);
    }
}

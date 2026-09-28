package org.cloudburstmc.server.entity;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Pose;

import java.util.Map;
import java.util.function.Predicate;

@UtilityClass
public class HumanPoses {
    private static final Map<Pose, EntityDimensions> DIMENSIONS = Map.of(
            Pose.STANDING, new EntityDimensions(0.6f, 1.8f, 1.62f),
            Pose.CROUCHING, new EntityDimensions(0.6f, 1.5f, 1.27f),
            Pose.SWIMMING, new EntityDimensions(0.6f, 0.6f, 0.4f),
            Pose.CRAWLING, new EntityDimensions(0.6f, 0.625f, 0.4f),
            Pose.FALL_FLYING, new EntityDimensions(0.6f, 0.6f, 0.4f),
            Pose.SPIN_ATTACK, new EntityDimensions(0.6f, 0.6f, 0.4f),
            Pose.SLEEPING, new EntityDimensions(0.2f, 0.2f, 0.2f)
    );

    public static EntityDimensions dimensions(Pose pose) {
        return DIMENSIONS.get(pose);
    }

    public static Pose resolve(Pose current, Pose desired, boolean unrestricted, Predicate<Pose> fits) {
        if (unrestricted) {
            return desired;
        }

        if (!fits.test(Pose.SWIMMING)) {
            return current;
        }

        if (fits.test(desired)) {
            return desired;
        }

        if (fits.test(Pose.CROUCHING)) {
            return Pose.CROUCHING;
        }

        return fits.test(Pose.CRAWLING) ? Pose.CRAWLING : Pose.SWIMMING;
    }
}

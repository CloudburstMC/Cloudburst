package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.misc.AreaEffectCloud;
import org.cloudburstmc.api.entity.projectile.LingeringPotion;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Called before a lingering potion creates an area effect cloud.
 */
public class LingeringPotionSplashEvent extends EntityEvent implements Cancellable {
    private final AreaEffectCloud areaEffectCloud;

    /**
     * Creates a lingering potion splash event.
     *
     * @param potion          the thrown potion
     * @param areaEffectCloud the cloud prepared from the potion contents
     */
    public LingeringPotionSplashEvent(LingeringPotion potion, AreaEffectCloud areaEffectCloud) {
        this.entity = requireNonNull(potion, "potion");
        this.areaEffectCloud = requireNonNull(areaEffectCloud, "areaEffectCloud");
    }

    @Override
    public LingeringPotion getEntity() {
        return (LingeringPotion) this.entity;
    }

    /**
     * Returns the thrown potion.
     *
     * @return the thrown potion
     */
    public LingeringPotion getPotion() {
        return this.getEntity();
    }

    /**
     * Returns the cloud that will be spawned unless the event is canceled.
     *
     * @return the prepared area effect cloud
     */
    public AreaEffectCloud getAreaEffectCloud() {
        return this.areaEffectCloud;
    }
}

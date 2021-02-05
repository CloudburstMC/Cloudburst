package org.cloudburstmc.api.event.potion;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.potion.Potion;
import org.cloudburstmc.server.entity.projectile.EntitySplashPotion;

/**
 * Created by Snake1999 on 2016/1/12.
 * Package cn.nukkit.event.potion in project nukkit
 */
public class PotionCollideEvent extends PotionEvent implements Cancellable {

    private final EntitySplashPotion thrownPotion;

    public PotionCollideEvent(Potion potion, EntitySplashPotion thrownPotion) {
        super(potion);
        this.thrownPotion = thrownPotion;
    }

    public EntitySplashPotion getThrownPotion() {
        return thrownPotion;
    }
}

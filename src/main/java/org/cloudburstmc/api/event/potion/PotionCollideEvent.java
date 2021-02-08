package org.cloudburstmc.api.event.potion;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.potion.Potion;

/**
 * Created by Snake1999 on 2016/1/12.
 * Package cn.nukkit.event.potion in project nukkit
 */
public class PotionCollideEvent extends PotionEvent implements Cancellable {

    private final Projectile thrownPotion;

    public PotionCollideEvent(Potion potion, Projectile thrownPotion) {
        super(potion);
        this.thrownPotion = thrownPotion;
    }

    public Projectile getThrownPotion() {
        return thrownPotion;
    }
}

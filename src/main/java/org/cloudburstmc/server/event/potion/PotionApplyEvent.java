package org.cloudburstmc.server.event.potion;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.potion.Effect;
import org.cloudburstmc.server.potion.Potion;

/**
 * Created by Snake1999 on 2016/1/12.
 * Package cn.nukkit.event.potion in project nukkit
 */
public class PotionApplyEvent extends PotionEvent implements Cancellable {

    private Effect applyEffect;

    private final Entity entity;

    public PotionApplyEvent(Potion potion, Effect applyEffect, Entity entity) {
        super(potion);
        this.applyEffect = applyEffect;
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public Effect getApplyEffect() {
        return applyEffect;
    }

    public void setApplyEffect(Effect applyEffect) {
        this.applyEffect = applyEffect;
    }
}

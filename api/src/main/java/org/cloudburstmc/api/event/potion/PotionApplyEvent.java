package org.cloudburstmc.api.event.potion;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.potion.CloudEffect;
import org.cloudburstmc.server.potion.Potion;

/**
 * Created by Snake1999 on 2016/1/12.
 * Package cn.nukkit.event.potion in project nukkit
 */
public class PotionApplyEvent extends PotionEvent implements Cancellable {

    private CloudEffect applyEffect;

    private final Entity entity;

    public PotionApplyEvent(Potion potion, CloudEffect applyEffect, Entity entity) {
        super(potion);
        this.applyEffect = applyEffect;
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public CloudEffect getApplyEffect() {
        return applyEffect;
    }

    public void setApplyEffect(CloudEffect applyEffect) {
        this.applyEffect = applyEffect;
    }
}

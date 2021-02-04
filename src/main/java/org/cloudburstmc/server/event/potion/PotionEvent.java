package org.cloudburstmc.server.event.potion;

import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.server.potion.Potion;

/**
 * Created by Snake1999 on 2016/1/12.
 * Package cn.nukkit.event.potion in project nukkit
 */
public abstract class PotionEvent extends Event {

    private Potion potion;

    public PotionEvent(Potion potion) {
        this.potion = potion;
    }

    public Potion getPotion() {
        return potion;
    }

    public void setPotion(Potion potion) {
        this.potion = potion;
    }

}

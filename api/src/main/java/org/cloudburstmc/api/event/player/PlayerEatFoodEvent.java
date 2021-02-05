package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.Food;
import org.cloudburstmc.api.player.Player;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.event.player in project nukkit.
 */
public class PlayerEatFoodEvent extends PlayerEvent implements Cancellable {

    private Food food;

    public PlayerEatFoodEvent(Player player, Food food) {
        super(player);
        this.food = food;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

}

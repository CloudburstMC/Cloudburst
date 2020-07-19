package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.food.Food;
import org.cloudburstmc.server.player.Player;

/**
 * Created by Snake1999 on 2016/1/14.
 * Package cn.nukkit.event.player in project nukkit.
 */
public class PlayerEatFoodEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Food food;

    public static HandlerList getHandlers() {
        return handlers;
    }

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

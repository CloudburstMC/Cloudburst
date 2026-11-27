package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;


/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class PlayerInteractEvent extends PlayerEvent implements Cancellable {

    protected final Block blockTouched;

    protected final Vector3f touchVector;

    protected final Direction direction;

    protected final ItemStack item;

    protected final PlayerInteractAction action;

    public PlayerInteractEvent(Player player, ItemStack item, Block block, Direction face, PlayerInteractAction action) {
        this(player, item, face, action, block, Vector3f.ZERO);
    }

    public PlayerInteractEvent(Player player, ItemStack item, Vector3f touchVector, Direction face, PlayerInteractAction action) {
        this(player, item, face, action, null, touchVector);
    }

    private PlayerInteractEvent(Player player, ItemStack item, Direction face, PlayerInteractAction action, Block block, Vector3f touchVector) {
        super(player);
        this.item = item;
        this.direction = face;
        this.action = action;
        this.blockTouched = block;
        this.touchVector = touchVector;
    }

    public PlayerInteractAction getAction() {
        return action;
    }

    public ItemStack getItem() {
        return item;
    }

    public Block getBlock() {
        return blockTouched;
    }

    public Vector3f getTouchVector() {
        return touchVector;
    }

    public Direction getFace() {
        return direction;
    }
}

package org.cloudburstmc.server.event.player;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerInteractEvent extends PlayerEvent implements Cancellable {

    protected final Block blockTouched;

    protected final Vector3f touchVector;

    protected final Direction direction;

    protected final ItemStack item;

    protected final Action action;

    public PlayerInteractEvent(Player player, ItemStack item, Block block, Direction face, Action action) {
        this(player, item, face, action, block, Vector3f.ZERO);
    }

    public PlayerInteractEvent(Player player, ItemStack item, Vector3f touchVector, Direction face, Action action) {
        this(player, item, face, action, new CloudBlock(player.getLevel(), Vector3i.ZERO, CloudBlock.EMPTY), touchVector);
    }

    private PlayerInteractEvent(Player player, ItemStack item, Direction face, Action action, Block block, Vector3f touchVector) {
        super(player);
        this.item = item;
        this.direction = face;
        this.action = action;
        this.blockTouched = block;
        this.touchVector = touchVector;
    }

    public Action getAction() {
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

    public enum Action {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
        LEFT_CLICK_AIR,
        RIGHT_CLICK_AIR,
        PHYSICAL
    }
}

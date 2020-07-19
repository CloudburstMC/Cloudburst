package org.cloudburstmc.server.event.player;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerInteractEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected final Block blockTouched;

    protected final Vector3f touchVector;

    protected final BlockFace blockFace;

    protected final Item item;

    protected final Action action;

    public PlayerInteractEvent(Player player, Item item, Block block, BlockFace face, Action action) {
        this(player, item, face, action, block, Vector3f.ZERO);
    }

    public PlayerInteractEvent(Player player, Item item, Vector3f touchVector, BlockFace face, Action action) {
        this(player, item, face, action, new CloudBlock(BlockState.AIR, player.getLevel(),
                player.getLevel().getChunk(player.getPosition()), Vector3i.ZERO, 0), touchVector);
    }

    private PlayerInteractEvent(Player player, Item item, BlockFace face, Action action, Block block, Vector3f touchVector) {
        super(player);
        this.item = item;
        this.blockFace = face;
        this.action = action;
        this.blockTouched = block;
        this.touchVector = touchVector;
    }

    public Action getAction() {
        return action;
    }

    public Item getItem() {
        return item;
    }

    public Block getBlock() {
        return blockTouched;
    }

    public Vector3f getTouchVector() {
        return touchVector;
    }

    public BlockFace getFace() {
        return blockFace;
    }

    public enum Action {
        LEFT_CLICK_BLOCK,
        RIGHT_CLICK_BLOCK,
        LEFT_CLICK_AIR,
        RIGHT_CLICK_AIR,
        PHYSICAL
    }
}

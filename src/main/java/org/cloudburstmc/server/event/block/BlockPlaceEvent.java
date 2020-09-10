package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockPlaceEvent extends BlockEvent implements Cancellable {

    protected final Player player;

    protected final ItemStack item;

    protected final BlockState blockPlace;
    protected final Block blockAgainst;

    public BlockPlaceEvent(Player player, BlockState blockPlace, Block blockReplace, Block blockAgainst, ItemStack item) {
        super(blockReplace);
        this.blockPlace = blockPlace;
        this.blockAgainst = blockAgainst;
        this.item = item;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }

    public BlockState getBlockPlace() {
        return blockPlace;
    }

    public Block getBlockAgainst() {
        return blockAgainst;
    }
}

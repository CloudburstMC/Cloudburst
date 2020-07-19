package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected final Player player;

    protected final Item item;
    protected final BlockFace face;

    protected boolean instaBreak = false;
    protected Item[] blockDrops = new Item[0];
    protected int blockXP = 0;

    protected boolean fastBreak = false;

    public BlockBreakEvent(Player player, Block block, Item item, Item[] drops) {
        this(player, block, item, drops, false, false);
    }

    public BlockBreakEvent(Player player, Block block, Item item, Item[] drops, boolean instaBreak) {
        this(player, block, item, drops, instaBreak, false);
    }

    public BlockBreakEvent(Player player, Block block, Item item, Item[] drops, boolean instaBreak, boolean fastBreak) {
        this(player, block, null, item, drops,
                BlockRegistry.get().getBehavior(block.getState().getType()).getDropExp(), instaBreak, fastBreak);
    }

    public BlockBreakEvent(Player player, Block block, BlockFace face, Item item, Item[] drops, int dropExp, boolean instaBreak, boolean fastBreak) {
        super(block);
        this.face = face;
        this.item = item;
        this.player = player;
        this.instaBreak = instaBreak;
        this.blockDrops = drops;
        this.fastBreak = fastBreak;
        this.blockXP = dropExp;
    }

    public Player getPlayer() {
        return player;
    }

    public BlockFace getFace() {
        return face;
    }

    public Item getItem() {
        return item;
    }

    public boolean getInstaBreak() {
        return this.instaBreak;
    }

    public Item[] getDrops() {
        return blockDrops;
    }

    public void setDrops(Item[] drops) {
        this.blockDrops = drops;
    }

    public int getDropExp() {
        return this.blockXP;
    }

    public void setDropExp(int xp) {
        this.blockXP = xp;
    }

    public void setInstaBreak(boolean instaBreak) {
        this.instaBreak = instaBreak;
    }

    public boolean isFastBreak() {
        return this.fastBreak;
    }
}

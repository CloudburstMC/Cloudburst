package org.cloudburstmc.server.event.block;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBreakEvent extends BlockEvent implements Cancellable {

    protected final Player player;

    protected final ItemStack item;
    protected final Direction face;

    protected boolean instaBreak = false;
    protected ItemStack[] blockDrops = new ItemStack[0];
    protected int blockXP = 0;

    protected boolean fastBreak = false;

    public BlockBreakEvent(Player player, Block block, ItemStack item, ItemStack[] drops) {
        this(player, block, item, drops, false, false);
    }

    public BlockBreakEvent(Player player, Block block, ItemStack item, ItemStack[] drops, boolean instaBreak) {
        this(player, block, item, drops, instaBreak, false);
    }

    public BlockBreakEvent(Player player, Block block, ItemStack item, ItemStack[] drops, boolean instaBreak, boolean fastBreak) {
        this(player, block, null, item, drops,
                block.getState().getBehavior().getDropExp(), instaBreak, fastBreak);
    }

    public BlockBreakEvent(Player player, Block block, Direction face, ItemStack item, ItemStack[] drops, int dropExp, boolean instaBreak, boolean fastBreak) {
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

    public Direction getFace() {
        return face;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean getInstaBreak() {
        return this.instaBreak;
    }

    public ItemStack[] getDrops() {
        return blockDrops;
    }

    public void setDrops(ItemStack[] drops) {
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

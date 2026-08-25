package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Called when a player places a block.
 */
public class BlockPlaceEvent extends BlockEvent implements Cancellable {

    private final BlockState replacedState;
    private final Block blockAgainst;
    private final ItemStack itemInHand;
    private final Player player;
    private final EquipmentSlot hand;
    private boolean canBuild;

    /**
     * Creates an event for a block placement.
     *
     * @param placedBlock the block that would be placed
     * @param replacedState the state that would be replaced
     * @param blockAgainst the block against which placement was attempted
     * @param itemInHand the item used for placement
     * @param player the player placing the block
     * @param canBuild whether server policy initially permits the placement
     * @param hand the hand used for placement
     */
    public BlockPlaceEvent(Block placedBlock, BlockState replacedState, Block blockAgainst, ItemStack itemInHand,
                           Player player, boolean canBuild, EquipmentSlot hand) {
        super(placedBlock);
        this.replacedState = requireNonNull(replacedState, "replacedState");
        this.blockAgainst = requireNonNull(blockAgainst, "blockAgainst");
        this.itemInHand = requireNonNull(itemInHand, "itemInHand");
        this.player = requireNonNull(player, "player");
        this.hand = requireNonNull(hand, "hand");
        this.canBuild = canBuild;
    }

    /**
     * Returns the block that would be placed.
     *
     * @return the placed block
     */
    public Block getBlockPlaced() {
        return this.getBlock();
    }

    /**
     * Returns the state that would be replaced.
     *
     * @return the replaced state
     */
    public BlockState getBlockReplacedState() {
        return this.replacedState;
    }

    /**
     * Returns the block against which placement was attempted.
     *
     * @return the block placed against
     */
    public Block getBlockAgainst() {
        return this.blockAgainst;
    }

    /**
     * Returns the item used for placement.
     *
     * @return the item used for placement
     */
    public ItemStack getItemInHand() {
        return this.itemInHand;
    }

    /**
     * Returns the player placing the block.
     *
     * @return the player placing the block
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns the hand used to place the block.
     *
     * @return the hand used for placement
     */
    public EquipmentSlot getHand() {
        return this.hand;
    }

    /**
     * Returns whether server placement policy permits the placement.
     *
     * @return whether the player may build
     */
    public boolean canBuild() {
        return this.canBuild;
    }

    /**
     * Sets whether server placement policy should permit the placement.
     *
     * @param canBuild whether the player may build
     */
    public void setBuild(boolean canBuild) {
        this.canBuild = canBuild;
    }
}

package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorButton extends FloodableBlockBehavior {

    protected final Identifier type;

    public BlockBehaviorButton(Identifier type) {
        this.type = type;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.getState().inCategory(BlockCategory.TRANSPARENT)) {
            return false;
        }

        BlockState btn = BlockRegistry.get().getBlock(this.type).withTrait(BlockTraits.FACING_DIRECTION, face);
        placeBlock(block, btn);
        return true;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (this.isActivated(block)) {
            return false;
        }

        World world = block.getWorld();
        world.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));

        block.set(block.getState().withTrait(BlockTraits.IS_BUTTON_PRESSED, true), true, false);

        world.addSound(block.getPosition(), Sound.RANDOM_CLICK);
        world.scheduleUpdate(block, 30);

        world.updateAroundRedstone(block.getPosition(), null);
        world.updateAroundRedstone(this.getFacing(block).getOpposite().getOffset(block.getPosition()), null);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (block.getSide(getFacing(block).getOpposite()).getState().inCategory(BlockCategory.TRANSPARENT)) {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == World.BLOCK_UPDATE_SCHEDULED) {
            if (this.isActivated(block)) {
                World world = block.getWorld();
                world.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));

                block.set(block.getState().withTrait(BlockTraits.IS_BUTTON_PRESSED, false),
                        true, false);
                world.addSound(block.getPosition(), Sound.RANDOM_CLICK);

                world.updateAroundRedstone(block.getPosition(), null);
                world.updateAroundRedstone(this.getFacing(block).getOpposite().getOffset(block.getPosition()), null);
            }

            return World.BLOCK_UPDATE_SCHEDULED;
        }

        return 0;
    }

    public boolean isActivated(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_BUTTON_PRESSED);
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    public int getWeakPower(Block block, Direction side) {
        return isActivated(block) ? 15 : 0;
    }

    public int getStrongPower(Block block, Direction side) {
        return !isActivated(block) ? 0 : (getFacing(block) == side ? 15 : 0);
    }

    public Direction getFacing(Block block) {
        return block.getState().ensureTrait(BlockTraits.FACING_DIRECTION);
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        if (isActivated(block)) {
            block.getWorld().getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));
        }

        return super.onBreak(block, item);
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().getType());
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

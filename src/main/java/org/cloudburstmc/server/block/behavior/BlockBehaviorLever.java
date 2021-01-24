package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.LeverDirection;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorLever extends FloodableBlockBehavior {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().defaultState());
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{toItem(block)};
    }

    public boolean isPowerOn(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_OPEN);
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        val level = block.getWorld();
        val state = block.getState();

        boolean powerOn = isPowerOn(state);
        level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, powerOn ? 15 : 0, powerOn ? 0 : 15));


        block.set(state.toggleTrait(BlockTraits.IS_OPEN));
        level.addSound(block.getPosition(), Sound.RANDOM_CLICK, 0.8f, powerOn ? 0.5f : 0.58f);

        val face = state.ensureTrait(BlockTraits.LEVER_DIRECTION).getDirection();

        level.updateAroundRedstone(face.getOpposite().getOffset(block.getPosition()), powerOn ? null : face);
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            Direction direction = block.getState().ensureTrait(BlockTraits.LEVER_DIRECTION).getDirection().getOpposite();
            if (!block.getSide(direction).getState().inCategory(BlockCategory.SOLID)) {
                block.getWorld().useBreakOn(block.getPosition());
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.getState().getBehavior().isNormalBlock(target)) {
            return placeBlock(block, BlockState.get(BlockIds.LEVER)
                    .withTrait(
                            BlockTraits.LEVER_DIRECTION,
                            LeverDirection.forDirection(face, player.getHorizontalDirection())
                    )
            );
        }
        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);

        val state = block.getState();
        if (isPowerOn(state)) {
            Direction face = state.ensureTrait(BlockTraits.LEVER_DIRECTION).getDirection();
            block.getWorld().updateAround(face.getOpposite().getOffset(block.getPosition()));
        }
        return true;
    }

    @Override
    public int getWeakPower(Block block, Direction side) {
        return isPowerOn(block.getState()) ? 15 : 0;
    }

    public int getStrongPower(Block block, Direction side) {
        val state = block.getState();
        return !isPowerOn(state) ? 0 : state.ensureTrait(BlockTraits.LEVER_DIRECTION).getDirection() == side ? 15 : 0;
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }
}

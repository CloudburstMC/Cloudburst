package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.DoorToggleEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorFenceGate extends BlockBehaviorTransparent {

    private static final float[] offMinX = new float[2];
    private static final float[] offMinZ = new float[2];

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_AXE;
    }

    private static final float[] offMaxX = new float[2];
    private static final float[] offMaxZ = new float[2];

    static {
        offMinX[0] = 0;
        offMinZ[0] = 0.375f;
        offMaxX[0] = 1;
        offMaxZ[0] = 0.625f;

        offMinX[1] = 0.375f;
        offMinZ[1] = 0;
        offMaxX[1] = 0.625f;
        offMaxZ[1] = 1;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 15;
    }

//    private int getOffsetIndex() {
//        switch (this.getMeta() & 0x03) {
//            case 0:
//            case 2:
//                return 0;
//            default:
//                return 1;
//        }
//    }
//
//    @Override
//    public float getMinX() {
//        return this.getX() + offMinX[getOffsetIndex()];
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + offMinZ[getOffsetIndex()];
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + offMaxX[getOffsetIndex()];
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + offMaxZ[getOffsetIndex()];
//    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.DIRECTION, player != null ? player.getHorizontalDirection() : Direction.NORTH));
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player == null) {
            return false;
        }

        if (!this.toggle(block, player)) {
            return false;
        }

        block.getLevel().addSound(block.getPosition(), isOpen(block) ? Sound.RANDOM_DOOR_OPEN : Sound.RANDOM_DOOR_CLOSE);
        return true;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }

    public boolean toggle(Block block, Player player) {
        DoorToggleEvent event = new DoorToggleEvent(block, player);
        block.getLevel().getServer().getEventManager().fire(event);

        if (event.isCancelled()) {
            return false;
        }

        player = event.getPlayer();

        val state = block.getState();
        var direction = state.ensureTrait(BlockTraits.DIRECTION);

        if (player != null) {
            val playerDirection = player.getHorizontalDirection();

            if (playerDirection == direction.getOpposite()) {
                direction = playerDirection;
            }
        }

        block.set(state.withTrait(BlockTraits.DIRECTION, direction).toggleTrait(BlockTraits.IS_OPEN));
        return true;
    }

    public boolean isOpen(Block block) {
        return block.getState().ensureTrait(BlockTraits.IS_OPEN);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            val level = block.getLevel();
            if ((!isOpen(block) && level.isBlockPowered(block.getPosition())) || (isOpen(block) && !level.isBlockPowered(block.getPosition()))) {
                this.toggle(block, null);
                return type;
            }
        }

        return 0;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.trait.EnumBlockTrait;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.data.WallConnectionType;

import java.util.EnumMap;
import java.util.Map;

public class BlockBehaviorWall extends BlockBehaviorTransparent {

    private static final EnumBlockTrait<WallConnectionType>[] DIRECTION_MAP = new EnumBlockTrait[Direction.values().length];

    static {
        DIRECTION_MAP[Direction.NORTH.ordinal()] = BlockTraits.WALL_CONNECTION_NORTH;
        DIRECTION_MAP[Direction.SOUTH.ordinal()] = BlockTraits.WALL_CONNECTION_SOUTH;
        DIRECTION_MAP[Direction.WEST.ordinal()] = BlockTraits.WALL_CONNECTION_WEST;
        DIRECTION_MAP[Direction.EAST.ordinal()] = BlockTraits.WALL_CONNECTION_EAST;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 30;
    }

//    @Override //TODO: bounding box
//    protected AxisAlignedBB recalculateBoundingBox() {
//
//        boolean north = this.canConnect(this.getSide(Direction.NORTH));
//        boolean south = this.canConnect(this.getSide(Direction.SOUTH));
//        boolean west = this.canConnect(this.getSide(Direction.WEST));
//        boolean east = this.canConnect(this.getSide(Direction.EAST));
//
//        float n = north ? 0 : 0.25f;
//        float s = south ? 1 : 0.75f;
//        float w = west ? 0 : 0.25f;
//        float e = east ? 1 : 0.75f;
//
//        if (north && south && !west && !east) {
//            w = 0.3125f;
//            e = 0.6875f;
//        } else if (!north && !south && west && east) {
//            n = 0.3125f;
//            s = 0.6875f;
//        }
//
//        return new SimpleAxisAlignedBB(
//                this.getX() + w,
//                this.getY(),
//                this.getZ() + n,
//                this.getX() + e,
//                this.getY() + 1.5f,
//                this.getZ() + s
//        );
//    }
//
//    public boolean canConnect(BlockState blockState) {
//        return (!(blockState.getId() != COBBLESTONE_WALL && blockState instanceof BlockBehaviorFence)) || blockState.isSolid() && !blockState.isTransparent();
//    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    public EnumBlockTrait<WallConnectionType> getConnectionTypeTrait(Direction direction) {
        return DIRECTION_MAP[direction.ordinal()];
    }

    protected Map<Direction, WallConnectionType> findConnections(Block block) {
        EnumMap<Direction, WallConnectionType> connections = new EnumMap<>(Direction.class);

        for (Direction face : Plane.HORIZONTAL) {
            BlockState state = block.getSideState(face);
            BlockState upState = block.upState();

            boolean wall = state.inCategory(BlockCategory.WALLS);
            boolean transparent = state.inCategory(BlockCategory.TRANSPARENT);
            boolean solid = state.inCategory(BlockCategory.SOLID);

            WallConnectionType connectType = upState.inCategory(BlockCategory.SOLID) ? WallConnectionType.TALL : WallConnectionType.SHORT;

            if (wall || solid && !transparent) {
                connections.put(face, connectType);
            } else {
                connections.put(face, WallConnectionType.NONE);
            }
        }

        return connections;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            BlockState state = block.getState();
            BlockState newState = state;

            val connections = findConnections(block);

            for (Direction face : Plane.HORIZONTAL) {
                val connectionType = connections.get(face);
                val trait = getConnectionTypeTrait(face);
                if (state.ensureTrait(trait) != connectionType) {
                    newState = newState.withTrait(trait, connectionType);
                }
            }

            if (newState != state) {
                block.set(newState);
            }

            return Level.BLOCK_UPDATE_NORMAL;
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val connections = findConnections(block);

        BlockState newState = item.getBlock();

        for (Direction direction : Plane.HORIZONTAL) {
            newState = newState.withTrait(
                    getConnectionTypeTrait(direction),
                    connections.get(direction)
            );
        }

        return placeBlock(block, newState);
    }
}

package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.Rail;
import org.cloudburstmc.server.utils.data.RailDirection;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.cloudburstmc.server.block.BlockIds.RAIL;

public class BlockBehaviorRail extends FloodableBlockBehavior {

    protected final Identifier type;
    protected final BlockTrait<RailDirection> directionTrait;

    public BlockBehaviorRail(Identifier type, BlockTrait<RailDirection> directionTrait) {
        this.type = type;
        this.directionTrait = directionTrait;
    }

    // 0x8: Set the block active
    // 0x7: Reset the block to normal
    // If the rail can be powered. So its a complex rail!
    protected boolean canBePowered = false;

    @Override
    public float getHardness() {
        return 0.7f;
    }

    @Override
    public float getResistance() {
        return 3.5f;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            Optional<Direction> ascendingDirection = this.getOrientation(block.getState()).ascendingDirection();
            if (block.down().getState().inCategory(BlockCategory.TRANSPARENT) || (ascendingDirection.isPresent() && block.getSide(ascendingDirection.get()).getState().inCategory(BlockCategory.TRANSPARENT))) {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.125f;
//    }
//
//    @Override
//    public AxisAlignedBB recalculateBoundingBox() {
//        return this;
//    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    //Information from http://minecraft.gamepedia.com/Rail
    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val down = block.down().getState();
        if (down.inCategory(BlockCategory.TRANSPARENT)) {
            return false;
        }
        Map<Block, Direction> railsAround = this.checkRailsAroundAffected(block);
        List<Block> rails = new ArrayList<>(railsAround.keySet());
        List<Direction> faces = new ArrayList<>(railsAround.values());

        RailDirection direction;
        if (railsAround.size() == 1) {
            Block other = rails.get(0);
            direction = this.connect(block, other, railsAround.get(other));
        } else if (railsAround.size() == 4) {
            if (this.isAbstract()) {
                direction = this.connect(block, rails.get(faces.indexOf(Direction.SOUTH)), Direction.SOUTH, rails.get(faces.indexOf(Direction.EAST)), Direction.EAST);
            } else {
                direction = this.connect(block, rails.get(faces.indexOf(Direction.EAST)), Direction.EAST, rails.get(faces.indexOf(Direction.WEST)), Direction.WEST);
            }
        } else if (!railsAround.isEmpty()) {
            if (this.isAbstract()) {
                if (railsAround.size() == 2) {
                    Block rail1 = rails.get(0);
                    Block rail2 = rails.get(1);
                    direction = this.connect(block, rail1, railsAround.get(rail1), rail2, railsAround.get(rail2));
                } else {
                    List<Direction> cd = Stream.of(RailDirection.CURVED_SOUTH_EAST, RailDirection.CURVED_NORTH_EAST, RailDirection.CURVED_SOUTH_WEST)
                            .filter(o -> faces.containsAll(o.connectingDirections()))
                            .findFirst().get().connectingDirections();
                    Direction f1 = cd.get(0);
                    Direction f2 = cd.get(1);
                    direction = this.connect(block, rails.get(faces.indexOf(f1)), f1, rails.get(faces.indexOf(f2)), f2);
                }
            } else {
                Direction f = faces.stream().min((f1, f2) -> (f1.getIndex() < f2.getIndex()) ? 1 : ((block.getX() == block.getY()) ? 0 : -1)).get();
                Direction fo = f.getOpposite();
                if (faces.contains(fo)) { //Opposite connectable
                    direction = this.connect(block, rails.get(faces.indexOf(f)), f, rails.get(faces.indexOf(fo)), fo);
                } else {
                    direction = this.connect(block, rails.get(faces.indexOf(f)), f);
                }
            }
        } else {
            direction = RailDirection.NORTH_SOUTH;
        }

        placeBlock(block, BlockState.get(this.type).withTrait(directionTrait, direction));
        if (!isAbstract()) {
            block.getWorld().scheduleUpdate(block.getPosition(), 0);
        }
        return true;
    }

    private RailDirection connect(Block block, Block rail1, Direction face1, Block rail2, Direction face2) {
        this.connect(block, rail1, face1);
        this.connect(block, rail2, face2);

        if (face1.getOpposite() == face2) {
            int delta1 = block.getY() - rail1.getY();
            int delta2 = block.getY() - rail2.getY();

            if (delta1 == -1) {
                return RailDirection.ascending(face1);
            } else if (delta2 == -1) {
                return RailDirection.ascending(face2);
            }
        }
        return RailDirection.straightOrCurved(face1, face2);
    }

    private RailDirection connect(Block block, Block other, Direction face) {
        int delta = block.getY() - other.getY();
        Map<Block, Direction> rails = checkRailsConnected(block);

        val behaviorOther = (BlockBehaviorRail) other.getState().getBehavior();

        if (rails.isEmpty()) { //Only one
            behaviorOther.setOrientation(other, delta == 1 ? RailDirection.ascending(face.getOpposite()) : RailDirection.straight(face));
            return delta == -1 ? RailDirection.ascending(face) : RailDirection.straight(face);
        } else if (rails.size() == 1) { //Already connected
            Direction faceConnected = rails.values().iterator().next();

            if (behaviorOther.isAbstract() && faceConnected != face) { //Curve!
                behaviorOther.setOrientation(other, RailDirection.curved(face.getOpposite(), faceConnected));
                return delta == -1 ? RailDirection.ascending(face) : RailDirection.straight(face);
            } else if (faceConnected == face) { //Turn!
                if (!behaviorOther.getOrientation(block.getState()).isAscending()) {
                    behaviorOther.setOrientation(other, delta == 1 ? RailDirection.ascending(face.getOpposite()) : RailDirection.straight(face));
                }
                return delta == -1 ? RailDirection.ascending(face) : RailDirection.straight(face);
            } else if (behaviorOther.getOrientation(block.getState()).hasConnectingDirections(Direction.NORTH, Direction.SOUTH)) { //North-south
                behaviorOther.setOrientation(other, delta == 1 ? RailDirection.ascending(face.getOpposite()) : RailDirection.straight(face));
                return delta == -1 ? RailDirection.ascending(face) : RailDirection.straight(face);
            }
        }
        return RailDirection.NORTH_SOUTH;
    }

    private Map<Block, Direction> checkRailsAroundAffected(Block block) {
        Map<Block, Direction> railsAround = this.checkRailsAround(block, Arrays.asList(Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH));
        return railsAround.keySet().stream()
                .filter(r -> ((BlockBehaviorRail) r.getState().getBehavior()).checkRailsConnected(r).size() != 2)
                .collect(Collectors.toMap(r -> r, railsAround::get));
    }

    private Map<Block, Direction> checkRailsAround(Block block, Collection<Direction> faces) {
        Map<Block, Direction> result = new HashMap<>();
        faces.forEach(f -> {
            val b = block.getSide(f);
            Stream.of(b, b.up(), b.down())
                    .filter(bl -> Rail.isRailBlock(bl.getState()))
                    .forEach(bl -> result.put(bl, f));
        });
        return result;
    }

    protected Map<Block, Direction> checkRailsConnected(Block block) {
        val state = block.getState();
        Map<Block, Direction> railsAround = this.checkRailsAround(block, this.getOrientation(state).connectingDirections());
        return railsAround.keySet().stream()
                .filter(r -> ((BlockBehaviorRail) r.getState().getBehavior()).getOrientation(r.getState()).hasConnectingDirections(railsAround.get(r).getOpposite()))
                .collect(Collectors.toMap(r -> r, railsAround::get));
    }

    public boolean isAbstract() {
        return this.type == RAIL;
    }

    public boolean canPowered() {
        return this.canBePowered;
    }

    public RailDirection getOrientation(BlockState state) {
        return state.ensureTrait(directionTrait);
    }

    public void setOrientation(Block block, RailDirection o) {
        val state = block.getState();
        if (state.ensureTrait(directionTrait) != o) {
            block.set(state.withTrait(directionTrait, o));
        }
    }

    public boolean isActive(BlockState state) {
        return state.getTrait(BlockTraits.IS_POWERED) == Boolean.TRUE;
    }

    public void setActive(Block block, boolean active) {
        val state = block.getState();
        val current = state.getTrait(BlockTraits.IS_POWERED);
        if (current != null && current != active) {
            block.set(state.toggleTrait(BlockTraits.IS_POWERED), true);
        }
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(block.getState().defaultState());
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(RAIL)
        };
    }
}

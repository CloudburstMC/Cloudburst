package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Rail;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.cloudburstmc.server.block.BlockTypes.RAIL;

public class BlockBehaviorRail extends FloodableBlockBehavior {

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
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            Optional<Direction> ascendingDirection = this.getOrientation().ascendingDirection();
            if (this.down().isTransparent() || (ascendingDirection.isPresent() && this.getSide(ascendingDirection.get()).isTransparent())) {
                this.getLevel().useBreakOn(this.getPosition());
                return Level.BLOCK_UPDATE_NORMAL;
            }
        }
        return 0;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.125f;
    }

    @Override
    public AxisAlignedBB recalculateBoundingBox() {
        return this;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    //Information from http://minecraft.gamepedia.com/Rail
    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (down == null || down.isTransparent()) {
            return false;
        }
        Map<BlockBehaviorRail, Direction> railsAround = this.checkRailsAroundAffected();
        List<BlockBehaviorRail> rails = new ArrayList<>(railsAround.keySet());
        List<Direction> faces = new ArrayList<>(railsAround.values());
        if (railsAround.size() == 1) {
            BlockBehaviorRail other = rails.get(0);
            this.setMeta(this.connect(other, railsAround.get(other)).metadata());
        } else if (railsAround.size() == 4) {
            if (this.isAbstract()) {
                this.setMeta(this.connect(rails.get(faces.indexOf(Direction.SOUTH)), Direction.SOUTH, rails.get(faces.indexOf(Direction.EAST)), Direction.EAST).metadata());
            } else {
                this.setMeta(this.connect(rails.get(faces.indexOf(Direction.EAST)), Direction.EAST, rails.get(faces.indexOf(Direction.WEST)), Direction.WEST).metadata());
            }
        } else if (!railsAround.isEmpty()) {
            if (this.isAbstract()) {
                if (railsAround.size() == 2) {
                    BlockBehaviorRail rail1 = rails.get(0);
                    BlockBehaviorRail rail2 = rails.get(1);
                    this.setMeta(this.connect(rail1, railsAround.get(rail1), rail2, railsAround.get(rail2)).metadata());
                } else {
                    List<Direction> cd = Stream.of(Rail.Orientation.CURVED_SOUTH_EAST, Rail.Orientation.CURVED_NORTH_EAST, Rail.Orientation.CURVED_SOUTH_WEST)
                            .filter(o -> faces.containsAll(o.connectingDirections()))
                            .findFirst().get().connectingDirections();
                    Direction f1 = cd.get(0);
                    Direction f2 = cd.get(1);
                    this.setMeta(this.connect(rails.get(faces.indexOf(f1)), f1, rails.get(faces.indexOf(f2)), f2).metadata());
                }
            } else {
                Direction f = faces.stream().min((f1, f2) -> (f1.getIndex() < f2.getIndex()) ? 1 : ((this.getX() == this.getY()) ? 0 : -1)).get();
                Direction fo = f.getOpposite();
                if (faces.contains(fo)) { //Opposite connectable
                    this.setMeta(this.connect(rails.get(faces.indexOf(f)), f, rails.get(faces.indexOf(fo)), fo).metadata());
                } else {
                    this.setMeta(this.connect(rails.get(faces.indexOf(f)), f).metadata());
                }
            }
        }
        this.level.setBlock(this.getPosition(), this, true, true);
        if (!isAbstract()) {
            level.scheduleUpdate(this, this.getPosition(), 0);
        }
        return true;
    }

    private Rail.Orientation connect(BlockBehaviorRail rail1, Direction face1, BlockBehaviorRail rail2, Direction face2) {
        this.connect(rail1, face1);
        this.connect(rail2, face2);

        if (face1.getOpposite() == face2) {
            int delta1 = (int) (this.getY() - rail1.getY());
            int delta2 = (int) (this.getY() - rail2.getY());

            if (delta1 == -1) {
                return Rail.Orientation.ascending(face1);
            } else if (delta2 == -1) {
                return Rail.Orientation.ascending(face2);
            }
        }
        return Rail.Orientation.straightOrCurved(face1, face2);
    }

    private Rail.Orientation connect(BlockBehaviorRail other, Direction face) {
        int delta = (int) (this.getY() - other.getY());
        Map<BlockBehaviorRail, Direction> rails = other.checkRailsConnected();
        if (rails.isEmpty()) { //Only one
            other.setOrientation(delta == 1 ? Rail.Orientation.ascending(face.getOpposite()) : Rail.Orientation.straight(face));
            return delta == -1 ? Rail.Orientation.ascending(face) : Rail.Orientation.straight(face);
        } else if (rails.size() == 1) { //Already connected
            Direction faceConnected = rails.values().iterator().next();

            if (other.isAbstract() && faceConnected != face) { //Curve!
                other.setOrientation(Rail.Orientation.curved(face.getOpposite(), faceConnected));
                return delta == -1 ? Rail.Orientation.ascending(face) : Rail.Orientation.straight(face);
            } else if (faceConnected == face) { //Turn!
                if (!other.getOrientation().isAscending()) {
                    other.setOrientation(delta == 1 ? Rail.Orientation.ascending(face.getOpposite()) : Rail.Orientation.straight(face));
                }
                return delta == -1 ? Rail.Orientation.ascending(face) : Rail.Orientation.straight(face);
            } else if (other.getOrientation().hasConnectingDirections(Direction.NORTH, Direction.SOUTH)) { //North-south
                other.setOrientation(delta == 1 ? Rail.Orientation.ascending(face.getOpposite()) : Rail.Orientation.straight(face));
                return delta == -1 ? Rail.Orientation.ascending(face) : Rail.Orientation.straight(face);
            }
        }
        return Rail.Orientation.STRAIGHT_NORTH_SOUTH;
    }

    private Map<BlockBehaviorRail, Direction> checkRailsAroundAffected() {
        Map<BlockBehaviorRail, Direction> railsAround = this.checkRailsAround(Arrays.asList(Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.NORTH));
        return railsAround.keySet().stream()
                .filter(r -> r.checkRailsConnected().size() != 2)
                .collect(Collectors.toMap(r -> r, railsAround::get));
    }

    private Map<BlockBehaviorRail, Direction> checkRailsAround(Collection<Direction> faces) {
        Map<BlockBehaviorRail, Direction> result = new HashMap<>();
        faces.forEach(f -> {
            BlockState b = this.getSide(f);
            Stream.of(b, b.up(), b.down())
                    .filter(Rail::isRailBlock)
                    .forEach(block -> result.put((BlockBehaviorRail) block, f));
        });
        return result;
    }

    protected Map<BlockBehaviorRail, Direction> checkRailsConnected() {
        Map<BlockBehaviorRail, Direction> railsAround = this.checkRailsAround(this.getOrientation().connectingDirections());
        return railsAround.keySet().stream()
                .filter(r -> r.getOrientation().hasConnectingDirections(railsAround.get(r).getOpposite()))
                .collect(Collectors.toMap(r -> r, railsAround::get));
    }

    public boolean isAbstract() {
        return this.getId() == RAIL;
    }

    public boolean canPowered() {
        return this.canBePowered;
    }

    public Rail.Orientation getOrientation() {
        return Rail.Orientation.byMetadata(this.getRealMeta());
    }

    public void setOrientation(Rail.Orientation o) {
        if (o.metadata() != this.getRealMeta()) {
            this.setMeta(o.metadata());
            this.level.setBlock(this.getPosition(), this, false, true);
        }
    }

    public int getRealMeta() {
        // Check if this can be powered
        // Avoid modifying the value from meta (The rail orientation may be false)
        // Reason: When the rail is curved, the meta will return STRAIGHT_NORTH_SOUTH.
        // OR Null Pointer Exception
        if (!isAbstract()) {
            return getMeta() & 0x7;
        }
        // Return the default: This meta
        return getMeta();
    }

    public boolean isActive() {
        return (getMeta() & 0x8) != 0;
    }

    public void setActive(boolean active) {
        if (active) {
            setMeta(getMeta() | 0x8);
        } else {
            setMeta(getMeta() & 0x7);
        }
        level.setBlock(this.getPosition(), this, true, true);
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(id, 0);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        return new Item[]{
                Item.get(RAIL)
        };
    }
}

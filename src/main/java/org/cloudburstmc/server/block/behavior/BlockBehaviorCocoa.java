package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.particle.BoneMealParticle;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.data.DyeColor;
import org.cloudburstmc.server.utils.data.TreeSpecies;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by CreeperFace on 27. 10. 2016.
 */
public class BlockBehaviorCocoa extends BlockBehaviorTransparent {
    protected static final AxisAlignedBB[] BB_NORTH = {
            new SimpleAxisAlignedBB(0.375f, 0.4375f, 0.0625f, 0.625f, 0.75f, 0.3125f),
            new SimpleAxisAlignedBB(0.3125f, 0.3125f, 0.0625f, 0.6875f, 0.75f, 0.4375f),
            new SimpleAxisAlignedBB(0.3125f, 0.3125f, 0.0625f, 0.6875f, 0.75f, 0.4375f)
    };
    protected static final AxisAlignedBB[] BB_EAST = {
            new SimpleAxisAlignedBB(0.6875f, 0.4375f, 0.375f, 0.9375f, 0.75f, 0.625f),
            new SimpleAxisAlignedBB(0.5625f, 0.3125f, 0.3125f, 0.9375f, 0.75f, 0.6875f),
            new SimpleAxisAlignedBB(0.5625f, 0.3125f, 0.3125f, 0.9375f, 0.75f, 0.6875f)
    };
    protected static final AxisAlignedBB[] BB_SOUTH = {
            new SimpleAxisAlignedBB(0.375f, 0.4375f, 0.6875f, 0.625f, 0.75f, 0.9375f),
            new SimpleAxisAlignedBB(0.3125f, 0.3125f, 0.5625f, 0.6875f, 0.75f, 0.9375f),
            new SimpleAxisAlignedBB(0.3125f, 0.3125f, 0.5625f, 0.6875f, 0.75f, 0.9375f)
    };
    protected static final AxisAlignedBB[] BB_WEST = {
            new SimpleAxisAlignedBB(0.0625f, 0.4375f, 0.375f, 0.3125f, 0.75f, 0.625f),
            new SimpleAxisAlignedBB(0.0625f, 0.3125f, 0.3125f, 0.4375f, 0.75f, 0.6875f),
            new SimpleAxisAlignedBB(0.0625f, 0.3125f, 0.3125f, 0.4375f, 0.75f, 0.6875f)
    };
    protected static final AxisAlignedBB[] BB_ALL = {
            BB_NORTH[0], BB_EAST[0], BB_SOUTH[0], BB_WEST[0],
            BB_NORTH[1], BB_EAST[1], BB_SOUTH[1], BB_WEST[1],
            BB_NORTH[2], BB_EAST[2], BB_SOUTH[2], BB_WEST[2],
    };

    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;
    public static final int DIR_MASK = 3;

    public static final int STAGE_1 = 0;
    public static final int STAGE_2 = 1;
    public static final int STAGE_3 = 2;
    public static final int STAGE_MASK = 12;


//    @Override
//    public float getMinX() {
//        return this.getX() + getRelativeBoundingBox().getMinX();
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + getRelativeBoundingBox().getMaxX();
//    }
//
//    @Override
//    public float getMinY() {
//        return this.getY() + getRelativeBoundingBox().getMinY();
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + getRelativeBoundingBox().getMaxY();
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + getRelativeBoundingBox().getMinZ();
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + getRelativeBoundingBox().getMaxZ();
//    }

    private AxisAlignedBB getRelativeBoundingBox(Block block) {
//        int meta = this.getMeta();
//        if (meta > 11) {
//            this.setMeta(meta = 11);
//        }

        return BB_ALL[0]; //TODO:
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (target.getState().getType() == BlockIds.LOG && target.getState().ensureTrait(BlockTraits.TREE_SPECIES) == TreeSpecies.JUNGLE) {
            if (face != Direction.DOWN && face != Direction.UP) {
                placeBlock(block, BlockRegistry.get().getBlock(BlockIds.COCOA)
                        .withTrait(BlockTraits.DIRECTION, face));
                return true;
            }
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            BlockState side = block.getSide(block.getState().ensureTrait(BlockTraits.DIRECTION)).getState();

            if (side.getType() != BlockIds.LOG || side.ensureTrait(BlockTraits.TREE_SPECIES) != TreeSpecies.JUNGLE) {
                block.getWorld().useBreakOn(block.getPosition());
                return World.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == World.BLOCK_UPDATE_RANDOM) {
            if (ThreadLocalRandom.current().nextInt(2) == 1) {
                if (!grow(block)) {
                    return World.BLOCK_UPDATE_RANDOM;
                }
            } else {
                return World.BLOCK_UPDATE_RANDOM;
            }
        }

        return 0;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (item.getId() == ItemIds.DYE && item.getMeta() == 0x0f) {
            if (grow(block)) {
                block.getWorld().addParticle(new BoneMealParticle(block.getPosition()));

                if (player != null && player.getGamemode().isSurvival()) {
                    item.decrementCount();
                }
            }

            return true;
        }

        return false;
    }

    public boolean grow(Block block) {
        int age = block.getState().ensureTrait(BlockTraits.AGE);
        if (age < 2) {
            BlockState cocoa = block.getState().incrementTrait(BlockTraits.AGE);

            BlockGrowEvent ev = new BlockGrowEvent(block, cocoa);
            Server.getInstance().getEventManager().fire(ev);

            if (!ev.isCancelled()) {
                block.set(ev.getNewState(), true, true);
                return true;
            }
        }

        return false;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public float getHardness() {
        return 0.2f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.DYE, DyeColor.BROWN.getDyeData());
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (block.getState().ensureTrait(BlockTraits.AGE) >= 2) {
            return new Item[]{
                    Item.get(ItemIds.DYE, 3, 3)
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.DYE, 3, 1)
            };
        }
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

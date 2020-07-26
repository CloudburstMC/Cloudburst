package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.FlowerPot;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorFlowerPot extends FloodableBlockBehavior {

    protected static boolean canPlaceIntoFlowerPot(Identifier id) {
        return id == BlockTypes.SAPLING || id == BlockTypes.WEB || id == BlockTypes.TALL_GRASS || id == BlockTypes.DEADBUSH || id == BlockTypes.YELLOW_FLOWER ||
                id == BlockTypes.RED_FLOWER || id == BlockTypes.RED_MUSHROOM || id == BlockTypes.BROWN_MUSHROOM || id == BlockTypes.CACTUS || id == BlockTypes.REEDS;
        // TODO: 2016/2/4 case NETHER_WART:
    }

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.UP) return false;

        FlowerPot flowerPot = BlockEntityRegistry.get().newEntity(BlockEntityTypes.FLOWER_POT, this.getChunk(), this.getPosition());
        flowerPot.loadAdditionalData(item.getTag());

        this.getLevel().setBlock(blockState.getPosition(), this, true, true);
        return true;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item) {
        return this.onActivate(item, null);
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockEntity blockEntity = getLevel().getBlockEntity(this.getPosition());
        if (!(blockEntity instanceof FlowerPot)) return false;
        FlowerPot flowerPot = (FlowerPot) blockEntity;

        Identifier blockId;
        int blockMeta;
        if (!canPlaceIntoFlowerPot(item.getId())) {
            if (!canPlaceIntoFlowerPot(item.getBlock().getId())) {
                return true;
            }
            blockId = item.getBlock().getId();
            blockMeta = item.getMeta();
        } else if (item.getBlock().getId() != BlockTypes.AIR) {
            blockId = item.getBlock().getId();
            blockMeta = item.getMeta();
        } else {
            return true;
        }
        flowerPot.setPlant(BlockState.get(blockId, blockMeta));

        this.setMeta(1);
        this.getLevel().setBlock(this.getPosition(), this, true);
        blockEntity.spawnToAll();

        if (player.isSurvival()) {
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item.getCount() > 0 ? item : Item.get(BlockTypes.AIR));
        }
        return true;
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        boolean dropInside = false;
        BlockState blockState = BlockState.AIR;
        BlockEntity blockEntity = getLevel().getBlockEntity(this.getPosition());
        if (blockEntity instanceof FlowerPot) {
            dropInside = true;
            blockState = ((FlowerPot) blockEntity).getPlant();
        }

        if (dropInside) {
            return new Item[]{
                    Item.get(ItemIds.FLOWER_POT),
                    blockState.toItem(blockState)
            };
        } else {
            return new Item[]{
                    Item.get(ItemIds.FLOWER_POT)
            };
        }
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        return this;
    }

    @Override
    public float getMinX() {
        return this.getX() + 0.3125f;
    }

    @Override
    public float getMinZ() {
        return this.getZ() + 0.3125f;
    }

    @Override
    public float getMaxX() {
        return this.getX() + 0.6875f;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.375f;
    }

    @Override
    public float getMaxZ() {
        return this.getZ() + 0.6875f;
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.FLOWER_POT);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

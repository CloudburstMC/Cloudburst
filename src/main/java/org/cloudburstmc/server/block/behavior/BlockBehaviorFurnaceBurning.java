package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Furnace;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorFurnaceBurning extends BlockBehaviorSolid {
    private BlockEntityType<? extends Furnace> furnaceEntity;

    protected BlockBehaviorFurnaceBurning(BlockEntityType<? extends Furnace> entity) {
        this.furnaceEntity = entity;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public float getHardness() {
        return 3.5f;
    }

    @Override
    public float getResistance() {
        return 17.5f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int getLightLevel() {
        return 13;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        int[] faces = {2, 5, 3, 4};
        this.setMeta(faces[player != null ? player.getDirection().getHorizontalIndex() : 0]);
        this.getLevel().setBlock(block.getPosition(), this, true, true);

        Furnace furnace = BlockEntityRegistry.get().newEntity(furnaceEntity, this.getChunk(), this.getPosition());
        furnace.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            furnace.setCustomName(item.getCustomName());
        }

        return true;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        this.getLevel().setBlock(this.getPosition(), BlockState.get(AIR), true, true);
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
            Furnace furnace;
            if (blockEntity instanceof Furnace) {
                furnace = (Furnace) blockEntity;
            } else {
                furnace = BlockEntityRegistry.get().newEntity(furnaceEntity, this.getChunk(), this.getPosition());
            }

            player.addWindow(furnace.getInventory());
        }

        return true;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(getId(), 0);
    }

    @Override
    public Item[] getDrops(Block block, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    this.toItem(block)
            };
        } else {
            return new Item[0];
        }
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(Block block) {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof Furnace) {
            return ContainerInventory.calculateRedstone(((Furnace) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride(block);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x7);
    }

}

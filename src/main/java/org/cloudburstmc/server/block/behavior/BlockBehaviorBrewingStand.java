package org.cloudburstmc.server.block.behavior;


import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BrewingStand;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.BREWING_STAND;

public class BlockBehaviorBrewingStand extends BlockBehaviorSolid {

    public BlockBehaviorBrewingStand(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
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
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public int getLightLevel() {
        return 1;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        if (!blockState.down().isTransparent()) {
            getLevel().setBlock(blockState.getPosition(), this, true, true);

            BrewingStand brewingStand = BlockEntityRegistry.get().newEntity(BREWING_STAND, this.getChunk(), this.getPosition());
            brewingStand.loadAdditionalData(item.getTag());
            if (item.hasCustomName()) {
                brewingStand.setCustomName(item.getCustomName());
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (player != null) {
            BlockEntity blockEntity = getLevel().getBlockEntity(this.getPosition());
            BrewingStand brewing;
            if (blockEntity instanceof BrewingStand) {
                brewing = (BrewingStand) blockEntity;
            } else {
                if (blockEntity != null) {
                    blockEntity.close();
                }

                brewing = BlockEntityRegistry.get().newEntity(BREWING_STAND, this.getChunk(), this.getPosition());
            }

            player.addWindow(brewing.getInventory());
        }

        return true;
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.BREWING_STAND);
    }

    @Override
    public Item[] getDrops(Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    toItem()
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride() {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof BrewingStand) {
            return ContainerInventory.calculateRedstone(((BrewingStand) blockEntity).getInventory());
        }

        return super.getComparatorInputOverride();
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.ShulkerBox;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.SHULKER_BOX;

/**
 * @author Reece Mackie
 */
public class BlockBehaviorUndyedShulkerBox extends BlockBehaviorTransparent {

    public BlockBehaviorUndyedShulkerBox(Identifier id) {
        super(id);
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item toItem() {

        ShulkerBox shulkerBox = (ShulkerBox) this.getLevel().getBlockEntity(this.getPosition());

        NbtMap tag = NbtMap.EMPTY;
        if (shulkerBox != null) {
            NbtMapBuilder tagBuilder = NbtMap.builder();
            shulkerBox.saveAdditionalData(tagBuilder);
            tag = tagBuilder.build();
        }

        return Item.get(this.id, 0, 1, tag);
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        this.getLevel().setBlock(blockState.getPosition(), this, true);

        ShulkerBox shulkerBox = BlockEntityRegistry.get().newEntity(SHULKER_BOX, this.getChunk(), this.getPosition());
        shulkerBox.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            shulkerBox.setCustomName(item.getCustomName());
        }
        return true;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (player != null) {
            BlockEntity t = this.getLevel().getBlockEntity(this.getPosition());
            ShulkerBox box;
            if (t instanceof ShulkerBox) {
                box = (ShulkerBox) t;
            } else {

                box = BlockEntityRegistry.get().newEntity(SHULKER_BOX, this.getChunk(), this.getPosition());
            }

            player.addWindow(box.getInventory());
        }

        return true;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.PURPLE_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

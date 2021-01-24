/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.ShulkerBox;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.SHULKER_BOX;

public class BlockBehaviorUndyedShulkerBox extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public Item toItem(Block block) {
        val be = block.getWorld().getBlockEntity(block.getPosition());

        var tag = NbtMap.EMPTY;
        if ((be instanceof ShulkerBox)) {
            ShulkerBox shulkerBox = (ShulkerBox) be;

            NbtMapBuilder tagBuilder = NbtMap.builder();
            shulkerBox.saveAdditionalData(tagBuilder);
            tag = tagBuilder.build();
        }

        return Item.get(block.getState().getType(), 0, 1, tag);
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        placeBlock(block, item);

        ShulkerBox shulkerBox = BlockEntityRegistry.get().newEntity(SHULKER_BOX, block);
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
    public boolean onActivate(Block block, Item item, Player player) {
        if (player != null) {
            BlockEntity t = block.getWorld().getBlockEntity(block.getPosition());
            ShulkerBox box;
            if (t instanceof ShulkerBox) {
                box = (ShulkerBox) t;
            } else {

                box = BlockEntityRegistry.get().newEntity(SHULKER_BOX, block);
            }

            player.addWindow(box.getInventory());
        }

        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.PURPLE_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

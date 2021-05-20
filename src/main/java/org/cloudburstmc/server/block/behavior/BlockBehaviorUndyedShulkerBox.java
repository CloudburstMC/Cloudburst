/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.ShulkerBox;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.blockentity.ShulkerBoxBlockEntity;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.blockentity.BlockEntityTypes.SHULKER_BOX;

public class BlockBehaviorUndyedShulkerBox extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        var be = block.getLevel().getBlockEntity(block.getPosition());

        var tag = NbtMap.EMPTY;
        if ((be instanceof ShulkerBoxBlockEntity)) {
            ShulkerBoxBlockEntity shulkerBox = (ShulkerBoxBlockEntity) be;

            NbtMapBuilder tagBuilder = NbtMap.builder();
            shulkerBox.saveAdditionalData(tagBuilder);
            tag = tagBuilder.build();
        }

        return new CloudItemStackBuilder((CloudItemStack) CloudItemRegistry.get().getItem(block.getState()))
                .dataTag(tag)
                .build();
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        placeBlock(block, item);

        ShulkerBoxBlockEntity shulkerBox = (ShulkerBoxBlockEntity) BlockEntityRegistry.get().newEntity(SHULKER_BOX, block);
        shulkerBox.loadAdditionalData(((CloudItemStack) item).getNbt());
        if (item.getName() != null) {
            shulkerBox.setCustomName(item.getName());
        }
        return true;
    }


    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            BlockEntity t = block.getLevel().getBlockEntity(block.getPosition());
            ShulkerBox box;
            if (t instanceof ShulkerBox) {
                box = (ShulkerBox) t;
            } else {

                box = BlockEntityRegistry.get().newEntity(SHULKER_BOX, block);
            }

            ((CloudPlayer) player).addWindow(box.getInventory());
        }

        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.PURPLE_BLOCK_COLOR;
    }


}

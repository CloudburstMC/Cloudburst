package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Jukebox;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.RecordItem;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Faceable;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.AIR;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.JUKEBOX;

/**
 * Created by CreeperFace on 7.8.2017.
 */
public class BlockBehaviorJukebox extends BlockBehaviorSolid implements Faceable {

    public BlockBehaviorJukebox(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public Item toItem() {
        return Item.get(id, 0);
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
        if (!(blockEntity instanceof Jukebox)) {
            blockEntity = this.createBlockEntity();
        }

        Jukebox jukebox = (Jukebox) blockEntity;
        if (jukebox.getRecordItem().getId() != AIR) {
            jukebox.dropItem();
        } else if (item instanceof RecordItem) {
            jukebox.setRecordItem(item);
            jukebox.play();
            player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
        }

        return false;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        if (super.place(item, blockState, target, face, clickPos, player)) {
            createBlockEntity();
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        if (super.onBreak(item)) {
            BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

            if (blockEntity instanceof Jukebox) {
                ((Jukebox) blockEntity).dropItem();
            }
            return true;
        }

        return false;
    }

    private BlockEntity createBlockEntity() {
        return BlockEntityRegistry.get().newEntity(JUKEBOX, this.getChunk(), this.getPosition());
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}

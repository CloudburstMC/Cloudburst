package cn.nukkit.block;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityJukebox;
import cn.nukkit.item.Item;
import cn.nukkit.item.RecordItem;
import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.player.Player;
import cn.nukkit.utils.Faceable;
import cn.nukkit.utils.Identifier;

import static cn.nukkit.block.BlockIds.AIR;

/**
 * Created by CreeperFace on 7.8.2017.
 */
public class BlockJukebox extends BlockSolid implements Faceable {

    public BlockJukebox(Identifier id) {
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
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this);
        if (!(blockEntity instanceof BlockEntityJukebox)) {
            blockEntity = this.createBlockEntity();
        }

        BlockEntityJukebox jukebox = (BlockEntityJukebox) blockEntity;
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
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        if (super.place(item, block, target, face, fx, fy, fz, player)) {
            createBlockEntity();
            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        if (super.onBreak(item)) {
            BlockEntity blockEntity = this.level.getBlockEntity(this);

            if (blockEntity instanceof BlockEntityJukebox) {
                ((BlockEntityJukebox) blockEntity).dropItem();
            }
            return true;
        }

        return false;
    }

    private BlockEntity createBlockEntity() {
        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Items"))
                .putString("id", BlockEntity.JUKEBOX)
                .putInt("x", getFloorX())
                .putInt("y", getFloorY())
                .putInt("z", getFloorZ());

        return BlockEntity.createBlockEntity(BlockEntity.JUKEBOX, this.level.getChunk(getFloorX() >> 4, getFloorZ() >> 4), nbt);
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getDamage() & 0x07);
    }
}

package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.Banner;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Faceable;
import org.cloudburstmc.server.utils.data.DyeColor;

import static org.cloudburstmc.server.block.BlockTypes.AIR;
import static org.cloudburstmc.server.block.BlockTypes.WALL_BANNER;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.BANNER;

public class BlockBehaviorBanner extends BlockBehaviorTransparent implements Faceable {

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    protected AxisAlignedBB recalculateBoundingBox() {
        return null;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        if (face != BlockFace.DOWN) {
            if (face == BlockFace.UP) {
                this.setMeta(NukkitMath.floorDouble(((player.getYaw() + 180) * 16 / 360) + 0.5) & 0x0f);
                this.getLevel().setBlock(blockState.getPosition(), this, true);
            } else {
                this.setMeta(face.getIndex());
                this.getLevel().setBlock(blockState.getPosition(), BlockState.get(WALL_BANNER, this.getMeta()), true);
            }

            NbtMapBuilder tag = NbtMap.builder();
            item.saveAdditionalData(tag);
            tag.putInt("Base", item.getMeta());

            BlockEntityRegistry.get().newEntity(BANNER, this.getChunk(), this.getPosition()).loadAdditionalData(tag.build());

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (this.down().getId() == AIR) {
                this.getLevel().useBreakOn(this.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public Item toItem(BlockState state) {
        BlockEntity blockEntity = this.getLevel().getBlockEntity(this.getPosition());
        Item item = Item.get(ItemIds.BANNER);
        if (blockEntity instanceof Banner) {
            Banner banner = (Banner) blockEntity;
            item.setMeta(banner.getBase().getDyeData());

            NbtMapBuilder tag = NbtMap.builder();
            banner.saveAdditionalData(tag);
            tag.remove("Base");

            item.loadAdditionalData(tag.build());
        }
        return item;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x7);
    }

    @Override
    public BlockColor getColor() {
        return this.getDyeColor().getColor();
    }

    public DyeColor getDyeColor() {
        if (this.level != null) {
            BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

            if (blockEntity instanceof Banner) {
                return ((Banner) blockEntity).getBase();
            }
        }

        return DyeColor.WHITE;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.Banner;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.CardinalDirection;
import org.cloudburstmc.server.utils.data.DyeColor;

import static org.cloudburstmc.server.block.BlockIds.AIR;
import static org.cloudburstmc.server.block.BlockIds.WALL_BANNER;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.BANNER;

public class BlockBehaviorBanner extends BlockBehaviorTransparent {

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
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.DOWN) {
            BlockState banner;
            if (face == Direction.UP) {
                banner = BlockRegistry.get().getBlock(BlockIds.STANDING_BANNER)
                        .withTrait(
                                BlockTraits.CARDINAL_DIRECTION,
                                CardinalDirection.values()[NukkitMath.floorDouble(((player.getYaw() + 180) * 16 / 360) + 0.5) & 0x0f]
                        );

            } else {
                banner = BlockRegistry.get().getBlock(WALL_BANNER)
                        .withTrait(BlockTraits.FACING_DIRECTION, face);
            }

            block.set(banner, true);

            NbtMapBuilder tag = NbtMap.builder();
            item.saveAdditionalData(tag);
            tag.putInt("Base", item.getMeta());

            BlockEntityRegistry.get().newEntity(BANNER, block.getChunk(), block.getPosition()).loadAdditionalData(tag.build());

            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() == AIR) {
                block.getWorld().useBreakOn(block.getPosition());

                return World.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public Item toItem(Block block) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
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
    public BlockColor getColor(Block block) {
        return this.getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Banner) {
            return ((Banner) blockEntity).getBase();
        }

        return DyeColor.WHITE;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

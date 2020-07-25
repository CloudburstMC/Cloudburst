package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Sign;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.AIR;

public class BlockBehaviorSignPost extends BlockBehaviorTransparent {

    protected final Identifier signItemId;
    protected final Identifier signWallId;
    protected final Identifier signStandingId;

    protected BlockBehaviorSignPost(Identifier signStandingId, Identifier signWallId, Identifier signItemId) {
        this.signItemId = signItemId;
        this.signWallId = signWallId;
        this.signStandingId = signStandingId;
    }

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(Block block) {
        return null;
    }

    public static BlockFactory factory(Identifier signWallId, Identifier signItemId) {
        return signStandingId -> new BlockBehaviorSignPost(signStandingId, signWallId, signItemId);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (down().getId() == AIR) {
                getLevel().useBreakOn(this.getPosition());

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.DOWN) {
            if (face == Direction.UP) {
                setMeta((int) Math.floor(((player.getYaw() + 180) * 16 / 360) + 0.5) & 0x0f);
                getLevel().setBlock(blockState.getPosition(), BlockState.get(signStandingId, getMeta()), true);
            } else {
                setMeta(face.getIndex());
                getLevel().setBlock(blockState.getPosition(), BlockState.get(signWallId, getMeta()), true);
            }

            Sign sign = BlockEntityRegistry.get().newEntity(BlockEntityTypes.SIGN, this.getChunk(), this.getPosition());
            if (!item.hasNbtMap()) {
                sign.setTextOwner(player.getXuid());
            } else {
                sign.loadAdditionalData(item.getTag());
            }

            return true;
        }

        return false;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromIndex(this.getMeta() & 0x07);
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(signItemId);
    }

    public Identifier getSignItemId() {
        return signItemId;
    }

    public Identifier getSignWallId() {
        return signWallId;
    }

    public Identifier getSignStandingId() {
        return signStandingId;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

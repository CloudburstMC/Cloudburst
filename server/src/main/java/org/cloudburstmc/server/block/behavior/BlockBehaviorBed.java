package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.blockentity.Bed;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.DyeColor;

public class BlockBehaviorBed extends BlockBehaviorTransparent {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public float getResistance() {
        return 1;
    }

    @Override
    public float getHardness() {
        return 0.2f;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        BlockState state = block.getState();

        BlockState head = null;

        if (state.ensureTrait(BlockTraits.IS_HEAD_PIECE)) {
            head = state;
        } else {
            for (Direction face : Plane.HORIZONTAL) {
                BlockState side = block.getSide(face).getState();

                if (side.getType() == BlockTypes.BED && side.ensureTrait(BlockTraits.IS_HEAD_PIECE)) {
                    head = side;
                    break;
                }
            }
        }

        if (head == null) {
            if (player != null) {
                player.sendMessage(new TranslationContainer("tile.bed.notValid"));
            }

            return true;
        }

        if (player != null) {
            Location spawn = Location.from(block.getPosition().toFloat().add(0.5, 0.5, 0.5), player.getYaw(), player.getPitch(), player.getLevel());

            if (!player.getSpawn().equals(spawn)) {
                player.setSpawn(spawn);
                player.sendMessage(new TranslationContainer("tile.bed.respawnSet"));
            }
        }

        int time = block.getLevel().getTime() % Level.TIME_FULL;

        boolean isNight = (time >= Level.TIME_NIGHT && time < Level.TIME_SUNRISE);

        if (player != null && !isNight) {
            player.sendMessage(new TranslationContainer("tile.bed.noSleep"));
            return true;
        }

        if (player != null && !player.sleepOn(block.getPosition())) {
            player.sendMessage(new TranslationContainer("tile.bed.occupied"));
        }


        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        BlockState down = block.down().getState();
        BlockRegistry registry = BlockRegistry.get();

        if (!registry.inCategory(down.getType(), BlockCategory.TRANSPARENT)) {
            Block next = block.getSide(player.getHorizontalFacing());
            BlockBehavior nextBehavior = next.getState().getBehavior();

            BlockState downNext = next.down().getState();

            if (nextBehavior.canBeReplaced() && !registry.inCategory(downNext.getType(), BlockCategory.TRANSPARENT)) {
                BlockState bed = registry.getBlock(BlockTypes.BED)
                        .withTrait(BlockTraits.DIRECTION, player.getDirection());

                block.getLevel().setBlock(block.getPosition(), bed, true);

                if (nextBehavior instanceof BlockBehaviorLiquid && ((BlockBehaviorLiquid) nextBehavior).usesWaterLogging()) {
                    block.getLevel().setBlock(next.getPosition(), 1, next.getState(), true, false);
                }

                block.getLevel().setBlock(next.getPosition(), bed.withTrait(BlockTraits.IS_HEAD_PIECE, true), true);

                createBlockEntity(block, item.getMeta());
                createBlockEntity(next, item.getMeta());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        BlockState state = block.getState();
        boolean head = state.ensureTrait(BlockTraits.IS_HEAD_PIECE);
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);

        Block otherPart = null;
        for (Direction direction : Plane.HORIZONTAL) {
            Block side = block.getSide(direction);
            BlockState face = side.getState();

            if (face.getType() == BlockTypes.BED && face.ensureTrait(BlockTraits.IS_HEAD_PIECE) != head && face.ensureTrait(BlockTraits.DIRECTION) == facing) {
                otherPart = side;
                break;
            }
        }

        if (otherPart != null) {
            otherPart.getState().getBehavior().removeBlock(otherPart, false);
        }

        removeBlock(block, true);
        return true;
    }

    private void createBlockEntity(Block block, int color) {
        Bed bed = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BED, block.getChunk(), block.getPosition());
        bed.setColor(DyeColor.getByDyeData(color));
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.BED, this.getDyeColor(block).getWoolData());
    }

    @Override
    public BlockColor getColor(Block block) {
        return this.getDyeColor(block).getColor();
    }

    public DyeColor getDyeColor(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        if (blockEntity instanceof Bed) {
            return ((Bed) blockEntity).getColor();
        }

        return DyeColor.WHITE;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

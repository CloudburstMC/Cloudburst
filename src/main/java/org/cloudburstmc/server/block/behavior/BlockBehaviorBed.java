package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.Bed;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Faceable;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorBed extends BlockBehaviorTransparent implements Faceable {

    public BlockBehaviorBed(Identifier id) {
        super(id);
    }

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
    public float getMaxY() {
        return this.getY() + 0.5625f;
    }

    @Override
    public boolean onActivate(Item item) {
        return this.onActivate(item, null);
    }

    @Override
    public boolean onActivate(Item item, Player player) {

        BlockState blockStateNorth = this.north();
        BlockState blockStateSouth = this.south();
        BlockState blockStateEast = this.east();
        BlockState blockStateWest = this.west();

        BlockState b;
        if ((this.getMeta() & 0x08) == 0x08) {
            b = this;
        } else {
            if (blockStateNorth.getId() == this.getId() && (blockStateNorth.getMeta() & 0x08) == 0x08) {
                b = blockStateNorth;
            } else if (blockStateSouth.getId() == this.getId() && (blockStateSouth.getMeta() & 0x08) == 0x08) {
                b = blockStateSouth;
            } else if (blockStateEast.getId() == this.getId() && (blockStateEast.getMeta() & 0x08) == 0x08) {
                b = blockStateEast;
            } else if (blockStateWest.getId() == this.getId() && (blockStateWest.getMeta() & 0x08) == 0x08) {
                b = blockStateWest;
            } else {
                if (player != null) {
                    player.sendMessage(new TranslationContainer("tile.bed.notValid"));
                }

                return true;
            }
        }

        Location spawn = Location.from(b.getPosition().toFloat().add(0.5, 0.5, 0.5), this.level);
        if (player != null && !player.getSpawn().equals(spawn)) {
            player.setSpawn(spawn);
            player.sendMessage(new TranslationContainer("tile.bed.respawnSet"));
        }

        int time = this.getLevel().getTime() % Level.TIME_FULL;

        boolean isNight = (time >= Level.TIME_NIGHT && time < Level.TIME_SUNRISE);

        if (player != null && !isNight) {
            player.sendMessage(new TranslationContainer("tile.bed.noSleep"));
            return true;
        }

        if (player != null && !player.sleepOn(b.getPosition())) {
            player.sendMessage(new TranslationContainer("tile.bed.occupied"));
        }


        return true;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        BlockState down = this.down();
        if (!down.isTransparent() || down instanceof BlockBehaviorSlab) {
            BlockState next = this.getSide(player.getHorizontalFacing());
            BlockState downNext = next.down();

            if (next.canBeReplaced() && (!downNext.isTransparent() || downNext instanceof BlockBehaviorSlab)) {
                int meta = player.getDirection().getHorizontalIndex();

                this.getLevel().setBlock(blockState.getPosition(), BlockState.get(this.getId(), meta), true, true);
                if (next instanceof BlockBehaviorLiquid && ((BlockBehaviorLiquid) next).usesWaterLogging()) {
                    this.getLevel().setBlock(next.getPosition(), 1, next.clone(), true, false);
                }
                this.getLevel().setBlock(next.getPosition(), BlockState.get(this.getId(), meta | 0x08), true, true);

                createBlockEntity(this.getPosition(), item.getMeta());
                createBlockEntity(next.getPosition(), item.getMeta());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onBreak(Item item) {
        BlockState blockStateNorth = this.north(); //Gets the blocks around them
        BlockState blockStateSouth = this.south();
        BlockState blockStateEast = this.east();
        BlockState blockStateWest = this.west();

        BlockState air = BlockState.AIR;
        BlockState otherPart = null;
        if ((this.getMeta() & 0x08) == 0x08) { //This is the Top part of bed
            if (blockStateNorth.getId() == BlockTypes.BED && (blockStateNorth.getMeta() & 0x08) != 0x08) { //Checks if the block ID&&meta are right
                otherPart = blockStateNorth;
            } else if (blockStateSouth.getId() == BlockTypes.BED && (blockStateSouth.getMeta() & 0x08) != 0x08) {
                otherPart = blockStateSouth;
            } else if (blockStateEast.getId() == BlockTypes.BED && (blockStateEast.getMeta() & 0x08) != 0x08) {
                otherPart = blockStateEast;
            } else if (blockStateWest.getId() == BlockTypes.BED && (blockStateWest.getMeta() & 0x08) != 0x08) {
                otherPart = blockStateWest;
            }
        } else { //Bottom Part of Bed
            if (blockStateNorth.getId() == this.getId() && (blockStateNorth.getMeta() & 0x08) == 0x08) {
                otherPart = blockStateNorth;
            } else if (blockStateSouth.getId() == this.getId() && (blockStateSouth.getMeta() & 0x08) == 0x08) {
                otherPart = blockStateSouth;
            } else if (blockStateEast.getId() == this.getId() && (blockStateEast.getMeta() & 0x08) == 0x08) {
                otherPart = blockStateEast;
            } else if (blockStateWest.getId() == this.getId() && (blockStateWest.getMeta() & 0x08) == 0x08) {
                otherPart = blockStateWest;
            }
        }

        if (otherPart instanceof BlockBehaviorBed) {
            otherPart.removeBlock(false); // Do not update both parts to prevent duplication bug if there is two fallable blocks top of the bed
        }

        removeBlock(true);
        return true;
    }

    private void createBlockEntity(Vector3i pos, int color) {
        Bed bed = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BED, this.level.getChunk(pos), pos);
        bed.setColor(DyeColor.getByDyeData(color));
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.BED, this.getDyeColor().getWoolData());
    }

    @Override
    public BlockColor getColor() {
        return this.getDyeColor().getColor();
    }

    public DyeColor getDyeColor() {
        if (this.level != null) {
            BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

            if (blockEntity instanceof Bed) {
                return ((Bed) blockEntity).getColor();
            }
        }

        return DyeColor.WHITE;
    }

    @Override
    public BlockFace getBlockFace() {
        return BlockFace.fromHorizontalIndex(this.getMeta() & 0x7);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Comparator;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.POWERED_COMPARATOR;
import static org.cloudburstmc.server.block.BlockTypes.UNPOWERED_COMPARATOR;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.COMPARATOR;

public abstract class BlockBehaviorRedstoneComparator extends BlockBehaviorRedstoneDiode {

    @Override
    protected int getDelay() {
        return 2;
    }

    @Override
    public Direction getFacing() {
        return Direction.fromHorizontalIndex(this.getMeta());
    }

    public Mode getMode() {
        return (getMeta() & 4) > 0 ? Mode.SUBTRACT : Mode.COMPARE;
    }

    @Override
    protected BlockState getUnpowered() {
        return BlockState.get(UNPOWERED_COMPARATOR, this.getMeta());
    }

    @Override
    protected BlockState getPowered() {
        return BlockState.get(POWERED_COMPARATOR, this.getMeta());
    }

    @Override
    protected int getRedstoneSignal() {
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        return blockEntity instanceof Comparator ? ((Comparator) blockEntity).getOutputSignal() : 0;
    }

    @Override
    public void updateState() {
        if (!this.level.isBlockTickPending(this.getPosition(), this)) {
            int output = this.calculateOutput();
            BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());
            int power = blockEntity instanceof Comparator ? ((Comparator) blockEntity).getOutputSignal() : 0;

            if (output != power || this.isPowered() != this.shouldBePowered()) {
                /*if(isFacingTowardsRepeater()) {
                    this.level.scheduleUpdate(this, this, 2, -1);
                } else {
                    this.level.scheduleUpdate(this, this, 2, 0);
                }*/

                //System.out.println("schedule update 0");
                this.level.scheduleUpdate(this, this.getPosition(), 2);
            }
        }
    }

    protected int calculateInputStrength() {
        int power = super.calculateInputStrength();
        Direction face = getFacing();
        BlockState blockState = this.getSide(face);

        if (blockState.hasComparatorInputOverride()) {
            power = blockState.getComparatorInputOverride();
        } else if (power < 15 && blockState.isNormalBlock()) {
            blockState = blockState.getSide(face);

            if (blockState.hasComparatorInputOverride()) {
                power = blockState.getComparatorInputOverride();
            }
        }

        return power;
    }

    protected boolean shouldBePowered() {
        int input = this.calculateInputStrength();

        if (input >= 15) {
            return true;
        } else if (input == 0) {
            return false;
        } else {
            int sidePower = this.getPowerOnSides();
            return sidePower == 0 || input >= sidePower;
        }
    }

    private int calculateOutput() {
        return getMode() == Mode.SUBTRACT ? Math.max(this.calculateInputStrength() - this.getPowerOnSides(), 0) : this.calculateInputStrength();
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (getMode() == Mode.SUBTRACT) {
            this.setMeta(this.getMeta() - 4);
        } else {
            this.setMeta(this.getMeta() + 4);
        }

        this.level.addSound(this.getPosition(), Sound.RANDOM_CLICK, 1, getMode() == Mode.SUBTRACT ? 0.55F : 0.5F);
        this.level.setBlock(this.getPosition(), this, true, false);
        //bug?

        this.onChange();
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            this.onChange();
            return type;
        }

        return super.onUpdate(block, type);
    }

    private void onChange() {
        int output = this.calculateOutput();
        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());
        int currentOutput = 0;

        if (blockEntity instanceof Comparator) {
            Comparator blockEntityComparator = (Comparator) blockEntity;
            currentOutput = blockEntityComparator.getOutputSignal();
            blockEntityComparator.setOutputSignal(output);
        }

        if (currentOutput != output || getMode() == Mode.COMPARE) {
            boolean shouldBePowered = this.shouldBePowered();
            boolean isPowered = this.isPowered();

            if (isPowered && !shouldBePowered) {
                this.level.setBlock(this.getPosition(), getUnpowered(), true, false);
            } else if (!isPowered && shouldBePowered) {
                this.level.setBlock(this.getPosition(), getPowered(), true, false);
            }

            this.level.updateAroundRedstone(this.getPosition(), null);
        }
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (super.place(item, block, target, face, clickPos, player)) {
            BlockEntityRegistry.get().newEntity(COMPARATOR, this.getChunk(), this.getPosition());

            this.onUpdate(Level.BLOCK_UPDATE_REDSTONE);
            return true;
        }

        return false;
    }

    @Override
    public boolean isPowered() {
        return this.isPowered || (this.getMeta() & 8) > 0;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(ItemIds.COMPARATOR);
    }

    public enum Mode {
        COMPARE,
        SUBTRACT
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}

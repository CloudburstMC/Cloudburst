package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Comparator;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.COMPARATOR;

public class BlockBehaviorRedstoneComparator extends BlockBehaviorRedstoneDiode {

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    public Mode getMode(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT) ? Mode.SUBTRACT : Mode.COMPARE;
    }

    @Override
    protected int getRedstoneSignal(Block block) {
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());

        return blockEntity instanceof Comparator ? ((Comparator) blockEntity).getOutputSignal() : 0;
    }

    @Override
    public void updateState(Block block) {
        val state = block.getState();
        if (!block.getLevel().isBlockTickPending(block.getPosition(), block)) {
            int output = this.calculateOutput(block);
            BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
            int power = blockEntity instanceof Comparator ? ((Comparator) blockEntity).getOutputSignal() : 0;

            if (output != power || this.isPowered(state) != this.shouldBePowered(block)) {
                /*if(isFacingTowardsRepeater()) {
                    this.level.scheduleUpdate(this, this, 2, -1);
                } else {
                    this.level.scheduleUpdate(this, this, 2, 0);
                }*/

                //System.out.println("schedule update 0");
                block.getLevel().scheduleUpdate(block, block.getPosition(), 2);
            }
        }
    }

    protected int calculateInputStrength(Block block) {
        int power = super.calculateInputStrength(block);
        Direction face = getFacing(block.getState());
        Block b = block.getSide(face);
        val behavior = b.getState().getBehavior();

        if (behavior.hasComparatorInputOverride()) {
            power = behavior.getComparatorInputOverride(b);
        } else if (power < 15 && behavior.isNormalBlock(b)) {
            b = b.getSide(face);

            if (behavior.hasComparatorInputOverride()) {
                power = behavior.getComparatorInputOverride(b);
            }
        }

        return power;
    }

    protected boolean shouldBePowered(Block block) {
        int input = this.calculateInputStrength(block);

        if (input >= 15) {
            return true;
        } else if (input == 0) {
            return false;
        } else {
            int sidePower = this.getPowerOnSides(block);
            return sidePower == 0 || input >= sidePower;
        }
    }

    private int calculateOutput(Block block) {
        return getMode(block.getState()) == Mode.SUBTRACT ? Math.max(this.calculateInputStrength(block) - this.getPowerOnSides(block), 0) : this.calculateInputStrength(block);
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        val state = block.getState();
        boolean subtract = state.ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT);
        block.set(state.withTrait(BlockTraits.IS_OUTPUT_SUBTRACT, !subtract), true);
        block.getLevel().addSound(block.getPosition(), Sound.RANDOM_CLICK, 1, subtract ? 0.5f : 0.55F);
        //bug?

        this.onChange(block.refresh());
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            this.onChange(block);
            return type;
        }

        return super.onUpdate(block, type);
    }

    private void onChange(Block block) {
        int output = this.calculateOutput(block);
        BlockEntity blockEntity = block.getLevel().getBlockEntity(block.getPosition());
        int currentOutput = 0;

        if (blockEntity instanceof Comparator) {
            Comparator blockEntityComparator = (Comparator) blockEntity;
            currentOutput = blockEntityComparator.getOutputSignal();
            blockEntityComparator.setOutputSignal(output);
        }

        val state = block.getState();
        if (currentOutput != output || getMode(state) == Mode.COMPARE) {
            boolean shouldBePowered = this.shouldBePowered(block);
            boolean isPowered = this.isPowered(state);

            if (isPowered && !shouldBePowered) {
                block.set(getUnpowered(state), true, false);
            } else if (!isPowered && shouldBePowered) {
                block.set(getPowered(state), true, false);
            }

            block.getLevel().updateAroundRedstone(block.getPosition(), null);
        }
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (super.place(item, block, target, face, clickPos, player)) {
            BlockEntityRegistry.get().newEntity(COMPARATOR, block);

            this.onUpdate(block.refresh(), Level.BLOCK_UPDATE_REDSTONE);
            return true;
        }

        return false;
    }

    @Override
    public boolean isPowered(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_POWERED) || state.ensureTrait(BlockTraits.IS_OUTPUT_LIT);
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.COMPARATOR);
    }

    public enum Mode {
        COMPARE,
        SUBTRACT
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}

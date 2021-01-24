package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Comparator;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockIds.POWERED_COMPARATOR;
import static org.cloudburstmc.server.block.BlockIds.UNPOWERED_COMPARATOR;
import static org.cloudburstmc.server.blockentity.BlockEntityTypes.COMPARATOR;

public class BlockBehaviorRedstoneComparator extends BlockBehaviorRedstoneDiode {

    public BlockBehaviorRedstoneComparator(Identifier type) {
        super(type);
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    public Mode getMode(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT) ? Mode.SUBTRACT : Mode.COMPARE;
    }

    @Override
    protected BlockState getUnpowered(BlockState state) {
        return BlockState.get(UNPOWERED_COMPARATOR).copyTraits(state);
    }

    @Override
    protected BlockState getPowered(BlockState state) {
        return BlockState.get(POWERED_COMPARATOR).copyTraits(state);
    }

    @Override
    protected int getRedstoneSignal(Block block) {
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());

        return blockEntity instanceof Comparator ? ((Comparator) blockEntity).getOutputSignal() : 0;
    }

    @Override
    public void updateState(Block block) {
        val state = block.getState();
        if (!block.getWorld().isBlockTickPending(block.getPosition(), block)) {
            int output = this.calculateOutput(block);
            BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
            int power = blockEntity instanceof Comparator ? ((Comparator) blockEntity).getOutputSignal() : 0;

            if (output != power || this.isPowered(state) != this.shouldBePowered(block)) {
                /*if(isFacingTowardsRepeater()) {
                    this.world.scheduleUpdate(this, this, 2, -1);
                } else {
                    this.world.scheduleUpdate(this, this, 2, 0);
                }*/

                //System.out.println("schedule update 0");
                block.getWorld().scheduleUpdate(block, block.getPosition(), 2);
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
    public boolean onActivate(Block block, Item item, Player player) {
        val state = block.getState();
        boolean subtract = state.ensureTrait(BlockTraits.IS_OUTPUT_SUBTRACT);
        block.set(state.withTrait(BlockTraits.IS_OUTPUT_SUBTRACT, !subtract), true);
        block.getWorld().addSound(block.getPosition(), Sound.RANDOM_CLICK, 1, subtract ? 0.5f : 0.55F);
        //bug?

        this.onChange(block.refresh());
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_SCHEDULED) {
            this.onChange(block);
            return type;
        }

        return super.onUpdate(block, type);
    }

    private void onChange(Block block) {
        int output = this.calculateOutput(block);
        BlockEntity blockEntity = block.getWorld().getBlockEntity(block.getPosition());
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

            block.getWorld().updateAroundRedstone(block.getPosition(), null);
        }
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (super.place(item, block, target, face, clickPos, player)) {
            BlockEntityRegistry.get().newEntity(COMPARATOR, block);

            this.onUpdate(block.refresh(), World.BLOCK_UPDATE_REDSTONE);
            return true;
        }

        return false;
    }

    @Override
    public boolean isPowered(BlockState state) {
        return this.isPowered || state.ensureTrait(BlockTraits.IS_OUTPUT_LIT);
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(ItemIds.COMPARATOR);
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

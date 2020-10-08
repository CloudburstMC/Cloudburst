package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.val;
import lombok.var;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorTripWireHook extends FloodableBlockBehavior {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (!_isNormalBlock(block.getSide(block.getState().ensureTrait(BlockTraits.DIRECTION).getOpposite()))) {
                block.getLevel().useBreakOn(block.getPosition());
            }

            return type;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            this.calculateState(block, false, true, -1, null);
            return type;
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!_isNormalBlock(block.getSide(face.getOpposite())) || face.getAxis().isVertical()) {
            return false;
        }

        placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.DIRECTION, face));

        if (player != null) {
            this.calculateState(block, false, false, -1, null);
        }
        return true;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        super.onBreak(block, item);
        val state = block.getState();
        boolean attached = isAttached(state);
        boolean powered = isPowered(state);

        if (attached || powered) {
            this.calculateState(block, true, false, -1, null);
        }

        if (powered) {
            block.getLevel().updateAroundRedstone(block.getPosition(), null);
            block.getLevel().updateAroundRedstone(state.ensureTrait(BlockTraits.DIRECTION).getOpposite().getOffset(block.getPosition()), null);
        }

        return true;
    }

    public void calculateState(Block block, boolean onBreak, boolean updateAround, int pos, BlockState blockState) {
        val state = block.getState();
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        Vector3i v = block.getPosition();
        val level = block.getLevel();

        boolean attached = isAttached(state);
        boolean powered = isPowered(state);
        boolean canConnect = !onBreak;
        boolean nextPowered = false;
        int distance = 0;
        BlockState[] blockStates = new BlockState[42];

        for (int i = 1; i < 42; ++i) {
            Vector3i vector = v.add(facing.getUnitVector().mul(i));
            BlockState b = level.getBlockAt(vector);

            if (b.getType() == TRIPWIRE_HOOK) {
                if (b.ensureTrait(BlockTraits.DIRECTION) == facing.getOpposite()) {
                    distance = i;
                }
                break;
            }

            if (b.getType() != TRIPWIRE && i != pos) {
                blockStates[i] = null;
                canConnect = false;
            } else {
                if (i == pos) {
                    b = blockState != null ? blockState : b;
                }

                if (b.getType() == TRIPWIRE) {
                    val behavior = (BlockBehaviorTripWire) b.getBehavior();

                    boolean disarmed = !behavior.isDisarmed(b);
                    boolean wirePowered = behavior.isPowered(b);
                    nextPowered |= disarmed && wirePowered;

                    if (i == pos) {
                        level.scheduleUpdate(block, 10);
                        canConnect &= disarmed;
                    }
                }
                blockStates[i] = b;
            }
        }

        canConnect = canConnect & distance > 1;
        nextPowered = nextPowered & canConnect;

        var hook = BlockState.get(TRIPWIRE_HOOK)
                .withTrait(BlockTraits.IS_ATTACHED, canConnect)
                .withTrait(BlockTraits.IS_POWERED, nextPowered);


        if (distance > 0) {
            Vector3i vec = v.add(facing.getUnitVector().mul(distance));
            Direction face = facing.getOpposite();

            level.setBlock(vec, hook.withTrait(BlockTraits.DIRECTION, face), true, false);
            level.updateAroundRedstone(vec, null);
            level.updateAroundRedstone(face.getOpposite().getOffset(vec), null);
            this.addSound(block, vec.toFloat(), canConnect, nextPowered, attached, powered);
        }

        this.addSound(block, v.toFloat(), canConnect, nextPowered, attached, powered);

        if (!onBreak) {
            level.setBlock(v, hook.withTrait(BlockTraits.DIRECTION, facing), true, false);

            if (updateAround) {
                level.updateAroundRedstone(v, null);
                level.updateAroundRedstone(facing.getOpposite().getOffset(v), null);
            }
        }

        if (attached != canConnect) {
            for (int i = 1; i < distance; i++) {
                Vector3i vc = v.add(facing.getUnitVector().mul(i));
                blockState = blockStates[i];

                if (blockState != null && level.getBlockAt(vc).getType() != AIR) {
                    level.setBlock(vc, blockState.withTrait(BlockTraits.IS_ATTACHED, canConnect), true, false);
                }
            }
        }
    }

    private void addSound(Block block, Vector3f pos, boolean canConnect, boolean nextPowered, boolean attached, boolean powered) {
        val level = block.getLevel();
        if (nextPowered && !powered) {
            level.addLevelSoundEvent(pos, SoundEvent.POWER_ON);
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));
        } else if (!nextPowered && powered) {
            level.addLevelSoundEvent(pos, SoundEvent.POWER_OFF);
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));
        } else if (canConnect && !attached) {
            level.addLevelSoundEvent(pos, SoundEvent.ATTACH);
        } else if (!canConnect && attached) {
            level.addLevelSoundEvent(pos, SoundEvent.DETACH);
        }
    }

    public boolean isAttached(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_ATTACHED);
    }

    public boolean isPowered(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_POWERED);
    }


    @Override
    public int getWeakPower(Block block, Direction face) {
        return isPowered(block.getState()) ? 15 : 0;
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        val state = block.getState();
        return !isPowered(state) ? 0 : state.ensureTrait(BlockTraits.DIRECTION) == side ? 15 : 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }


    private boolean _isNormalBlock(Block block) {
        return block.getState().getBehavior().isNormalBlock(block);
    }
}

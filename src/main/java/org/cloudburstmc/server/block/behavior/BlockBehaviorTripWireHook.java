package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.event.block.BlockRedstoneEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.*;

public class BlockBehaviorTripWireHook extends FloodableBlockBehavior {

    @Override
    public int onUpdate(Block block, int type) {
        if (type == CloudLevel.BLOCK_UPDATE_NORMAL) {
            if (!_isNormalBlock(block.getSide(block.getState().ensureTrait(BlockTraits.DIRECTION).getOpposite()))) {
                block.getLevel().useBreakOn(block.getPosition());
            }

            return type;
        } else if (type == CloudLevel.BLOCK_UPDATE_SCHEDULED) {
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
        var state = block.getState();
        boolean attached = isAttached(state);
        boolean powered = isPowered(state);

        if (attached || powered) {
            this.calculateState(block, true, false, -1, null);
        }

        if (powered) {
            ((CloudLevel) block.getLevel()).updateAroundRedstone(block.getPosition(), null);
            ((CloudLevel) block.getLevel()).updateAroundRedstone(state.ensureTrait(BlockTraits.DIRECTION).getOpposite().getOffset(block.getPosition()), null);
        }

        return true;
    }

    public void calculateState(Block block, boolean onBreak, boolean updateAround, int pos, BlockState blockState) {
        var state = block.getState();
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        Vector3i v = block.getPosition();
        var level = block.getLevel();

        boolean attached = isAttached(state);
        boolean powered = isPowered(state);
        boolean canConnect = !onBreak;
        boolean nextPowered = false;
        int distance = 0;
        BlockState[] blockStates = new BlockState[42];

        for (int i = 1; i < 42; ++i) {
            Vector3i vector = v.add(facing.getUnitVector().mul(i));
            BlockState b = level.getBlockState(vector);

            if (b.getType() == TRIPWIRE_HOOK) {
                if (b.ensureTrait(BlockTraits.DIRECTION) == facing.getOpposite()) {
                    distance = i;
                }
                break;
            }

            if (b.getType() != TRIP_WIRE && i != pos) {
                blockStates[i] = null;
                canConnect = false;
            } else {
                if (i == pos) {
                    b = blockState != null ? blockState : b;
                }

                if (b.getType() == TRIP_WIRE) {
                    var behavior = (BlockBehaviorTripWire) b.getBehavior();

                    boolean disarmed = !behavior.isDisarmed(b);
                    boolean wirePowered = behavior.isPowered(b);
                    nextPowered |= disarmed && wirePowered;

                    if (i == pos) {
                        level.scheduleUpdate(block.getPosition(), 10);
                        canConnect &= disarmed;
                    }
                }
                blockStates[i] = b;
            }
        }

        canConnect = canConnect & distance > 1;
        nextPowered = nextPowered & canConnect;

        var hook = CloudBlockRegistry.get().getBlock(TRIPWIRE_HOOK)
                .withTrait(BlockTraits.IS_ATTACHED, canConnect)
                .withTrait(BlockTraits.IS_POWERED, nextPowered);


        if (distance > 0) {
            Vector3i vec = v.add(facing.getUnitVector().mul(distance));
            Direction face = facing.getOpposite();

            level.setBlockState(vec, hook.withTrait(BlockTraits.DIRECTION, face), true, false);
            ((CloudLevel) level).updateAroundRedstone(vec, null);
            ((CloudLevel) level).updateAroundRedstone(face.getOpposite().getOffset(vec), null);
            this.addSound(block, vec.toFloat(), canConnect, nextPowered, attached, powered);
        }

        this.addSound(block, v.toFloat(), canConnect, nextPowered, attached, powered);

        if (!onBreak) {
            level.setBlockState(v, hook.withTrait(BlockTraits.DIRECTION, facing), true, false);

            if (updateAround) {
                ((CloudLevel) level).updateAroundRedstone(v, null);
                ((CloudLevel) level).updateAroundRedstone(facing.getOpposite().getOffset(v), null);
            }
        }

        if (attached != canConnect) {
            for (int i = 1; i < distance; i++) {
                Vector3i vc = v.add(facing.getUnitVector().mul(i));
                blockState = blockStates[i];

                if (blockState != null && level.getBlockState(vc).getType() != AIR) {
                    level.setBlockState(vc, blockState.withTrait(BlockTraits.IS_ATTACHED, canConnect), true, false);
                }
            }
        }
    }

    private void addSound(Block block, Vector3f pos, boolean canConnect, boolean nextPowered, boolean attached, boolean powered) {
        var level = block.getLevel();
        if (nextPowered && !powered) {
            ((CloudLevel) level).addLevelSoundEvent(pos, SoundEvent.POWER_ON);
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 0, 15));
        } else if (!nextPowered && powered) {
            ((CloudLevel) level).addLevelSoundEvent(pos, SoundEvent.POWER_OFF);
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, 15, 0));
        } else if (canConnect && !attached) {
            ((CloudLevel) level).addLevelSoundEvent(pos, SoundEvent.ATTACH);
        } else if (!canConnect && attached) {
            ((CloudLevel) level).addLevelSoundEvent(pos, SoundEvent.DETACH);
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
        var state = block.getState();
        return !isPowered(state) ? 0 : state.ensureTrait(BlockTraits.DIRECTION) == side ? 15 : 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(block.getState());
    }


    private boolean _isNormalBlock(Block block) {
        return block.getState().getBehavior().isNormalBlock(block);
    }
}

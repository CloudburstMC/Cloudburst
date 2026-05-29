package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import javax.annotation.Nullable;

@UtilityClass
public class TripwireCalculator {

    private static final int RECHECK_TICKS = 10;

    public static final int MAX_SCAN_DISTANCE = 41;

    public void calculateState(CloudLevel level, Vector3i hookPos, BlockState hookState, boolean hookBeingRemoved, boolean canUpdate, int wireSource, @Nullable BlockState wireSourceState) {
        Direction facing = hookState.ensureTrait(BlockTraits.DIRECTION);
        boolean wasAttached = hookState.ensureTrait(BlockTraits.IS_ATTACHED);
        boolean wasPowered = hookState.ensureTrait(BlockTraits.IS_POWERED);

        boolean attached = !hookBeingRemoved;
        boolean powered = false;
        boolean needsRecheck = false;

        int oppositeHookOffset = 0;
        BlockState[] wireStates = new BlockState[MAX_SCAN_DISTANCE + 1];

        for (int i = 1; i <= MAX_SCAN_DISTANCE; i++) {
            Vector3i scanPos = facing.relative(hookPos, i);
            BlockState scanState = level.getBlockState(scanPos.getX(), scanPos.getY(), scanPos.getZ());

            if (scanState.getType() == BlockTypes.TRIPWIRE_HOOK) {
                if (scanState.ensureTrait(BlockTraits.DIRECTION) == facing.getOpposite()) {
                    oppositeHookOffset = i;
                }
                break;
            }

            if (i == wireSource && wireSourceState != null) {
                scanState = wireSourceState;
            }

            if (scanState.getType() == BlockTypes.TRIP_WIRE) {
                boolean wirePowered = scanState.ensureTrait(BlockTraits.IS_POWERED);
                boolean wireDisarmed = scanState.ensureTrait(BlockTraits.IS_DISARMED);
                powered |= wirePowered && !wireDisarmed;
                wireStates[i] = scanState;

                if (i == wireSource) {
                    needsRecheck = true;
                    attached &= !wireDisarmed;
                }
            } else {
                wireStates[i] = null;
                attached = false;
            }
        }

        attached = attached && oppositeHookOffset > 1;
        powered = powered && attached;

        BlockState newHookState = hookState
                .withTrait(BlockTraits.IS_ATTACHED, attached)
                .withTrait(BlockTraits.IS_POWERED, powered);

        if (oppositeHookOffset > 0) {
            Vector3i oppositePos = facing.relative(hookPos, oppositeHookOffset);
            if (level.isChunkLoaded(oppositePos)) {
                BlockState oppositeOld = level.getBlockState(oppositePos.getX(), oppositePos.getY(), oppositePos.getZ());
                if (oppositeOld.getType() == BlockTypes.TRIPWIRE_HOOK) {
                    BlockState oppositeNew = oppositeOld
                            .withTrait(BlockTraits.IS_ATTACHED, attached)
                            .withTrait(BlockTraits.IS_POWERED, powered);
                    level.setBlockState(oppositePos, oppositeNew, true, true);
                    emitHookSounds(level, oppositePos, oppositeNew,
                            oppositeOld.ensureTrait(BlockTraits.IS_ATTACHED),
                            oppositeOld.ensureTrait(BlockTraits.IS_POWERED));
                    notifyRedstoneNeighbours(level, oppositePos, facing.getOpposite());
                }
            }
        }

        if (!hookBeingRemoved && level.getBlockState(hookPos.getX(), hookPos.getY(), hookPos.getZ()).getType() != BlockTypes.TRIPWIRE_HOOK) {
            onRemoved(newHookState, level, hookPos);
            return;
        }

        emitHookSounds(level, hookPos, newHookState, wasAttached, wasPowered);

        if (!hookBeingRemoved) {
            level.setBlockState(hookPos, newHookState, true, true);
            if (needsRecheck) {
                level.scheduleUpdate(hookPos, RECHECK_TICKS);
            }
            if (canUpdate) {
                notifyRedstoneNeighbours(level, hookPos, facing);
            }
        }

        if (wasAttached != attached) {
            for (int i = 1; i < oppositeHookOffset; i++) {
                if (wireStates[i] != null) {
                    Vector3i wirePos = facing.relative(hookPos, i);
                    BlockState current = level.getBlockState(wirePos.getX(), wirePos.getY(), wirePos.getZ());
                    if (current.getType() == BlockTypes.TRIP_WIRE) {
                        level.setBlockState(wirePos, wireStates[i].withTrait(BlockTraits.IS_ATTACHED, attached), true, true);
                    }
                }
            }
        }
    }

    public void calculateState(CloudLevel level, Vector3i hookPos, BlockState hookState, boolean hookBeingRemoved, boolean canUpdate) {
        calculateState(level, hookPos, hookState, hookBeingRemoved, canUpdate, -1, null);
    }

    public void calculateState(CloudLevel level, Vector3i hookPos, BlockState hookState, boolean hookBeingRemoved) {
        calculateState(level, hookPos, hookState, hookBeingRemoved, false, -1, null);
    }

    public void notifyHooksAround(CloudLevel level, Vector3i wirePos, @Nullable BlockState wireSourceState) {
        for (Direction dir : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i <= MAX_SCAN_DISTANCE; i++) {
                Vector3i scanPos = dir.relative(wirePos, i);
                BlockState scanState = level.getBlockState(scanPos.getX(), scanPos.getY(), scanPos.getZ());

                if (scanState.getType() == BlockTypes.TRIPWIRE_HOOK) {
                    if (scanState.ensureTrait(BlockTraits.DIRECTION) == dir.getOpposite() && level.isChunkLoaded(scanPos)) {
                        calculateState(level, scanPos, scanState, false, true, i, wireSourceState);
                    }
                    break;
                }

                if (scanState.getType() != BlockTypes.TRIP_WIRE) {
                    break;
                }
            }
        }
    }

    public void notifyHooksAround(CloudLevel level, Vector3i wirePos) {
        notifyHooksAround(level, wirePos, null);
    }

    private void onRemoved(BlockState state, CloudLevel level, Vector3i pos) {
        boolean attached = state.ensureTrait(BlockTraits.IS_ATTACHED);
        boolean powered = state.ensureTrait(BlockTraits.IS_POWERED);
        if (attached || powered) {
            calculateState(level, pos, state, true);
        }
        if (powered) {
            notifyRedstoneNeighbours(level, pos, state.ensureTrait(BlockTraits.DIRECTION));
        }
    }

    private void emitHookSounds(CloudLevel level, Vector3i pos, BlockState newState, boolean wasAttached, boolean wasPowered) {
        boolean nowAttached = newState.ensureTrait(BlockTraits.IS_ATTACHED);
        boolean nowPowered = newState.ensureTrait(BlockTraits.IS_POWERED);
        int soundData = CloudBlockRegistry.REGISTRY.getRuntimeId(newState);

        if (nowPowered && !wasPowered) {
            level.addLevelSoundEvent(pos, SoundEvent.PRESSURE_PLATE_CLICK_ON, soundData);
        } else if (!nowPowered && wasPowered) {
            level.addLevelSoundEvent(pos, SoundEvent.PRESSURE_PLATE_CLICK_OFF, soundData);
        } else if (nowAttached && !wasAttached) {
            level.addLevelSoundEvent(pos, SoundEvent.ATTACH, soundData);
        } else if (!nowAttached && wasAttached) {
            level.addLevelSoundEvent(pos, SoundEvent.DETACH, soundData);
        }
    }

    public void notifyRedstoneNeighbours(CloudLevel level, Vector3i hookPos, Direction hookFacing) {
        level.updateAround(hookPos);
        level.updateAround(hookFacing.getOpposite().relative(hookPos));
    }
}

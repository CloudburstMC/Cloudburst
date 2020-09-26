package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.redstone.RedstoneUpdateEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.cloudburstmc.server.block.BlockTypes.GLOWSTONE;
import static org.cloudburstmc.server.block.BlockTypes.REDSTONE_WIRE;

public class BlockBehaviorRedstoneWire extends FloodableBlockBehavior {

    private boolean canProvidePower = true;
    private final Set<Vector3f> blocksNeedingUpdate = new HashSet<>();

    protected static boolean canConnectUpwardsTo(Level level, Vector3i pos) {
        return canConnectTo(level.getBlock(pos), null);
    }

    protected static boolean canConnectTo(Block block, Direction side) {
        val state = block.getState();
        if (state.getType() == REDSTONE_WIRE) {
            return true;
        } else if (BlockBehaviorRedstoneDiode.isDiode(state.getBehavior())) {
            Direction face = ((BlockBehaviorRedstoneDiode) state.getBehavior()).getFacing(state);
            return face == side || face.getOpposite() == side;
        } else {
            return state.getBehavior().isPowerSource(block) && side != null;
        }
    }

    private void updateSurroundingRedstone(Block block, boolean force) {
        this.calculateCurrentChanges(block, force);
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face != Direction.UP || !canBePlacedOn(target.getState())) {
            return false;
        }

        placeBlock(block, item.getBlock());

        this.updateSurroundingRedstone(block.refresh(), true);
        val level = block.getLevel();
        Vector3i pos = block.getPosition();

        for (Direction direction : Direction.Plane.VERTICAL) {
            level.updateAroundRedstone(direction.getOffset(pos), direction.getOpposite());
        }

        for (Direction direction : Direction.Plane.VERTICAL) {
            this.updateAround(level, direction.getOffset(pos), direction.getOpposite());
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i v = direction.getOffset(pos);

            if (_isNormalBlock(level.getBlock(v))) {
                this.updateAround(level, v.up(), Direction.DOWN);
            } else {
                this.updateAround(level, v.down(), Direction.UP);
            }
        }
        return true;
    }

    private void calculateCurrentChanges(Block block, boolean force) {
        Vector3i pos = block.getPosition();
        val level = block.getLevel();

        int meta = block.getState().ensureTrait(BlockTraits.REDSTONE_SIGNAL);
        int maxStrength = meta;
        this.canProvidePower = false;
        int power = this.getIndirectPower(block);

        this.canProvidePower = true;

        if (power > 0 && power > maxStrength - 1) {
            maxStrength = power;
        }

        int strength = 0;

        for (Direction face : Direction.Plane.HORIZONTAL) {
            Vector3i v = face.getOffset(pos);

            if (v.getX() == block.getX() && v.getZ() == block.getZ()) {
                continue;
            }


            strength = this.getMaxCurrentStrength(level, v, strength);

            boolean vNormal = _isNormalBlock(level.getBlock(v));

            if (vNormal && !_isNormalBlock(block.up())) {
                strength = this.getMaxCurrentStrength(level, v.up(), strength);
            } else if (!vNormal) {
                strength = this.getMaxCurrentStrength(level, v.down(), strength);
            }
        }

        if (strength > maxStrength) {
            maxStrength = strength - 1;
        } else if (maxStrength > 0) {
            --maxStrength;
        } else {
            maxStrength = 0;
        }

        if (power > maxStrength - 1) {
            maxStrength = power;
        } else if (power < maxStrength && strength <= maxStrength) {
            maxStrength = Math.max(power, strength - 1);
        }

        if (meta != maxStrength) {
            level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, meta, maxStrength));

            block.set(block.getState().withTrait(BlockTraits.REDSTONE_SIGNAL, maxStrength), false, false);

            level.updateAroundRedstone(block.getPosition(), null);
            for (Direction face : Direction.values()) {
                level.updateAroundRedstone(face.getOffset(pos), face.getOpposite());
            }
        } else if (force) {
            for (Direction face : Direction.values()) {
                level.updateAroundRedstone(face.getOffset(pos), face.getOpposite());
            }
        }
    }

    private int getMaxCurrentStrength(Level level, Vector3i pos, int maxStrength) {
        val state = level.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        if (state.getType() != REDSTONE_WIRE) {
            return maxStrength;
        } else {
            int strength = state.ensureTrait(BlockTraits.REDSTONE_SIGNAL);
            return Math.max(strength, maxStrength);
        }
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        removeBlock(block);

        val level = block.getLevel();
        Vector3i pos = block.getPosition();

        this.updateSurroundingRedstone(block, false);

        for (Direction direction : Direction.values()) {
            level.updateAroundRedstone(direction.getOffset(pos), null);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vector3i v = direction.getOffset(pos);

            if (_isNormalBlock(level.getBlock(v))) {
                this.updateAround(level, v.up(), Direction.DOWN);
            } else {
                this.updateAround(level, v.down(), Direction.UP);
            }
        }
        return true;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    private void updateAround(Level level, Vector3i pos, Direction face) {
        if (level.getBlockAt(pos).getType() == REDSTONE_WIRE) {
            level.updateAroundRedstone(pos, face);

            for (Direction side : Direction.values()) {
                level.updateAroundRedstone(side.getOffset(pos), side.getOpposite());
            }
        }
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.REDSTONE);
    }

    public int getStrongPower(Block block, Direction side) {
        return !this.canProvidePower ? 0 : getWeakPower(block, side);
    }

    public int getWeakPower(Block block, Direction side) {
        if (!this.canProvidePower) {
            return 0;
        } else {
            int power = block.getState().ensureTrait(BlockTraits.REDSTONE_SIGNAL);

            if (power == 0) {
                return 0;
            } else if (side == Direction.UP) {
                return power;
            } else {
                EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);

                for (Direction face : Direction.Plane.HORIZONTAL) {
                    if (this.isPowerSourceAt(block, face)) {
                        faces.add(face);
                    }
                }

                if (side.getAxis().isHorizontal() && faces.isEmpty()) {
                    return power;
                } else if (faces.contains(side) && !faces.contains(side.rotateYCCW()) && !faces.contains(side.rotateY())) {
                    return power;
                } else {
                    return 0;
                }
            }
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type != Level.BLOCK_UPDATE_NORMAL && type != Level.BLOCK_UPDATE_REDSTONE) {
            return 0;
        }
        val level = block.getLevel();
        // Redstone event
        RedstoneUpdateEvent ev = new RedstoneUpdateEvent(block);
        level.getServer().getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return 0;
        }

        if (type == Level.BLOCK_UPDATE_NORMAL && !this.canBePlacedOn(level.getBlockAt(block.getPosition().down()))) {
            level.useBreakOn(block.getPosition());
            return Level.BLOCK_UPDATE_NORMAL;
        }

        this.updateSurroundingRedstone(block, false);

        return Level.BLOCK_UPDATE_NORMAL;
    }

    private boolean isPowerSourceAt(Block block, Direction side) {
        val level = block.getLevel();
        Vector3i pos = block.getPosition();
        Vector3i v = side.getOffset(pos);
        Block b = level.getBlock(v);
        boolean flag = _isNormalBlock(b);
        boolean flag1 = _isNormalBlock(block.up());
        return !flag1 && flag && canConnectUpwardsTo(level, v.up()) || (canConnectTo(b, side) ||
                !flag && canConnectUpwardsTo(level, b.getPosition().down()));
    }

    public boolean canBePlacedOn(BlockState state) {
        return state.inCategory(BlockCategory.SOLID) && !state.inCategory(BlockCategory.TRANSPARENT) && state.getType() != GLOWSTONE;
    }

    @Override
    public boolean isPowerSource(Block block) {
        return this.canProvidePower;
    }

    private int getIndirectPower(Block block) {
        int power = 0;
        Vector3i pos = block.getPosition();

        for (Direction face : Direction.values()) {
            int blockPower = this.getIndirectPower(block.getLevel(), face.getOffset(pos), face);

            if (blockPower >= 15) {
                return 15;
            }

            if (blockPower > power) {
                power = blockPower;
            }
        }

        return power;
    }

    private int getIndirectPower(Level level, Vector3i pos, Direction face) {
        val block = level.getBlock(pos);
        val state = block.getState();
        if (state.getType() == REDSTONE_WIRE) {
            return 0;
        }
        return _isNormalBlock(block) ? getStrongPower(level, face.getOffset(pos), face) : state.getBehavior().getWeakPower(block, face);
    }

    private int getStrongPower(Level level, Vector3i pos, Direction direction) {
        Block block = level.getBlock(pos);
        val state = block.getState();

        if (state.getType() == REDSTONE_WIRE) {
            return 0;
        }

        return state.getBehavior().getStrongPower(block, direction);
    }

    private boolean _isNormalBlock(Block block) {
        return block.getState().getBehavior().isNormalBlock(block);
    }
}

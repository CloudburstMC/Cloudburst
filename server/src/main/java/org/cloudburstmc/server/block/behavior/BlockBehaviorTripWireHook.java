package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorTripWireHook extends FloodableBlockBehavior {

    public Direction getFacing() {
        return Direction.fromHorizontalIndex(getMeta() & 0b11);
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (!this.getSide(this.getFacing().getOpposite()).isNormalBlock()) {
                this.level.useBreakOn(this.getPosition());
            }

            return type;
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            this.calculateState(false, true, -1, null);
            return type;
        }

        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (!this.getSide(face.getOpposite()).isNormalBlock() || face == Direction.DOWN || face == Direction.UP) {
            return false;
        }

        if (face.getAxis().isHorizontal()) {
            this.setFace(face);
        }

        this.level.setBlock(this.getPosition(), this);

        if (player != null) {
            this.calculateState(false, false, -1, null);
        }
        return true;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);
        boolean attached = isAttached();
        boolean powered = isPowered();

        if (attached || powered) {
            this.calculateState(true, false, -1, null);
        }

        if (powered) {
            this.level.updateAroundRedstone(this.getPosition(), null);
            this.level.updateAroundRedstone(this.getFacing().getOpposite().getOffset(this.getPosition()), null);
        }

        return true;
    }

    public void calculateState(boolean onBreak, boolean updateAround, int pos, BlockState blockState) {
        Direction facing = getFacing();
        Vector3i v = this.getPosition();
        boolean attached = isAttached();
        boolean powered = isPowered();
        boolean canConnect = !onBreak;
        boolean nextPowered = false;
        int distance = 0;
        BlockState[] blockStates = new BlockState[42];

        for (int i = 1; i < 42; ++i) {
            Vector3i vector = v.add(facing.getUnitVector().mul(i));
            BlockState b = this.level.getBlock(vector);

            if (b instanceof BlockBehaviorTripWireHook) {
                if (((BlockBehaviorTripWireHook) b).getFacing() == facing.getOpposite()) {
                    distance = i;
                }
                break;
            }

            if (b.getId() != TRIPWIRE && i != pos) {
                blockStates[i] = null;
                canConnect = false;
            } else {
                if (i == pos) {
                    b = blockState != null ? blockState : b;
                }

                if (b instanceof BlockBehaviorTripWire) {
                    boolean disarmed = !((BlockBehaviorTripWire) b).isDisarmed();
                    boolean wirePowered = ((BlockBehaviorTripWire) b).isPowered();
                    nextPowered |= disarmed && wirePowered;

                    if (i == pos) {
                        this.level.scheduleUpdate(this, 10);
                        canConnect &= disarmed;
                    }
                }
                blockStates[i] = b;
            }
        }

        canConnect = canConnect & distance > 1;
        nextPowered = nextPowered & canConnect;
        BlockBehaviorTripWireHook hook = (BlockBehaviorTripWireHook) BlockState.get(TRIPWIRE_HOOK);
        hook.setAttached(canConnect);
        hook.setPowered(nextPowered);


        if (distance > 0) {
            Vector3i vec = v.add(facing.getUnitVector().mul(distance));
            Direction face = facing.getOpposite();
            hook.setFace(face);
            this.level.setBlock(vec, hook, true, false);
            this.level.updateAroundRedstone(vec, null);
            this.level.updateAroundRedstone(face.getOpposite().getOffset(vec), null);
            this.addSound(vec.toFloat(), canConnect, nextPowered, attached, powered);
        }

        this.addSound(v.toFloat(), canConnect, nextPowered, attached, powered);

        if (!onBreak) {
            hook.setFace(facing);
            this.level.setBlock(v, hook, true, false);

            if (updateAround) {
                this.level.updateAroundRedstone(v, null);
                this.level.updateAroundRedstone(facing.getOpposite().getOffset(v), null);
            }
        }

        if (attached != canConnect) {
            for (int i = 1; i < distance; i++) {
                Vector3i vc = v.add(facing.getUnitVector().mul(i));
                blockState = blockStates[i];

                if (blockState != null && this.level.getBlockId(vc) != AIR) {
                    if (canConnect ^ ((blockState.getMeta() & 0x04) > 0)) {
                        blockState.setMeta(blockState.getMeta() ^ 0x04);
                    }

                    this.level.setBlock(vc, blockState, true, false);
                }
            }
        }
    }

    private void addSound(Vector3f pos, boolean canConnect, boolean nextPowered, boolean attached, boolean powered) {
        if (nextPowered && !powered) {
            this.level.addLevelSoundEvent(pos, SoundEvent.POWER_ON);
            this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 0, 15));
        } else if (!nextPowered && powered) {
            this.level.addLevelSoundEvent(pos, SoundEvent.POWER_OFF);
            this.level.getServer().getPluginManager().callEvent(new BlockRedstoneEvent(this, 15, 0));
        } else if (canConnect && !attached) {
            this.level.addLevelSoundEvent(pos, SoundEvent.ATTACH);
        } else if (!canConnect && attached) {
            this.level.addLevelSoundEvent(pos, SoundEvent.DETACH);
        }
    }

    public boolean isAttached() {
        return (getMeta() & 0x04) > 0;
    }

    public void setAttached(boolean value) {
        if (value ^ this.isAttached()) {
            this.setMeta(this.getMeta() ^ 0x04);
        }
    }

    public boolean isPowered() {
        return (this.getMeta() & 0x08) > 0;
    }

    public void setPowered(boolean value) {
        if (value ^ this.isPowered()) {
            this.setMeta(this.getMeta() ^ 0x08);
        }
    }

    public void setFace(Direction face) {
        this.setMeta(this.getMeta() - this.getMeta() % 4);
        this.setMeta(this.getMeta() | face.getHorizontalIndex());
    }

    @Override
    public boolean isPowerSource(Block block) {
        return true;
    }

    @Override
    public int getWeakPower(Block block, Direction face) {
        return isPowered() ? 15 : 0;
    }

    @Override
    public int getStrongPower(Block block, Direction side) {
        return !isPowered() ? 0 : getFacing() == side ? 15 : 0;
    }

    @Override
    public Item toItem(Block block) {
        return Item.get(id, 0);
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }
}

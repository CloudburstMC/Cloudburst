package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Piston;
import org.cloudburstmc.server.event.block.BlockPistonChangeEvent;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import java.util.ArrayList;
import java.util.List;

import static com.nukkitx.math.vector.Vector3i.UP;
import static org.cloudburstmc.server.block.BlockTypes.*;

public abstract class BlockBehaviorPistonBase extends BlockBehaviorSolid {

    public boolean sticky;

    @Override
    public float getResistance() {
        return 2.5f;
    }

    @Override
    public float getHardness() {
        return 0.5f;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (Math.abs(player.getX() - this.getX()) < 2 && Math.abs(player.getZ() - this.getZ()) < 2) {
            float y = player.getY() + player.getEyeHeight();

            if (y - this.getY() > 2) {
                this.setMeta(Direction.UP.getIndex());
            } else if (this.getY() - y > 0) {
                this.setMeta(Direction.DOWN.getIndex());
            } else {
                this.setMeta(player.getHorizontalFacing().getIndex());
            }
        } else {
            this.setMeta(player.getHorizontalFacing().getIndex());
        }
        this.level.setBlock(blockState.getPosition(), this, true, false);

        Piston piston = BlockEntityRegistry.get().newEntity(BlockEntityTypes.PISTON, this.getChunk(), this.getPosition());
        piston.setSticky(this.sticky);

        return true;
    }

    @Override
    public boolean onBreak(Block block, Item item) {
        super.onBreak(block, item);

        BlockState blockState = this.getSide(getFacing());

        if (blockState instanceof BlockBehaviorPistonHead && ((BlockBehaviorPistonHead) blockState).getFacing() == this.getFacing()) {
            blockState.onBreak(item);
        }
        return true;
    }

    public boolean isExtended() {
        Direction face = getFacing();
        BlockState blockState = getSide(face);
        return blockState instanceof BlockBehaviorPistonHead && ((BlockBehaviorPistonHead) blockState).getFacing() == face;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type != 6 && type != 1) {
            return 0;
        } else {
            BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());
            if (blockEntity instanceof Piston) {
                Piston piston = (Piston) blockEntity;
                boolean powered = this.isPowered();
                if (piston.isPowered() != powered) {
                    this.level.getServer().getPluginManager().callEvent(new BlockPistonChangeEvent(this, powered ? 0 : 15, powered ? 15 : 0));
                    piston.setPowered(!piston.isPowered());
                }
            }

            return type;
        }
    }

    public static boolean canPush(BlockState blockState, Direction face, boolean destroyBlocks) {
        if (blockState.canBePushed() && blockState.getY() >= 0 && (face != Direction.DOWN || blockState.getY() != 0) &&
                blockState.getY() <= 255 && (face != Direction.UP || blockState.getY() != 255)) {
            if (!(blockState instanceof BlockBehaviorPistonBase)) {

                if (blockState instanceof FloodableBlockBehavior) {
                    return destroyBlocks;
                }
            } else return !((BlockBehaviorPistonBase) blockState).isExtended();
            return true;
        }
        return false;

    }

    public Direction getFacing() {
        return Direction.fromIndex(this.getMeta()).getOpposite();
    }

    private boolean isPowered() {
        Direction face = getFacing();

        for (Direction side : Direction.values()) {
            if (side != face && this.level.isSidePowered(side.getOffset(this.getPosition()), side)) {
                return true;
            }
        }

        if (this.level.isSidePowered(this.getPosition(), Direction.DOWN)) {
            return true;
        } else {
            Vector3i pos = this.getPosition().add(UP);

            for (Direction side : Direction.values()) {
                if (side != Direction.DOWN && this.level.isSidePowered(side.getOffset(pos), side)) {
                    return true;
                }
            }

            return false;
        }
    }

    private void checkState() {
        Direction facing = getFacing();
        boolean isPowered = this.isPowered();

        if (isPowered && !isExtended()) {
            if ((new BlocksCalculator(this.level, this, facing, true)).canMove()) {
                if (!this.doMove(true)) {
                    return;
                }

                this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.PISTON_OUT);
            } else {
            }
        } else if (!isPowered && isExtended()) {
            //this.level.setBlock() TODO: set piston extension?

            if (this.sticky) {
                Vector3i pos = this.getPosition().add(facing.getXOffset() * 2, facing.getYOffset() * 2, facing.getZOffset() * 2);
                BlockState blockState = this.level.getBlock(pos);

                if (blockState.getId() == AIR) {
                    this.level.setBlock(facing.getOffset(this.getPosition()), BlockState.get(AIR), true, true);
                }
                if (canPush(blockState, facing.getOpposite(), false) && (!(blockState instanceof FloodableBlockBehavior) || blockState.getId() == PISTON || blockState.getId() == STICKY_PISTON)) {
                    this.doMove(false);
                }
            } else {
                this.level.setBlock(facing.getOffset(this.getPosition()), BlockState.get(AIR), true, false);
            }

            this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.PISTON_IN);
        }
    }

    private boolean doMove(boolean extending) {
        Vector3i pos = this.getPosition();
        Direction direction = getFacing();

        if (!extending) {
            this.level.setBlock(direction.getOffset(pos), BlockState.get(AIR), true, false);
        }

        BlocksCalculator calculator = new BlocksCalculator(this.level, this, direction, extending);

        if (!calculator.canMove()) {
            return false;
        } else {
            List<BlockState> blockStates = calculator.getBlocksToMove();

            List<BlockState> newBlockStates = new ArrayList<>(blockStates);

            List<BlockState> destroyBlockStates = calculator.getBlocksToDestroy();
            Direction side = extending ? direction : direction.getOpposite();

            for (int i = destroyBlockStates.size() - 1; i >= 0; --i) {
                BlockState blockState = destroyBlockStates.get(i);
                this.level.useBreakOn(blockState.getPosition());
            }

            for (int i = blockStates.size() - 1; i >= 0; --i) {
                BlockState blockState = blockStates.get(i);
                this.level.setBlock(blockState.getPosition(), BlockState.get(AIR));
                Vector3i newPos = side.getOffset(blockState.getPosition());

                //TODO: change this to block entity
                this.level.setBlock(newPos, newBlockStates.get(i));
            }

            Vector3i pistonHead = direction.getOffset(pos);

            if (extending) {
                //extension block entity
                this.level.setBlock(pistonHead, BlockState.get(PISTON_ARM_COLLISION, this.getMeta()));
            }

            return true;
        }
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, 0);
    }

    @Override
    public Direction getBlockFace() {
        return Direction.fromHorizontalIndex(this.getMeta() & 0x07);
    }

    public class BlocksCalculator {

        private final Level level;
        private final Vector3i pistonPos;
        private final BlockState blockStateToMove;
        private final Direction moveDirection;

        private final List<BlockState> toMove = new ArrayList<>();
        private final List<BlockState> toDestroy = new ArrayList<>();

        public BlocksCalculator(Level level, BlockState pos, Direction facing, boolean extending) {
            this.level = level;
            this.pistonPos = pos.getPosition();

            if (extending) {
                this.moveDirection = facing;
                this.blockStateToMove = pos.getSide(facing);
            } else {
                this.moveDirection = facing.getOpposite();
                this.blockStateToMove = pos.getSide(facing, 2);
            }
        }

        public boolean canMove() {
            this.toMove.clear();
            this.toDestroy.clear();
            BlockState blockState = this.blockStateToMove;

            if (!canPush(blockState, this.moveDirection, false)) {
                if (blockState instanceof FloodableBlockBehavior) {
                    this.toDestroy.add(this.blockStateToMove);
                    return true;
                } else {
                    return false;
                }
            } else if (!this.addBlockLine(this.blockStateToMove)) {
                return false;
            } else {
                for (BlockState b : this.toMove) {
                    if (b.getId() == SLIME && !this.addBranchingBlocks(b)) {
                        return false;
                    }
                }

                return true;
            }
        }

        private boolean addBlockLine(BlockState origin) {
            BlockState blockState = origin.clone();

            if (blockState.getId() == AIR) {
                return true;
            } else if (!canPush(origin, this.moveDirection, false)) {
                return true;
            } else if (origin.equals(this.pistonPos)) {
                return true;
            } else if (this.toMove.contains(origin)) {
                return true;
            } else {
                int count = 1;

                if (count + this.toMove.size() > 12) {
                    return false;
                } else {
                    while (blockState.getId() == SLIME) {
                        blockState = origin.getSide(this.moveDirection.getOpposite(), count);

                        if (blockState.getId() == AIR || !canPush(blockState, this.moveDirection, false) || blockState.equals(this.pistonPos)) {
                            break;
                        }

                        ++count;

                        if (count + this.toMove.size() > 12) {
                            return false;
                        }
                    }

                    int blockCount = 0;

                    for (int step = count - 1; step >= 0; --step) {
                        this.toMove.add(blockState.getSide(this.moveDirection.getOpposite(), step));
                        ++blockCount;
                    }

                    int steps = 1;

                    while (true) {
                        BlockState nextBlockState = blockState.getSide(this.moveDirection, steps);
                        int index = this.toMove.indexOf(nextBlockState);

                        if (index > -1) {
                            this.reorderListAtCollision(blockCount, index);

                            for (int l = 0; l <= index + blockCount; ++l) {
                                BlockState b = this.toMove.get(l);

                                if (b.getId() == SLIME && !this.addBranchingBlocks(b)) {
                                    return false;
                                }
                            }

                            return true;
                        }

                        if (nextBlockState.getId() == AIR) {
                            return true;
                        }

                        if (!canPush(nextBlockState, this.moveDirection, true) || nextBlockState.equals(this.pistonPos)) {
                            return false;
                        }

                        if (nextBlockState instanceof FloodableBlockBehavior) {
                            this.toDestroy.add(nextBlockState);
                            return true;
                        }

                        if (this.toMove.size() >= 12) {
                            return false;
                        }

                        this.toMove.add(nextBlockState);
                        ++blockCount;
                        ++steps;
                    }
                }
            }
        }

        private void reorderListAtCollision(int count, int index) {
            List<BlockState> list = new ArrayList<>(this.toMove.subList(0, index));
            List<BlockState> list1 = new ArrayList<>(this.toMove.subList(this.toMove.size() - count, this.toMove.size()));
            List<BlockState> list2 = new ArrayList<>(this.toMove.subList(index, this.toMove.size() - count));
            this.toMove.clear();
            this.toMove.addAll(list);
            this.toMove.addAll(list1);
            this.toMove.addAll(list2);
        }

        private boolean addBranchingBlocks(BlockState blockState) {
            for (Direction face : Direction.values()) {
                if (face.getAxis() != this.moveDirection.getAxis() && !this.addBlockLine(blockState.getSide(face))) {
                    return false;
                }
            }

            return true;
        }

        public List<BlockState> getBlocksToMove() {
            return this.toMove;
        }

        public List<BlockState> getBlocksToDestroy() {
            return this.toDestroy;
        }
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}

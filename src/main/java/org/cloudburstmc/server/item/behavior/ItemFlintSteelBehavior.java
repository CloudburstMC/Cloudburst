package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockTypes.OBSIDIAN;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemFlintSteelBehavior extends ItemToolBehavior {
    /**
     * The maximum possible size of the outside of a nether portal
     * 23x23 in vanilla
     */
    private static final int MAX_PORTAL_SIZE = 23;

    public ItemFlintSteelBehavior() {
        super(null, null);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        val targetState = target.getState();
        if (block.getState() == BlockStates.AIR && targetState.getBehavior().isSolid(targetState) || targetState.getType() == BlockTypes.LEAVES) {
            PORTAL:
            if (target.getState().getType() == OBSIDIAN) {
                final Vector3i pos = target.getPosition();
                final int targX = pos.getX();
                final int targY = pos.getY();
                final int targZ = pos.getZ();
                //check if there's air above (at least 3 blocks)
                for (int i = 1; i < 4; i++) {
                    if (level.getBlockAt(targX, targY + i, targZ) != BlockStates.AIR) {
                        break PORTAL;
                    }
                }
                int sizePosX = 0;
                int sizeNegX = 0;
                int sizePosZ = 0;
                int sizeNegZ = 0;
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockAt(targX + i, targY, targZ).getType() == OBSIDIAN) {
                        sizePosX++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockAt(targX - i, targY, targZ).getType() == OBSIDIAN) {
                        sizeNegX++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockAt(targX, targY, targZ + i).getType() == OBSIDIAN) {
                        sizePosZ++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockAt(targX, targY, targZ - i).getType() == OBSIDIAN) {
                        sizeNegZ++;
                    } else {
                        break;
                    }
                }
                //plus one for target block
                int sizeX = sizePosX + sizeNegX + 1;
                int sizeZ = sizePosZ + sizeNegZ + 1;
                if (sizeX >= 2 && sizeX <= MAX_PORTAL_SIZE) {
                    //start scan from 1 block above base
                    //find pillar or end of portal to start scan
                    int scanX = targX;
                    int scanY = targY + 1;
                    int scanZ = targZ;
                    for (int i = 0; i < sizePosX + 1; i++) {
                        //this must be air
                        if (level.getBlockAt(scanX + i, scanY, scanZ) != BlockStates.AIR) {
                            break PORTAL;
                        }
                        if (level.getBlockAt(scanX + i + 1, scanY, scanZ).getType() == OBSIDIAN) {
                            scanX += i;
                            break;
                        }
                    }
                    //make sure that the above loop finished
                    if (level.getBlockAt(scanX + 1, scanY, scanZ).getType() != OBSIDIAN) {
                        break PORTAL;
                    }

                    int innerWidth = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockAt(scanX - i, scanY, scanZ);
                        if (state == BlockStates.AIR) {
                            innerWidth++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    int innerHeight = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockAt(scanX, scanY + i, scanZ);
                        if (state == BlockStates.AIR) {
                            innerHeight++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    if (!(innerWidth <= MAX_PORTAL_SIZE - 2
                            && innerWidth >= 2
                            && innerHeight <= MAX_PORTAL_SIZE - 2
                            && innerHeight >= 3)) {
                        break PORTAL;
                    }

                    for (int height = 0; height < innerHeight + 1; height++) {
                        if (height == innerHeight) {
                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockAt(scanX - width, scanY + height, scanZ).getType() != OBSIDIAN) {
                                    break PORTAL;
                                }
                            }
                        } else {
                            if (level.getBlockAt(scanX + 1, scanY + height, scanZ).getType() != OBSIDIAN
                                    || level.getBlockAt(scanX - innerWidth, scanY + height, scanZ).getType() != OBSIDIAN) {
                                break PORTAL;
                            }

                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockAt(scanX - width, scanY + height, scanZ) != BlockStates.AIR) {
                                    break PORTAL;
                                }
                            }
                        }
                    }

                    for (int height = 0; height < innerHeight; height++) {
                        for (int width = 0; width < innerWidth; width++) {
                            level.setBlock(Vector3i.from(scanX - width, scanY + height, scanZ), BlockState.get(BlockTypes.PORTAL));
                        }
                    }

                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    return null;
                } else if (sizeZ >= 2 && sizeZ <= MAX_PORTAL_SIZE) {
                    //start scan from 1 block above base
                    //find pillar or end of portal to start scan
                    int scanX = targX;
                    int scanY = targY + 1;
                    int scanZ = targZ;
                    for (int i = 0; i < sizePosZ + 1; i++) {
                        //this must be air
                        if (level.getBlockAt(scanX, scanY, scanZ + i) != BlockStates.AIR) {
                            break PORTAL;
                        }
                        if (level.getBlockAt(scanX, scanY, scanZ + i + 1).getType() == OBSIDIAN) {
                            scanZ += i;
                            break;
                        }
                    }
                    //make sure that the above loop finished
                    if (level.getBlockAt(scanX, scanY, scanZ + 1).getType() != OBSIDIAN) {
                        break PORTAL;
                    }

                    int innerWidth = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockAt(scanX, scanY, scanZ - i);
                        if (state == BlockStates.AIR) {
                            innerWidth++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    int innerHeight = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockAt(scanX, scanY + i, scanZ);
                        if (state == BlockStates.AIR) {
                            innerHeight++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    if (!(innerWidth <= MAX_PORTAL_SIZE - 2
                            && innerWidth >= 2
                            && innerHeight <= MAX_PORTAL_SIZE - 2
                            && innerHeight >= 3)) {
                        break PORTAL;
                    }

                    for (int height = 0; height < innerHeight + 1; height++) {
                        if (height == innerHeight) {
                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockAt(scanX, scanY + height, scanZ - width).getType() != OBSIDIAN) {
                                    break PORTAL;
                                }
                            }
                        } else {
                            if (level.getBlockAt(scanX, scanY + height, scanZ + 1).getType() != OBSIDIAN
                                    || level.getBlockAt(scanX, scanY + height, scanZ - innerWidth).getType() != OBSIDIAN) {
                                break PORTAL;
                            }

                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockAt(scanX, scanY + height, scanZ - width) != BlockStates.AIR) {
                                    break PORTAL;
                                }
                            }
                        }
                    }

                    for (int height = 0; height < innerHeight; height++) {
                        for (int width = 0; width < innerWidth; width++) {
                            level.setBlock(Vector3i.from(scanX, scanY + height, scanZ - width), BlockState.get(BlockTypes.PORTAL));
                        }
                    }

                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    return null;
                }
            }
            BlockState fire = BlockState.get(BlockTypes.FIRE);
            BlockBehaviorFire fireBehavior = (BlockBehaviorFire) BlockRegistry.get().getBehavior(BlockTypes.FIRE);

            if (BlockBehaviorFire.isBlockTopFacingSurfaceSolid(block.downState()) || BlockBehaviorFire.canNeighborBurn(block)) {
                BlockIgniteEvent e = new BlockIgniteEvent(block, null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
                block.getLevel().getServer().getEventManager().fire(e);

                if (!e.isCancelled()) {
                    level.setBlock(block.getPosition(), fire, true);
                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    block = block.getLevel().getBlock(block.getPosition());
                    level.scheduleUpdate(block, fireBehavior.tickRate() + ThreadLocalRandom.current().nextInt(10));
                }
                return null;
            }

            if (player.getGamemode().isSurvival()) {
                return this.useOn(itemStack, block);
            }
        }

        return null;
    }

    @Override
    public int getMaxDurability() {
        return ItemToolBehavior.DURABILITY_FLINT_STEEL;
    }
}

package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLeaves;
import org.cloudburstmc.server.block.behavior.BlockBehaviorSolid;
import org.cloudburstmc.server.event.block.BlockIgniteEvent;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.server.block.BlockIds.OBSIDIAN;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemFlintSteel extends ItemTool {
    /**
     * The maximum possible size of the outside of a nether portal
     * 23x23 in vanilla
     */
    private static final int MAX_PORTAL_SIZE = 23;

    public ItemFlintSteel(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        if (block.getState() == BlockStates.AIR && target instanceof BlockBehaviorSolid || target instanceof BlockBehaviorLeaves) {
            PORTAL:
            if (target.getState().getType() == OBSIDIAN) {
                final Vector3i pos = target.getPosition();
                final int targX = pos.getX();
                final int targY = pos.getY();
                final int targZ = pos.getZ();
                //check if there's air above (at least 3 blocks)
                for (int i = 1; i < 4; i++) {
                    if (world.getBlockAt(targX, targY + i, targZ) != BlockStates.AIR) {
                        break PORTAL;
                    }
                }
                int sizePosX = 0;
                int sizeNegX = 0;
                int sizePosZ = 0;
                int sizeNegZ = 0;
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (world.getBlockAt(targX + i, targY, targZ).getType() == OBSIDIAN) {
                        sizePosX++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (world.getBlockAt(targX - i, targY, targZ).getType() == OBSIDIAN) {
                        sizeNegX++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (world.getBlockAt(targX, targY, targZ + i).getType() == OBSIDIAN) {
                        sizePosZ++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (world.getBlockAt(targX, targY, targZ - i).getType() == OBSIDIAN) {
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
                        if (world.getBlockAt(scanX + i, scanY, scanZ) != BlockStates.AIR) {
                            break PORTAL;
                        }
                        if (world.getBlockAt(scanX + i + 1, scanY, scanZ).getType() == OBSIDIAN) {
                            scanX += i;
                            break;
                        }
                    }
                    //make sure that the above loop finished
                    if (world.getBlockAt(scanX + 1, scanY, scanZ).getType() != OBSIDIAN) {
                        break PORTAL;
                    }

                    int innerWidth = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = world.getBlockAt(scanX - i, scanY, scanZ);
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
                        BlockState state = world.getBlockAt(scanX, scanY + i, scanZ);
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
                                if (world.getBlockAt(scanX - width, scanY + height, scanZ).getType() != OBSIDIAN) {
                                    break PORTAL;
                                }
                            }
                        } else {
                            if (world.getBlockAt(scanX + 1, scanY + height, scanZ).getType() != OBSIDIAN
                                    || world.getBlockAt(scanX - innerWidth, scanY + height, scanZ).getType() != OBSIDIAN) {
                                break PORTAL;
                            }

                            for (int width = 0; width < innerWidth; width++) {
                                if (world.getBlockAt(scanX - width, scanY + height, scanZ) != BlockStates.AIR) {
                                    break PORTAL;
                                }
                            }
                        }
                    }

                    for (int height = 0; height < innerHeight; height++) {
                        for (int width = 0; width < innerWidth; width++) {
                            world.setBlock(Vector3i.from(scanX - width, scanY + height, scanZ), BlockState.get(BlockIds.PORTAL));
                        }
                    }

                    world.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    return true;
                } else if (sizeZ >= 2 && sizeZ <= MAX_PORTAL_SIZE) {
                    //start scan from 1 block above base
                    //find pillar or end of portal to start scan
                    int scanX = targX;
                    int scanY = targY + 1;
                    int scanZ = targZ;
                    for (int i = 0; i < sizePosZ + 1; i++) {
                        //this must be air
                        if (world.getBlockAt(scanX, scanY, scanZ + i) != BlockStates.AIR) {
                            break PORTAL;
                        }
                        if (world.getBlockAt(scanX, scanY, scanZ + i + 1).getType() == OBSIDIAN) {
                            scanZ += i;
                            break;
                        }
                    }
                    //make sure that the above loop finished
                    if (world.getBlockAt(scanX, scanY, scanZ + 1).getType() != OBSIDIAN) {
                        break PORTAL;
                    }

                    int innerWidth = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = world.getBlockAt(scanX, scanY, scanZ - i);
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
                        BlockState state = world.getBlockAt(scanX, scanY + i, scanZ);
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
                                if (world.getBlockAt(scanX, scanY + height, scanZ - width).getType() != OBSIDIAN) {
                                    break PORTAL;
                                }
                            }
                        } else {
                            if (world.getBlockAt(scanX, scanY + height, scanZ + 1).getType() != OBSIDIAN
                                    || world.getBlockAt(scanX, scanY + height, scanZ - innerWidth).getType() != OBSIDIAN) {
                                break PORTAL;
                            }

                            for (int width = 0; width < innerWidth; width++) {
                                if (world.getBlockAt(scanX, scanY + height, scanZ - width) != BlockStates.AIR) {
                                    break PORTAL;
                                }
                            }
                        }
                    }

                    for (int height = 0; height < innerHeight; height++) {
                        for (int width = 0; width < innerWidth; width++) {
                            world.setBlock(Vector3i.from(scanX, scanY + height, scanZ - width), BlockState.get(BlockIds.PORTAL));
                        }
                    }

                    world.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    return true;
                }
            }
            BlockState fire = BlockState.get(BlockIds.FIRE);
            BlockBehaviorFire fireBehavior = (BlockBehaviorFire) BlockRegistry.get().getBehavior(BlockIds.FIRE);

            if (BlockBehaviorFire.isBlockTopFacingSurfaceSolid(block.downState()) || BlockBehaviorFire.canNeighborBurn(block)) {
                BlockIgniteEvent e = new BlockIgniteEvent(block, null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
                block.getWorld().getServer().getEventManager().fire(e);

                if (!e.isCancelled()) {
                    world.setBlock(block.getPosition(), fire, true);
                    world.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    block = block.getWorld().getBlock(block.getPosition());
                    world.scheduleUpdate(block, fireBehavior.tickRate() + ThreadLocalRandom.current().nextInt(10));
                }
                return true;
            }

            if (player.getGamemode().isSurvival() && this.useOn(block)) {
                if (this.getMeta() >= this.getMaxDurability()) {
                    this.setCount(0);
                } else {
                    this.setMeta(this.getMeta() + 1);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int getMaxDurability() {
        return ItemTool.DURABILITY_FLINT_STEEL;
    }
}

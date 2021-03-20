package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import lombok.val;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.api.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.api.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.inventory.PlayerInventory;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBucketBehavior extends CloudItemBehavior {

    protected static String getName(int meta) {
        switch (meta) {
            case 1:
                return "Milk";
            case 2:
                return "Bucket of Cod";
            case 3:
                return "Bucket of Salmon";
            case 4:
                return "Bucket of Tropical Fish";
            case 5:
                return "Bucket of Pufferfish";
            case 8:
                return "Water Bucket";
            case 10:
                return "Lava Bucket";
            default:
                return "Bucket";
        }
    }

    public static BlockType getBlockIdFromDamage(Bucket data) {
        switch (data) {
            case WATER:
                return FLOWING_WATER;
            case LAVA:
                return FLOWING_LAVA;
            default:
                return AIR;
        }
    }

    public Bucket getDamageFromIdentifier(BlockType id) {
        if (id == FLOWING_WATER || id == WATER) {
            return Bucket.WATER;
        } else if (id == FLOWING_LAVA || id == LAVA) {
            return Bucket.LAVA;
        }
        throw new IllegalArgumentException(id + " cannot be in bucket");
    }

    @Override
    public int getMaxStackSize(ItemStack item) {
        return item.getMetadata(Bucket.class) == Bucket.EMPTY ? 16 : 1;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        if (player.isAdventure()) {
            return null;
        }

        BlockState bucketContents = BlockRegistry.get().getBlock(getBlockIdFromDamage(itemStack.getMetadata(Bucket.class)));

        if (bucketContents == BlockStates.AIR) {
            BlockState liquid;

            if (target.isWaterlogged()) {
                liquid = target.getExtra();
            } else {
                liquid = target.getState();
            }

            if (liquid.inCategory(BlockCategory.LIQUID) && liquid.ensureTrait(BlockTraits.FLUID_LEVEL) == 0) {
                ItemStack result = CloudItemRegistry.get().getItem(ItemTypes.BUCKET, 1, this.getDamageFromIdentifier(liquid.getType()));
                PlayerBucketFillEvent ev;
                player.getServer().getEventManager().fire(ev = new PlayerBucketFillEvent(player, block.getState(), face, itemStack, result));
                if (!ev.isCancelled()) {
                    target.set(BlockStates.AIR, true);

                    // When water is removed ensure any adjacent still water is
                    // replaced with water that can flow.
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        Block b = target.getSide(side);

                        if (b.getState().getType() == WATER) {
                            b.set(BlockRegistry.get().getBlock(FLOWING_WATER));
                        } else if (b.getExtra().getType() == WATER) {
                            b.set(BlockRegistry.get().getBlock(FLOWING_WATER), 1, true, false);
                        }
                    }

                    if (player.isSurvival()) {
                        if(itemStack.getAmount() == 1) {
                            ((PlayerInventory) player.getInventory()).setItemInHand(ev.getItem());
                        } else {
                            ((PlayerInventory) player.getInventory()).decrementHandCount();
                            ItemStack[] drops = player.getInventory().addItem(ev.getItem());
                            if(drops.length > 0) {
                                ((CloudPlayer) player).dropItem(drops[0]);
                            }
                        }
                    }

                    if (liquid.getType() == LAVA) {
                        ((CloudLevel) level).addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_LAVA);
                    } else {
                        ((CloudLevel) level).addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_WATER);
                    }

                    return null;
                } else {
                    player.getInventory().sendContents(player);
                }
            }
        } else if (bucketContents.inCategory(BlockCategory.LIQUID)) {
            ItemStack result = CloudItemRegistry.get().getItem(ItemTypes.BUCKET);
            Block emptyTarget = block;
            val targetState = target.getState();
            val behavior = targetState.getBehavior();

            if (behavior.canWaterlogSource(targetState) && bucketContents.getType() == FLOWING_WATER) {
                emptyTarget = target;
            }

            val blockBehavior = emptyTarget.getState().getBehavior();

            PlayerBucketEmptyEvent ev = new PlayerBucketEmptyEvent(player, emptyTarget, face, itemStack, result);
            if (!blockBehavior.canBeFlooded(emptyTarget.getState()) && !blockBehavior.canWaterlogSource(emptyTarget.getState())) {
                ev.setCancelled(true);
            }

            if (((CloudLevel) player.getLevel()).getDimension() == CloudLevel.DIMENSION_NETHER && itemStack.getMetadata(Bucket.class) != Bucket.LAVA) {
                ev.setCancelled(true);
            }

            player.getServer().getEventManager().fire(ev);

            if (!ev.isCancelled()) {
                int layer = behavior.canWaterlogSource(targetState) ? 1 : 0;
                if (target.getLevel().setBlockState(emptyTarget.getPosition(), layer, bucketContents, true, true)) {
                    target.getLevel().scheduleUpdate(emptyTarget.getPosition(), bucketContents.getBehavior().tickRate());
                }
                if (player.isSurvival()) {
                    if(itemStack.getAmount() == 1) {
                        ((PlayerInventory) player.getInventory()).setItemInHand(ev.getItem());
                    } else {
                        ((PlayerInventory) player.getInventory()).decrementHandCount();
                        ItemStack[] drops = player.getInventory().addItem(ev.getItem());
                        if(drops.length > 0) {
                            ((CloudPlayer) player).dropItem(drops[0]);
                        }
                    }
                }

                if (itemStack.getMetadata(Bucket.class) == Bucket.LAVA) {
                    ((CloudLevel) level).addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_LAVA);
                } else {
                    ((CloudLevel) level).addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_WATER);
                }

                return null;
            } else {
                ((CloudLevel) player.getLevel()).sendBlocks(new CloudPlayer[]{(CloudPlayer) player},
                        new Block[]{new CloudBlock((CloudLevel) block.getLevel(), block.getPosition(), CloudBlock.EMPTY)},
                        UpdateBlockPacket.FLAG_ALL_PRIORITY);
                player.getInventory().sendContents(player);
            }
        }

        return null;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        return item.getMetadata(Bucket.class) == Bucket.MILK;
    }

    @Override
    public ItemStack onUse(ItemStack item, int ticksUsed, Player player) {
        if (player.isSpectator()) {
            return null;
        }

        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, item);

        player.getServer().getEventManager().fire(consumeEvent);
        if (consumeEvent.isCancelled()) {
            player.getInventory().sendContents(player);
            return null;
        }

        if (!player.isCreative()) {
            ((PlayerInventory) player.getInventory()).decrementHandCount();
            player.getInventory().addItem(CloudItemRegistry.get().getItem(ItemTypes.BUCKET));
        }

        player.removeAllEffects();
        return null;
    }

    @Override
    public short getFuelTime(ItemStack item) {
        if (item.getMetadata(Bucket.class) == Bucket.LAVA) {
            return 20000;
        }

        return 0;
    }
}

package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import lombok.val;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.server.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.server.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.block.BlockTypes.*;

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

        BlockState bucketContents = BlockState.get(getBlockIdFromDamage(itemStack.getMetadata(Bucket.class)));

        if (bucketContents == BlockStates.AIR) {
            BlockState liquid;

            if (target.isWaterlogged()) {
                liquid = target.getExtra();
            } else {
                liquid = target.getState();
            }

            if (liquid.inCategory(BlockCategory.LIQUID) && liquid.ensureTrait(BlockTraits.FLUID_LEVEL) == 0) {
                ItemStack result = ItemStack.get(ItemTypes.BUCKET, 1, this.getDamageFromIdentifier(liquid.getType()));
                PlayerBucketFillEvent ev;
                player.getServer().getEventManager().fire(ev = new PlayerBucketFillEvent(player, block, face, itemStack, result));
                if (!ev.isCancelled()) {
                    target.set(BlockStates.AIR, true);

                    // When water is removed ensure any adjacent still water is
                    // replaced with water that can flow.
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        Block b = target.getSide(side);

                        if (b.getState().getType() == WATER) {
                            b.set(BlockState.get(FLOWING_WATER));
                        } else if (b.getExtra().getType() == WATER) {
                            b.set(BlockState.get(FLOWING_WATER), 1, true, false);
                        }
                    }

                    if (player.isSurvival()) {
                        player.getInventory().decrementHandCount();
                        player.getInventory().addItem(ev.getItem());
                    }

                    if (liquid.getType() == LAVA) {
                        level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_LAVA);
                    } else {
                        level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_WATER);
                    }

                    return null;
                } else {
                    player.getInventory().sendContents(player);
                }
            }
        } else if (bucketContents.inCategory(BlockCategory.LIQUID)) {
            ItemStack result = ItemStack.get(ItemTypes.BUCKET);
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

            if (player.getLevel().getDimension() == Level.DIMENSION_NETHER && itemStack.getMetadata(Bucket.class) != Bucket.LAVA) {
                ev.setCancelled(true);
            }

            player.getServer().getEventManager().fire(ev);

            if (!ev.isCancelled()) {
                int layer = behavior.canWaterlogSource(targetState) ? 1 : 0;
                if (target.getLevel().setBlock(emptyTarget.getPosition(), layer, bucketContents, true, true)) {
                    target.getLevel().scheduleUpdate(emptyTarget.getPosition(), bucketContents.getBehavior().tickRate());
                }
                if (player.isSurvival()) {
                    player.getInventory().decrementHandCount();
                    player.getInventory().addItem(ev.getItem());
                }

                if (itemStack.getMetadata(Bucket.class) == Bucket.LAVA) {
                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_LAVA);
                } else {
                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_WATER);
                }

                return null;
            } else {
                player.getLevel().sendBlocks(new Player[]{player},
                        new Block[]{new CloudBlock(block.getLevel(), block.getPosition(), CloudBlock.EMPTY)},
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
            player.getInventory().decrementHandCount();
            player.getInventory().addItem(ItemStack.get(ItemTypes.BUCKET));
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

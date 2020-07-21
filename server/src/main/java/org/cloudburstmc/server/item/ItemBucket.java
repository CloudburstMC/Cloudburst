package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.block.behavior.BlockBehaviorAir;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLava;
import org.cloudburstmc.server.block.behavior.BlockBehaviorLiquid;
import org.cloudburstmc.server.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.server.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.server.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBucket extends Item {

    public ItemBucket(Identifier id) {
        super(id);
    }

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

    public static Identifier getBlockIdFromDamage(int target) {
        switch (target) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 8:
            case 9:
                return FLOWING_WATER;
            case 10:
            case 11:
                return FLOWING_LAVA;
            default:
                return AIR;
        }
    }

    public int getDamageFromIdentifier(Identifier id) {
        if (id == FLOWING_WATER) {
            return 8;
        } else if (id == FLOWING_LAVA) {
            return 10;
        }
        throw new IllegalArgumentException(id + " cannot be in bucket");
    }

    @Override
    public int getMaxStackSize() {
        return this.getMeta() == 0 ? 16 : 1;
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, Vector3f clickPos) {
        BlockState bucketContents = BlockState.get(getBlockIdFromDamage(this.getMeta()));

        if (bucketContents instanceof BlockBehaviorAir) {
            if (target.isWaterlogged()) {
                target = player.getLevel().getBlock(target.getX(), target.getY(), target.getZ(), 1);
            }
            if (target instanceof BlockBehaviorLiquid && target.getMeta() == 0) {
                Item result = Item.get(ItemIds.BUCKET, this.getDamageFromIdentifier(target.getId()), 1);
                PlayerBucketFillEvent ev;
                player.getServer().getPluginManager().callEvent(ev = new PlayerBucketFillEvent(player, block, face, this, result));
                if (!ev.isCancelled()) {
                    player.getLevel().setBlock(target.getPosition(), BlockState.get(AIR), true, true);

                    // When water is removed ensure any adjacent still water is
                    // replaced with water that can flow.
                    for (BlockFace side : BlockFace.Plane.HORIZONTAL) {
                        Block b = target.getSide(side);
                        if (b.isWaterlogged()) {
                            b = player.getLevel().getBlock(b.getX(), b.getY(), b.getZ(), 1);
                        }
                        if (b.getState().getType() == WATER) {
                            level.setBlock(b.getPosition(), BlockState.get(FLOWING_WATER));
                        }
                    }

                    if (player.isSurvival()) {
                        Item clone = this.clone();
                        clone.setCount(this.getCount() - 1);
                        player.getInventory().setItemInHand(clone);
                        player.getInventory().addItem(ev.getItem());
                    }

                    if (target instanceof BlockBehaviorLava) {
                        level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_LAVA);
                    } else {
                        level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_WATER);
                    }

                    return true;
                } else {
                    player.getInventory().sendContents(player);
                }
            }
        } else if (bucketContents instanceof BlockBehaviorLiquid) {
            Item result = Item.get(ItemIds.BUCKET, 0, 1);
            Block emptyTarget = block;
            if (target.canWaterlogSource() && bucketContents.getId() == FLOWING_WATER) {
                emptyTarget = target;
            }
            PlayerBucketEmptyEvent ev = new PlayerBucketEmptyEvent(player, emptyTarget, face, this, result);
            if (!emptyTarget.canBeFlooded() && !emptyTarget.canWaterlogSource()) {
                ev.setCancelled(true);
            }

            if (player.getLevel().getDimension() == Level.DIMENSION_NETHER && this.getMeta() != 10) {
                ev.setCancelled(true);
            }

            player.getServer().getPluginManager().callEvent(ev);

            if (!ev.isCancelled()) {
                int layer = emptyTarget.canWaterlogSource() ? 1 : 0;
                if (target.getLevel().setBlock(emptyTarget.getPosition(), layer, bucketContents, true, true)) {
                    bucketContents.getLevel().scheduleUpdate(bucketContents, bucketContents.tickRate());
                }
                if (player.isSurvival()) {
                    Item clone = this.clone();
                    clone.setCount(this.getCount() - 1);
                    player.getInventory().setItemInHand(clone);
                    player.getInventory().addItem(ev.getItem());
                }

                if (this.getMeta() == 10) {
                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_LAVA);
                } else {
                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_WATER);
                }

                return true;
            } else {
                player.getLevel().sendBlocks(new Player[]{player},
                        new Block[]{new CloudBlock(BlockState.AIR, block.getLevel(), block.getChunk(), block.getPosition(), block.getLayer())},
                        UpdateBlockPacket.FLAG_ALL_PRIORITY);
                player.getInventory().sendContents(player);
            }
        }

        return false;
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        return this.getMeta() == 1; // Milk
    }

    @Override
    public boolean onUse(Player player, int ticksUsed) {
        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, this);

        player.getServer().getPluginManager().callEvent(consumeEvent);
        if (consumeEvent.isCancelled()) {
            player.getInventory().sendContents(player);
            return false;
        }

        if (player.isSurvival()) {
            this.decrementCount();
            player.getInventory().setItemInHand(this);
            player.getInventory().addItem(Item.get(ItemIds.BUCKET));
        }

        player.removeAllEffects();
        return true;
    }
}

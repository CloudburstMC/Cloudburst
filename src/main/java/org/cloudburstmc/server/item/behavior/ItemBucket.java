package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import lombok.val;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.server.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.server.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockIds.*;

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
        if (id == FLOWING_WATER || id == WATER) {
            return 8;
        } else if (id == FLOWING_LAVA || id == LAVA) {
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
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        if (player.isAdventure()) {
            return false;
        }

        BlockState bucketContents = BlockState.get(getBlockIdFromDamage(this.getMeta()));

        if (bucketContents == BlockStates.AIR) {
            BlockState liquid;

            if (target.isWaterlogged()) {
                liquid = target.getExtra();
            } else {
                liquid = target.getState();
            }

            if (liquid.inCategory(BlockCategory.LIQUID) && liquid.ensureTrait(BlockTraits.FLUID_LEVEL) == 0) {
                Item result = Item.get(ItemIds.BUCKET, this.getDamageFromIdentifier(liquid.getType()), 1);
                PlayerBucketFillEvent ev;
                player.getServer().getEventManager().fire(ev = new PlayerBucketFillEvent(player, block, face, this, result));
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
                        Item clone = this.clone();
                        clone.setCount(this.getCount() - 1);
                        player.getInventory().setItemInHand(clone);
                        if (this.getCount() == 1) {
                            player.getInventory().setItemInHand(ev.getItem());
                        } else if (player.getInventory().canAddItem(ev.getItem())) {
                            player.getInventory().addItem(ev.getItem());
                        } else {
                            player.dropItem(ev.getItem());
                        }
                    }

                    if (liquid.getType() == LAVA) {
                        world.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_LAVA);
                    } else {
                        world.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_FILL_WATER);
                    }

                    return true;
                } else {
                    player.getInventory().sendContents(player);
                }
            }
        } else if (bucketContents.inCategory(BlockCategory.LIQUID)) {
            Item result = Item.get(ItemIds.BUCKET, 0, 1);
            Block emptyTarget = block;
            val targetState = target.getState();
            val behavior = targetState.getBehavior();

            if (behavior.canWaterlogSource() && bucketContents.getType() == FLOWING_WATER) {
                emptyTarget = target;
            }

            val blockBehavior = emptyTarget.getState().getBehavior();

            PlayerBucketEmptyEvent ev = new PlayerBucketEmptyEvent(player, emptyTarget, face, this, result);
            if (!blockBehavior.canBeFlooded() && !blockBehavior.canWaterlogSource()) {
                System.out.println("cancel");
                ev.setCancelled(true);
            }

            if (player.getWorld().getDimension() == World.DIMENSION_NETHER && this.getMeta() != 10) {
                ev.setCancelled(true);
            }

            player.getServer().getEventManager().fire(ev);

            if (!ev.isCancelled()) {
                int layer = behavior.canWaterlogSource() ? 1 : 0;
                if (target.getWorld().setBlock(emptyTarget.getPosition(), layer, bucketContents, true, true)) {
                    target.getWorld().scheduleUpdate(emptyTarget.getPosition(), bucketContents.getBehavior().tickRate());
                }
                if (player.isSurvival()) {
                    Item clone = this.clone();
                    clone.setCount(this.getCount() - 1);
                    player.getInventory().setItemInHand(clone);
                    player.getInventory().setItemInHand(ev.getItem());
                }

                if (this.getMeta() == 10) {
                    world.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_LAVA);
                } else {
                    world.addLevelSoundEvent(block.getPosition(), SoundEvent.BUCKET_EMPTY_WATER);
                }

                return true;
            } else {
                player.getWorld().sendBlocks(new Player[]{player},
                        new Block[]{new CloudBlock(block.getWorld(), block.getPosition(), CloudBlock.EMPTY)},
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
        if (player.isSpectator()) {
            return false;
        }

        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, this);

        player.getServer().getEventManager().fire(consumeEvent);
        if (consumeEvent.isCancelled()) {
            player.getInventory().sendContents(player);
            return false;
        }

        if (!player.isCreative()) {
            this.decrementCount();
            player.getInventory().setItemInHand(this);
            player.getInventory().addItem(Item.get(ItemIds.BUCKET));
        }

        player.removeAllEffects();
        return true;
    }
}

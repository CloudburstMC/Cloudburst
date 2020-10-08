package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Cauldron;
import org.cloudburstmc.server.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.server.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.data.Bucket;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.CAULDRON;

public class BlockBehaviorCauldron extends BlockBehaviorSolid {

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    public int getFillLevel(Block block) {
        return block.getState().ensureTrait(BlockTraits.FILL_LEVEL);
    }

    public boolean isFull(Block block) {
        return getFillLevel(block) == 0x06;
    }

    public boolean isEmpty(Block block) {
        return getFillLevel(block) == 0x00;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        BlockEntity be = block.getLevel().getBlockEntity(block.getPosition());

        if (!(be instanceof Cauldron)) {
            return false;
        }

        Cauldron cauldron = (Cauldron) be;

        val itemType = item.getType();

        if (itemType == ItemTypes.BUCKET) {
            val bucket = item.getMetadata(Bucket.class);

            if (bucket == Bucket.EMPTY) {//empty bucket
                if (!isFull(block) || cauldron.hasCustomColor() || cauldron.getPotionId() != 0) {
                    return true;
                }

                ItemStack bucketItem = item.toBuilder()
                        .amount(1).itemData(Bucket.WATER).build();

                PlayerBucketFillEvent ev = new PlayerBucketFillEvent(player, block, null, item, bucketItem);
                block.getLevel().getServer().getEventManager().fire(ev);
                if (!ev.isCancelled()) {
                    replaceBucket(item, player, ev.getItem());

                    block.set(block.getState().withTrait(BlockTraits.FILL_LEVEL, 0), true);
                    cauldron.setCustomColor(null);
                    block.getLevel().addSound(block.getPosition().toFloat().add(0.5, 1, 0.5), Sound.CAULDRON_TAKEWATER);
                }
            } else if (bucket == Bucket.WATER || bucket == Bucket.LAVA) {//water bucket
                if (isFull(block) && !cauldron.hasCustomColor() && cauldron.getPotionId() == 0) {
                    return true;
                }

                ItemStack bucketItem = item.toBuilder()
                        .amount(1).itemData(Bucket.EMPTY).build();

                PlayerBucketEmptyEvent ev = new PlayerBucketEmptyEvent(player, block, null, item, bucketItem);
                block.getLevel().getServer().getEventManager().fire(ev);
                if (!ev.isCancelled()) {
                    replaceBucket(item, player, ev.getItem());

                    if (cauldron.getPotionId() != 0 || block.getState().ensureTrait(BlockTraits.CAULDRON_TYPE) != bucket) { //if has potion
                        cauldron.setPotionId(0xffff);//reset potion
                        cauldron.setSplash(false);
                        cauldron.setCustomColor(null);
                        block.set(block.getState().withTrait(BlockTraits.FILL_LEVEL, 0), true);
                        block.getLevel().addSound(block.getPosition(), Sound.CAULDRON_EXPLODE);
                    } else {
                        cauldron.setCustomColor(null);
                        block.set(block.getState().withTrait(BlockTraits.FILL_LEVEL, 6), true);
                        block.getLevel().addLevelSoundEvent(block.getPosition().toFloat().add(0.5, 1, 0.5), SoundEvent.BUCKET_FILL_WATER);
                    }
                    //this.update();
                }
            }
        } else if (itemType == ItemTypes.DYE) {
            // todo
        } else if (itemType == ItemTypes.LEATHER_HELMET || itemType == ItemTypes.LEATHER_CHESTPLATE ||
                itemType == ItemTypes.LEATHER_LEGGINGS || itemType == ItemTypes.LEATHER_BOOTS) {
            // todo
        } else if (itemType == ItemTypes.POTION) {
            if (isFull(block)) {
                return true;
            }

            if (item.getCount() == 1) {
                player.getInventory().clear(player.getInventory().getHeldItemIndex());
            } else if (item.getCount() > 1) {
                player.getInventory().decrementHandCount();

                ItemStack bottle = ItemStack.get(ItemTypes.GLASS_BOTTLE);
                if (player.getInventory().canAddItem(bottle)) {
                    player.getInventory().addItem(bottle);
                } else {
                    player.getLevel().dropItem(player.getPosition().toFloat().add(0, 1.3, 0), bottle, player.getDirectionVector().mul(0.4));
                }
            }

            block.set(block.getState().incrementTrait(BlockTraits.FILL_LEVEL));
            block.getLevel().addSound(block.getPosition(), Sound.CAULDRON_FILLPOTION);
        } else if (itemType == ItemTypes.GLASS_BOTTLE) {
            if (isEmpty(block)) {
                return true;
            }

            if (item.getCount() == 1) {
                player.getInventory().setItemInHand(ItemStack.get(ItemTypes.POTION));
            } else if (item.getCount() > 1) {
                player.getInventory().decrementHandCount();

                ItemStack potion = ItemStack.get(ItemTypes.POTION);
                if (player.getInventory().canAddItem(potion)) {
                    player.getInventory().addItem(potion);
                } else {
                    player.getLevel().dropItem(player.getPosition().toFloat().add(0, 1.3, 0), potion, player.getDirectionVector().mul(0.4));
                }
            }

            block.set(block.getState().decrementTrait(BlockTraits.FILL_LEVEL));
            block.getLevel().addSound(block.getPosition(), Sound.CAULDRON_TAKEPOTION);
        } else {
            return true;
        }

        block.getLevel().updateComparatorOutputLevel(block.getPosition());
        return true;
    }

    protected void replaceBucket(ItemStack oldBucket, Player player, ItemStack newBucket) {
        if (player.isSurvival() || player.isAdventure()) {
            if (oldBucket.getCount() == 1) {
                player.getInventory().setItemInHand(newBucket);
            } else {
                player.getInventory().decrementHandCount();
                if (player.getInventory().canAddItem(newBucket)) {
                    player.getInventory().addItem(newBucket);
                } else {
                    player.getLevel().dropItem(player.getPosition().add(0, 1.3, 0), newBucket, player.getDirectionVector().mul(0.4));
                }
            }
        }
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Cauldron cauldron = BlockEntityRegistry.get().newEntity(CAULDRON, block.getChunk(), block.getPosition());
        cauldron.loadAdditionalData(((CloudItemStack) item).getDataTag());
        cauldron.setPotionId(0xffff);

        return placeBlock(block, item);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (checkTool(block.getState(), hand)) {
            return new ItemStack[]{ItemStack.get(ItemTypes.CAULDRON)};
        }

        return new ItemStack[0];
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.CAULDRON);
    }

    public int getComparatorInputOverride(Block block) {
        return block.getState().ensureTrait(BlockTraits.FILL_LEVEL);
    }
}

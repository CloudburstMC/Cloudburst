package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Cauldron;
import org.cloudburstmc.server.event.player.PlayerBucketEmptyEvent;
import org.cloudburstmc.server.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemBucketBehavior;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.CAULDRON;

public class BlockBehaviorCauldron extends BlockBehaviorSolid {

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

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

        Identifier itemType = item.getId();

        if (itemType == ItemTypes.BUCKET) {
            if (item.getMeta() == 0) {//empty bucket
                if (!isFull(block) || cauldron.hasCustomColor() || cauldron.getPotionId() != 0) {
                    return true;
                }

                ItemBucketBehavior bucket = (ItemBucketBehavior) item.clone();
                bucket.setCount(1);
                bucket.setMeta(8);//water bucket

                PlayerBucketFillEvent ev = new PlayerBucketFillEvent(player, block, null, item, bucket);
                block.getLevel().getServer().getEventManager().fire(ev);
                if (!ev.isCancelled()) {
                    replaceBucket(item, player, ev.getItem());

                    block.set(block.getState().withTrait(BlockTraits.FILL_LEVEL, 0), true);
                    cauldron.setCustomColor(null);
                    block.getLevel().addSound(block.getPosition().toFloat().add(0.5, 1, 0.5), Sound.CAULDRON_TAKEWATER);
                }
            } else if (item.getMeta() == 8) {//water bucket

                if (isFull(block) && !cauldron.hasCustomColor() && cauldron.getPotionId() == 0) {
                    return true;
                }

                ItemBucketBehavior bucket = (ItemBucketBehavior) item.clone();
                bucket.setCount(1);
                bucket.setMeta(0);//empty bucket

                PlayerBucketEmptyEvent ev = new PlayerBucketEmptyEvent(player, block, null, item, bucket);
                block.getLevel().getServer().getEventManager().fire(ev);
                if (!ev.isCancelled()) {
                    replaceBucket(item, player, ev.getItem());

                    if (cauldron.getPotionId() != 0) {//if has potion
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
                item.setCount(item.getCount() - 1);
                player.getInventory().setItemInHand(item);

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
                item.setCount(item.getCount() - 1);
                player.getInventory().setItemInHand(item);

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
                oldBucket.setCount(oldBucket.getCount() - 1);
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
        cauldron.loadAdditionalData(item.getTag());
        cauldron.setPotionId(0xffff);

        return placeBlock(block, item);
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        if (hand.getTier() >= ItemToolBehavior.TIER_WOODEN) {
            return new ItemStack[]{ItemStack.get(ItemTypes.CAULDRON)};
        }

        return new ItemStack[0];
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.CAULDRON);
    }

    public boolean hasComparatorInputOverride() {
        return true;
    }

    public int getComparatorInputOverride(Block block) {
        return block.getState().ensureTrait(BlockTraits.FILL_LEVEL);
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }
}

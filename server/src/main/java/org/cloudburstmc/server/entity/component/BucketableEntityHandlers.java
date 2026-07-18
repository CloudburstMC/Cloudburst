package org.cloudburstmc.server.entity.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.entity.Bucketable;
import org.cloudburstmc.api.entity.component.InteractEntityHandler;
import org.cloudburstmc.api.event.player.PlayerBucketEntityEvent;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.data.BucketEntityData;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.player.CloudPlayer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BucketableEntityHandlers {

    public static InteractEntityHandler capture(ItemType requiredBucket) {
        return (entity, player, item, clickedPos) -> {
            if (!(entity instanceof Bucketable bucketable) || !(player instanceof CloudPlayer cloudPlayer)
                    || item.getType() != requiredBucket || !entity.isAlive()) {
                return false;
            }

            CloudEntity cloudEntity = (CloudEntity) entity;
            ItemStack entityBucket = bucketable.getBaseBucketItem().toBuilder()
                    .data(ItemKeys.BUCKET_ENTITY_DATA, new BucketEntityData(
                            entity.getHealth(), cloudEntity.isInvulnerable(), cloudEntity.isImmobile()))
                    .build();

            if (entity.hasNameTag()) {
                entityBucket = entityBucket.toBuilder().data(ItemKeys.CUSTOM_NAME, entity.getNameTag()).build();
            }

            PlayerBucketEntityEvent event = new PlayerBucketEntityEvent(player, bucketable, item, entityBucket);
            cloudPlayer.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return true;
            }

            ItemStack result = event.getEntityBucket();
            if (item.getCount() == 1) {
                player.getInventory().setSelectedItem(result);
            } else {
                player.getInventory().setSelectedItem(item.withCount(item.getCount() - 1));
                ItemStack[] remaining = player.getInventory().addItem(result);
                for (ItemStack stack : remaining) {
                    cloudPlayer.dropItem(stack);
                }
            }

            cloudPlayer.getLevel().addSound(entity.getPosition(), Sound.BUCKET_FILL_FISH);
            entity.close();
            return true;
        };
    }
}

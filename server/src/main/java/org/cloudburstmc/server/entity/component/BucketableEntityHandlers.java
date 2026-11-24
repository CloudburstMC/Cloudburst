package org.cloudburstmc.server.entity.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.entity.Bucketable;
import org.cloudburstmc.api.entity.component.InteractEntityHandler;
import org.cloudburstmc.api.entity.passive.SulfurCube;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.player.PlayerBucketEntityEvent;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.data.BucketEntityData;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.player.CloudPlayer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BucketableEntityHandlers {

    public static InteractEntityHandler capture(ItemType requiredBucket) {
        return (entity, player, item, _) -> {
            if (!(entity instanceof Bucketable bucketable) || !(player instanceof CloudPlayer cloudPlayer)
                    || item.getType() != requiredBucket || !entity.isAlive() || !player.isAlive()
                    || entity.getLevel() != player.getLevel()) {
                return false;
            }

            PlayerInventoryView inventory = player.getInventory();
            int selectedSlot = inventory.getSelectedSlot();
            if (!item.equals(inventory.getSelectedItem())) {
                return true;
            }

            CloudEntity cloudEntity = (CloudEntity) entity;
            ItemStack entityBucket = bucketable.getBaseBucketItem().toBuilder()
                    .setData(ItemDataComponents.BUCKET_ENTITY_DATA, new BucketEntityData(
                            cloudEntity.getHealth(), cloudEntity.isInvulnerable(), cloudEntity.isImmobile(),
                            entity instanceof SulfurCube cube && cube.isBaby()))
                    .build();

            Component name = entity.customName();
            if (name != null) {
                entityBucket = entityBucket.toBuilder()
                        .setData(ItemDataComponents.CUSTOM_NAME, BedrockLegacyTextSerializer.getInstance().serialize(name))
                        .build();
            }

            PlayerBucketEntityEvent event = new PlayerBucketEntityEvent(player, bucketable, item, entityBucket);
            cloudPlayer.getServer().getEventManager().fire(event);
            if (event.isCancelled() || !entity.isAlive() || !player.isAlive()
                    || entity.getLevel() != player.getLevel() || inventory.getSelectedSlot() != selectedSlot
                    || !item.equals(inventory.getSelectedItem())) {
                return true;
            }

            ItemStack result = event.getEntityBucket();
            if (!player.isCreative() && item.getCount() == 1) {
                inventory.setSelectedItem(result);
            } else {
                if (!player.isCreative()) {
                    inventory.setSelectedItem(item.withCount(item.getCount() - 1));
                }

                ItemStack[] remaining = inventory.addItem(result);
                for (ItemStack stack : remaining) {
                    cloudPlayer.dropItem(stack);
                }
            }

            cloudPlayer.getLevel().playSound(entity.getPosition(), SoundTypes.BUCKET_FILL_FISH);
            cloudEntity.remove(EntityRemoveCause.PICKUP);
            return true;
        };
    }
}

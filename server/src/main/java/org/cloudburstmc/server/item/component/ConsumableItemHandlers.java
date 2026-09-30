package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.event.player.PlayerItemConsumeEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.ConsumableComponent;
import org.cloudburstmc.api.item.component.FinishUseHandler;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class ConsumableItemHandlers {

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        int duration = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.USE_DURATION_TICKS);
        player.startUsingItem(item, duration);
        return item;
    };

    public static ItemStack finishConsumption(ItemStack item, CloudPlayer player) {
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, item);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            player.sendHeldItemSlot();
            player.sendAttributes();
            return item;
        }

        EntityEventPacket completed = new EntityEventPacket();
        completed.setRuntimeEntityId(player.getRuntimeId());
        completed.setType(EntityEventType.USE_ITEM);

        player.sendPacket(completed);
        CloudServer.broadcastPacket(player.getViewers(), completed);

        ItemStack consumed = event.getItem();
        ConsumableComponent consumable = consumed.isEmpty() ? null : CloudItemRegistry.get().getComponent(consumed.getType(), ItemBehaviors.CONSUMABLE);
        if (consumable != null) {
            player.getLevel().playSound(player.getPosition(), consumable.sound());
        }

        FinishUseHandler handler = consumed.isEmpty() ? null : CloudItemRegistry.get().getComponent(consumed.getType(), ItemBehaviors.FINISH_USE);
        ItemStack normalResult = handler == null ? consumed : handler.execute(consumed, player);
        if (consumable != null) {
            for (ConsumableComponent.Effect effect : consumable.effects()) {
                if (ThreadLocalRandom.current().nextFloat() < effect.probability()) {
                    player.addPotionEffect(effect.effect(), player, PotionEffectCause.FOOD);
                }
            }
        }

        return result(event, normalResult);
    }

    public static ItemStack result(PlayerItemConsumeEvent event, ItemStack normalResult) {
        ItemStack replacement = event.getReplacement();
        return replacement == null ? normalResult : replacement;
    }

    public static ItemStack afterConsumption(ItemStack item, CloudPlayer player) {
        if (player.isCreative()) {
            return item;
        }

        ConsumableComponent consumable = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.CONSUMABLE);
        if (consumable.remainder() == null) {
            return item.decreaseCount();
        }

        ItemStack remainder = ItemStack.from(consumable.remainder());
        if (item.getCount() == 1) {
            return remainder;
        }

        for (ItemStack leftover : player.getInventory().addItem(remainder)) {
            player.dropItem(leftover);
        }

        return item.decreaseCount();
    }
}

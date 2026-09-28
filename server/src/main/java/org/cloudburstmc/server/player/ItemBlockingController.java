package org.cloudburstmc.server.player;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.player.PlayerShieldDisableEvent;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.AttackBlockingComponent;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Objects;

public class ItemBlockingController {
    private final CloudPlayer player;
    private SlotGroup slots;
    private int slot = -1;
    private ItemStack raisedItem = ItemStack.EMPTY;
    private int raisedAt;
    private int interruptedUntil;

    public ItemBlockingController(CloudPlayer player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public void update() {
        int tick = this.player.getServer().getTick();
        if (!this.player.isAlive() || this.player.isSpectator() || this.player.isSleeping()
                || !this.player.isSneaking() || this.player.isUsingItem() || this.player.isBreakingBlock()
                || tick < this.interruptedUntil) {
            this.clear();
            return;
        }

        SlotGroup selectedSlots = this.player.getInventory();
        int selectedSlot = this.player.getSelectedHotbarSlot();
        ItemStack item = selectedSlots.getItem(selectedSlot);
        if (this.cannotRaise(item)) {
            selectedSlots = this.player.getOffhand();
            selectedSlot = 0;
            item = selectedSlots.getItem(selectedSlot);
        }

        if (this.cannotRaise(item)) {
            this.clear();
            return;
        }

        if (selectedSlots != this.slots || selectedSlot != this.slot || !item.hasSameDataComponents(this.raisedItem)) {
            this.raisedAt = tick;
        }

        this.slots = selectedSlots;
        this.slot = selectedSlot;
        this.raisedItem = item;
        this.player.getData().setFlag(EntityFlag.BLOCKING, true);
    }

    public ItemStack getBlockingItem() {
        this.update();
        if (this.raisedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        AttackBlockingComponent blocking = CloudItemRegistry.get().requireComponent(this.raisedItem.getType(), ItemBehaviors.BLOCKS_ATTACKS);
        return this.player.getServer().getTick() - this.raisedAt >= blocking.delayTicks() ? this.raisedItem : ItemStack.EMPTY;
    }

    public void interrupt() {
        PotionEffect haste = this.player.getPotionEffect(EffectTypes.HASTE);
        PotionEffect conduit = this.player.getPotionEffect(EffectTypes.CONDUIT_POWER);
        PotionEffect fatigue = this.player.getPotionEffect(EffectTypes.MINING_FATIGUE);
        int hasteLevel = Math.max(haste == null ? 0 : haste.getAmplifier() + 1, conduit == null ? 0 : conduit.getAmplifier() + 1);
        int duration = hasteLevel > 0 ? Math.max(1, 6 - hasteLevel) : 6 + (fatigue == null ? 0 : (fatigue.getAmplifier() + 1) * 2);
        this.interruptedUntil = this.player.getServer().getTick() + duration;
        this.clear();
    }

    public void onDamageBlocked(EntityDamageEvent event) {
        ItemStack item = this.getBlockingItem();
        if (item.isEmpty()) {
            return;
        }

        AttackBlockingComponent blocking = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.BLOCKS_ATTACKS);
        SlotGroup blockingSlots = Objects.requireNonNull(this.slots, "blocking slots");
        int blockingSlot = this.slot;
        this.player.getLevel().addLevelSoundEvent(this.player.getPosition(), SoundEvent.SHIELD_BLOCK);
        int durability = blocking.durabilityDamage(event.getBlockedDamage());
        if (durability > 0 && !this.player.isCreative()) {
            ItemStack damaged = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE)
                    .execute(item, durability, this.player);
            blockingSlots.setItem(blockingSlot, damaged);
            this.raisedItem = damaged;
        }

        Entity attacker = event.getDamageSource().getDirectEntity();
        if (attacker instanceof CloudPlayer attackingPlayer && !event.getDamageType().is(DamageTypeTags.IS_PROJECTILE)) {
            ItemStack weapon = attackingPlayer.getInventory().getSelectedItem();
            float seconds = CloudItemRegistry.get().requireComponent(weapon.getType(), ItemBehaviors.GET_BLOCKING_DISABLE_SECONDS).execute(weapon);
            int cooldown = Math.round(seconds * blocking.disableCooldownScale() * 20);
            if (cooldown > 0 && !this.raisedItem.isEmpty()) {
                PlayerShieldDisableEvent disable = new PlayerShieldDisableEvent(this.player, attacker, cooldown);
                this.player.getServer().getEventManager().fire(disable);
                if (!disable.isCancelled() && disable.getCooldown() > 0) {
                    this.player.setItemCooldown(item.getType(), disable.getCooldown());
                    this.player.getLevel().addSound(this.player.getPosition(), Sound.RANDOM_BREAK,
                            0.8f, 0.8f + this.player.getLevel().getRandom().nextFloat() * 0.4f);
                    this.clear();
                }
            }
        }

        this.update();
    }

    private boolean cannotRaise(ItemStack item) {
        return item.isEmpty() || CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.BLOCKS_ATTACKS) == null
                || this.player.getItemCooldown(item.getType()) != 0;
    }

    private void clear() {
        this.slots = null;
        this.slot = -1;
        this.raisedItem = ItemStack.EMPTY;
        this.player.getData().setFlag(EntityFlag.BLOCKING, false);
    }
}

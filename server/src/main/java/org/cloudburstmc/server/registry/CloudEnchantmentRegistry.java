package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.EnchantmentRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.enchantment.behavior.*;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageAll;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageArthropods;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageSmite;
import org.cloudburstmc.server.enchantment.behavior.protection.*;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentImpaling;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

public class CloudEnchantmentRegistry implements EnchantmentRegistry {

    private static final CloudEnchantmentRegistry INSTANCE = new CloudEnchantmentRegistry();

    private final Map<EnchantmentType, EnchantmentBehavior> behaviorMap = new Reference2ObjectOpenHashMap<>();
    private final BiMap<EnchantmentType, Short> idMap = HashBiMap.create();
    private final BiMap<EnchantmentType, Identifier> identifierMap = HashBiMap.create();
    private volatile boolean closed;

    private CloudEnchantmentRegistry() {
        this.registerVanillaEnchantments();
    }

    public static CloudEnchantmentRegistry get() {
        return INSTANCE;
    }

    public Enchantment getEnchantment(@NonNull EnchantmentType type, int level) {
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkArgument(level > 0, "level must be positive");
        getBehavior(type);

        return new Enchantment(type, level);
    }

    public boolean canEnchant(@NonNull Enchantment enchantment, @NonNull ItemStack item) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        Preconditions.checkNotNull(item, "item");
        return getBehavior(enchantment.type()).canEnchant(enchantment, item);
    }

    public boolean areCompatible(@NonNull Enchantment first, @NonNull Enchantment second) {
        Preconditions.checkNotNull(first, "first");
        Preconditions.checkNotNull(second, "second");
        return !first.type().conflictsWith(second.type());
    }

    public float getDamageProtection(@NonNull ItemStack item, @NonNull EntityDamageEvent event) {
        Preconditions.checkNotNull(item, "item");
        Preconditions.checkNotNull(event, "event");

        float protection = 0;
        for (Enchantment enchantment : item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).values()) {
            protection += getBehavior(enchantment.type()).getDamageProtection(enchantment, event);
        }

        return protection;
    }

    public float modifyDamage(@NonNull ItemStack item, @NonNull Entity target, float damage) {
        Preconditions.checkNotNull(item, "item");
        Preconditions.checkNotNull(target, "target");

        float modifiedDamage = damage;
        for (Enchantment enchantment : item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).values()) {
            modifiedDamage = getBehavior(enchantment.type()).modifyDamage(enchantment, target, modifiedDamage);
        }

        return modifiedDamage;
    }

    public float modifyKnockback(@NonNull ItemStack item, @NonNull Entity target, float knockback) {
        Preconditions.checkNotNull(item, "item");
        Preconditions.checkNotNull(target, "target");

        float modifiedKnockback = knockback;
        for (Enchantment enchantment : item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).values()) {
            modifiedKnockback = getBehavior(enchantment.type()).modifyKnockback(enchantment, target, modifiedKnockback);
        }

        return modifiedKnockback;
    }

    public void applyPostAttackEffects(@NonNull ItemStack item, @NonNull Entity attacker, @NonNull Entity target) {
        Preconditions.checkNotNull(item, "item");
        Preconditions.checkNotNull(attacker, "attacker");
        Preconditions.checkNotNull(target, "target");

        for (Enchantment enchantment : item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).values()) {
            getBehavior(enchantment.type()).onPostAttack(enchantment, attacker, target);
        }
    }

    public ItemStack applyPostHurtEffects(@NonNull ItemStack item, @NonNull Entity wearer, @NonNull Entity attacker) {
        Preconditions.checkNotNull(item, "item");
        Preconditions.checkNotNull(wearer, "wearer");
        Preconditions.checkNotNull(attacker, "attacker");

        ItemStack result = item;
        for (Enchantment enchantment : item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).values()) {
            result = getBehavior(enchantment.type()).onPostHurt(enchantment, result, wearer, attacker);
        }

        return result;
    }

    public @Nullable EnchantmentType getType(short id) {
        return idMap.inverse().get(id);
    }

    public short getSerializedId(EnchantmentType type) {
        Short id = idMap.get(type);
        Preconditions.checkArgument(id != null, "Unregistered enchantment type: %s", type);
        return id;
    }

    @Override
    public Identifier getId(EnchantmentType type) {
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkArgument(identifierMap.containsKey(type), "Unregistered enchantment type: %s", type);
        return type.identifier();
    }

    @Override
    public Optional<EnchantmentType> get(Identifier id) {
        return Optional.ofNullable(this.identifierMap.inverse().get(id));
    }

    @Override
    public Collection<EnchantmentType> values() {
        return List.copyOf(this.identifierMap.keySet());
    }

    @Override
    public synchronized void close() throws RegistryException {
        this.checkClosed();
        this.closed = true;
    }

    private void checkClosed() {
        checkState(!this.closed, "Registration is already closed");
    }

    private EnchantmentBehavior getBehavior(@NonNull EnchantmentType type) {
        Preconditions.checkNotNull(type, "type");
        EnchantmentBehavior behavior = behaviorMap.get(type);
        Preconditions.checkArgument(behavior != null, "Unregistered enchantment type: %s", type);
        return behavior;
    }

    private synchronized void registerVanilla(short id, @NonNull EnchantmentType type, @NonNull EnchantmentBehavior behavior) {
        this.checkClosed();
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(behavior, "behavior");
        Preconditions.checkState(!behaviorMap.containsKey(type), "Enchantment %s already registered", type);
        Preconditions.checkState(!idMap.inverse().containsKey(id), "Enchantment ID %s already registered", id);

        behaviorMap.put(type, behavior);
        idMap.put(type, id);
        identifierMap.put(type, type.identifier());
    }

    private void registerVanillaEnchantments() {
        this.registerVanilla((short) 0, EnchantmentTypes.PROTECTION, new EnchantmentProtectionAll());
        this.registerVanilla((short) 1, EnchantmentTypes.FIRE_PROTECTION, new EnchantmentProtectionFire());
        this.registerVanilla((short) 2, EnchantmentTypes.FEATHER_FALLING, new EnchantmentProtectionFall());
        this.registerVanilla((short) 3, EnchantmentTypes.BLAST_PROTECTION, new EnchantmentProtectionExplosion());
        this.registerVanilla((short) 4, EnchantmentTypes.PROJECTILE_PROTECTION, new EnchantmentProtectionProjectile());
        this.registerVanilla((short) 5, EnchantmentTypes.THORNS, new EnchantmentThorns());
        this.registerVanilla((short) 6, EnchantmentTypes.RESPIRATION, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 7, EnchantmentTypes.DEPTH_STRIDER, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 8, EnchantmentTypes.AQUA_AFFINITY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 9, EnchantmentTypes.SHARPNESS, new EnchantmentDamageAll());
        this.registerVanilla((short) 10, EnchantmentTypes.SMITE, new EnchantmentDamageSmite());
        this.registerVanilla((short) 11, EnchantmentTypes.BANE_OF_ARTHROPODS, new EnchantmentDamageArthropods());
        this.registerVanilla((short) 12, EnchantmentTypes.KNOCKBACK, new EnchantmentKnockback());
        this.registerVanilla((short) 13, EnchantmentTypes.FIRE_ASPECT, new EnchantmentFireAspect());
        this.registerVanilla((short) 14, EnchantmentTypes.LOOTING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 15, EnchantmentTypes.EFFICIENCY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 16, EnchantmentTypes.SILK_TOUCH, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 17, EnchantmentTypes.UNBREAKING, new EnchantmentDurability());
        this.registerVanilla((short) 18, EnchantmentTypes.FORTUNE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 19, EnchantmentTypes.POWER, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 20, EnchantmentTypes.PUNCH, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 21, EnchantmentTypes.FLAME, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 22, EnchantmentTypes.INFINITY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 23, EnchantmentTypes.LUCK_OF_THE_SEA, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 24, EnchantmentTypes.LURE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 25, EnchantmentTypes.FROST_WALKER, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 26, EnchantmentTypes.MENDING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 27, EnchantmentTypes.BINDING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 28, EnchantmentTypes.VANISHING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 29, EnchantmentTypes.IMPALING, new EnchantmentTridentImpaling());
        this.registerVanilla((short) 30, EnchantmentTypes.RIPTIDE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 31, EnchantmentTypes.LOYALTY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 32, EnchantmentTypes.CHANNELING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 33, EnchantmentTypes.MULTISHOT, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 34, EnchantmentTypes.PIERCING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 35, EnchantmentTypes.QUICK_CHARGE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 36, EnchantmentTypes.SOUL_SPEED, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 37, EnchantmentTypes.SWIFT_SNEAK, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 38, EnchantmentTypes.WIND_BURST, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 39, EnchantmentTypes.DENSITY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 40, EnchantmentTypes.BREACH, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla((short) 41, EnchantmentTypes.LUNGE, NoopEnchantmentBehavior.INSTANCE);
    }
}

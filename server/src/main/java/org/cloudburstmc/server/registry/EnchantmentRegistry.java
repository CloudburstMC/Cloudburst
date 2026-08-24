package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.Registry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.enchantment.behavior.*;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageAll;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageArthropods;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageSmite;
import org.cloudburstmc.server.enchantment.behavior.protection.*;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentImpaling;

import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

@Singleton
public class EnchantmentRegistry implements Registry {

    private static final EnchantmentRegistry INSTANCE;

    static {
        INSTANCE = new EnchantmentRegistry();
    }

    public static EnchantmentRegistry get() {
        return INSTANCE;
    }

    private final Map<EnchantmentType, EnchantmentBehavior> behaviorMap = new Reference2ObjectOpenHashMap<>();
    private final BiMap<EnchantmentType, Short> idMap = HashBiMap.create();
    private final BiMap<EnchantmentType, Identifier> identifierMap = HashBiMap.create();

    private volatile boolean closed;

    public EnchantmentRegistry() {
        this.registerVanillaEnchantments();
    }

    private synchronized void registerVanilla(@NonNull EnchantmentType type, @NonNull EnchantmentBehavior behavior) {
        this.checkClosed();
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(behavior, "behavior");
        Preconditions.checkState(!behaviorMap.containsKey(type), "Enchantment %s already registered", type);

        behaviorMap.put(type, behavior);
        idMap.put(type, type.id());
        identifierMap.put(type, type.identifier());
    }

    public Enchantment getEnchantment(@NonNull EnchantmentType type) {
        return getEnchantment(type, 1);
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

    public float getProtectionFactor(@NonNull Enchantment enchantment, @NonNull EntityDamageEvent event) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        Preconditions.checkNotNull(event, "event");
        return getBehavior(enchantment.type()).getProtectionFactor(enchantment, event);
    }

    public void doPostAttack(@NonNull Enchantment enchantment, @NonNull Entity entity, @NonNull Entity attacker) {
        Preconditions.checkNotNull(enchantment, "enchantment");
        Preconditions.checkNotNull(entity, "entity");
        Preconditions.checkNotNull(attacker, "attacker");
        getBehavior(enchantment.type()).doPostAttack(enchantment, entity, attacker);
    }

    private EnchantmentBehavior getBehavior(@NonNull EnchantmentType type) {
        Preconditions.checkNotNull(type, "type");
        EnchantmentBehavior behavior = behaviorMap.get(type);
        Preconditions.checkArgument(behavior != null, "Unregistered enchantment type: %s", type);
        return behavior;
    }

    public EnchantmentType getType(short id) {
        return idMap.inverse().get(id);
    }

    public EnchantmentType getType(Identifier id) {
        return identifierMap.inverse().get(id);
    }

    @Override
    public synchronized void close() throws RegistryException {
        this.checkClosed();
        this.closed = true;
    }

    private void checkClosed() {
        checkState(!this.closed, "Registration is already closed");
    }

    private void registerVanillaEnchantments() {
        this.registerVanilla(EnchantmentTypes.PROTECTION, new EnchantmentProtectionAll());
        this.registerVanilla(EnchantmentTypes.FIRE_PROTECTION, new EnchantmentProtectionFire());
        this.registerVanilla(EnchantmentTypes.FEATHER_FALLING, new EnchantmentProtectionFall());
        this.registerVanilla(EnchantmentTypes.BLAST_PROTECTION, new EnchantmentProtectionExplosion());
        this.registerVanilla(EnchantmentTypes.PROJECTILE_PROTECTION, new EnchantmentProtectionProjectile());
        this.registerVanilla(EnchantmentTypes.THORNS, new EnchantmentThorns());
        this.registerVanilla(EnchantmentTypes.RESPIRATION, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.DEPTH_STRIDER, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.AQUA_AFFINITY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.SHARPNESS, new EnchantmentDamageAll());
        this.registerVanilla(EnchantmentTypes.SMITE, new EnchantmentDamageSmite());
        this.registerVanilla(EnchantmentTypes.BANE_OF_ARTHROPODS, new EnchantmentDamageArthropods());
        this.registerVanilla(EnchantmentTypes.KNOCKBACK, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.FIRE_ASPECT, new EnchantmentFireAspect());
        this.registerVanilla(EnchantmentTypes.LOOTING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.EFFICIENCY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.SILK_TOUCH, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.UNBREAKING, new EnchantmentDurability());
        this.registerVanilla(EnchantmentTypes.FORTUNE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.POWER, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.PUNCH, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.FLAME, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.INFINITY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.LUCK_OF_THE_SEA, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.LURE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.FROST_WALKER, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.MENDING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.BINDING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.VANISHING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.IMPALING, new EnchantmentTridentImpaling());
        this.registerVanilla(EnchantmentTypes.RIPTIDE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.LOYALTY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.CHANNELING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.MULTISHOT, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.PIERCING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.QUICK_CHARGE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.SOUL_SPEED, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.SWIFT_SNEAK, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.WIND_BURST, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.DENSITY, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.BREACH, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.LUNGE, NoopEnchantmentBehavior.INSTANCE);
    }
}

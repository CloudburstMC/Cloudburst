package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.enchantment.behavior.EnchantmentBehavior;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.behavior.*;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowFlame;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowInfinity;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowKnockback;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowPower;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageAll;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageArthropods;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageSmite;
import org.cloudburstmc.server.enchantment.behavior.protection.*;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentChanneling;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentImpaling;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentLoyalty;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentRiptide;

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

    public synchronized void register(@NonNull EnchantmentType type, @NonNull EnchantmentBehavior behavior) {
        throw new UnsupportedOperationException("Custom enchantments are not currently supported!");
    }

    private synchronized void registerVanilla(@NonNull EnchantmentType type, @NonNull EnchantmentBehavior behavior) {
        this.checkClosed();
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(behavior, "behavior");
        Preconditions.checkState(!behaviorMap.containsKey(type), "Enchantment %s already registered", type);

        behaviorMap.put(type, behavior);
        idMap.put(type, type.getId());
        identifierMap.put(type, type.getType());
    }

    public EnchantmentInstance getEnchantment(@NonNull EnchantmentType type) {
        return getEnchantment(type, 1);
    }

    public EnchantmentInstance getEnchantment(@NonNull EnchantmentType type, int level) {
        Preconditions.checkNotNull(type, "type");

        return new CloudEnchantmentInstance(type, level);
    }

    public EnchantmentBehavior getBehavior(@NonNull EnchantmentType type) {
        Preconditions.checkNotNull(type, "type");
        return behaviorMap.get(type);
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
        this.registerVanilla(EnchantmentTypes.RESPIRATION, new EnchantmentWaterBreath());
        this.registerVanilla(EnchantmentTypes.DEPTH_STRIDER, new EnchantmentWaterWalker());
        this.registerVanilla(EnchantmentTypes.AQUA_AFFINITY, new EnchantmentWaterWorker());
        this.registerVanilla(EnchantmentTypes.SHARPNESS, new EnchantmentDamageAll());
        this.registerVanilla(EnchantmentTypes.SMITE, new EnchantmentDamageSmite());
        this.registerVanilla(EnchantmentTypes.BANE_OF_ARTHROPODS, new EnchantmentDamageArthropods());
        this.registerVanilla(EnchantmentTypes.KNOCKBACK, new EnchantmentKnockback());
        this.registerVanilla(EnchantmentTypes.FIRE_ASPECT, new EnchantmentFireAspect());
        this.registerVanilla(EnchantmentTypes.LOOTING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.EFFICIENCY, new EnchantmentEfficiency());
        this.registerVanilla(EnchantmentTypes.SILK_TOUCH, new EnchantmentSilkTouch());
        this.registerVanilla(EnchantmentTypes.UNBREAKING, new EnchantmentDurability());
        this.registerVanilla(EnchantmentTypes.FORTUNE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.POWER, new EnchantmentBowPower());
        this.registerVanilla(EnchantmentTypes.PUNCH, new EnchantmentBowKnockback());
        this.registerVanilla(EnchantmentTypes.FLAME, new EnchantmentBowFlame());
        this.registerVanilla(EnchantmentTypes.INFINITY, new EnchantmentBowInfinity());
        this.registerVanilla(EnchantmentTypes.LUCK_OF_THE_SEA, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.LURE, new EnchantmentLure());
        this.registerVanilla(EnchantmentTypes.FROST_WALKER, new EnchantmentFrostWalker());
        this.registerVanilla(EnchantmentTypes.MENDING, new EnchantmentMending());
        this.registerVanilla(EnchantmentTypes.BINDING, new EnchantmentBindingCurse());
        this.registerVanilla(EnchantmentTypes.VANISHING, new EnchantmentVanishingCurse());
        this.registerVanilla(EnchantmentTypes.IMPALING, new EnchantmentTridentImpaling());
        this.registerVanilla(EnchantmentTypes.RIPTIDE, new EnchantmentTridentRiptide());
        this.registerVanilla(EnchantmentTypes.LOYALTY, new EnchantmentTridentLoyalty());
        this.registerVanilla(EnchantmentTypes.CHANNELING, new EnchantmentTridentChanneling());
        this.registerVanilla(EnchantmentTypes.MULTISHOT, NoopEnchantmentBehavior.INSTANCE); //TODO: implement
        this.registerVanilla(EnchantmentTypes.PIERCING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.QUICK_CHARGE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.SOUL_SPEED, NoopEnchantmentBehavior.INSTANCE);
    }
}

package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.NonNull;
import org.cloudburstmc.server.enchantment.CloudEnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentType;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.enchantment.EnchantmentTypes.CloudEnchantmentType;
import org.cloudburstmc.server.enchantment.behavior.*;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowFlame;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowInfinity;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowKnockback;
import org.cloudburstmc.server.enchantment.behavior.bow.EnchantmentBowPower;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageAll;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageArthropods;
import org.cloudburstmc.server.enchantment.behavior.damage.EnchantmentDamageSmite;
import org.cloudburstmc.server.enchantment.behavior.protection.*;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentImpaling;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentLoyalty;
import org.cloudburstmc.server.enchantment.behavior.trident.EnchantmentTridentRiptide;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
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

    public synchronized void register(@NonNull EnchantmentType type, @Nonnull EnchantmentBehavior behavior) {
        throw new UnsupportedOperationException("Custom enchantments are not currently supported!");
    }

    private synchronized void registerVanilla(@NonNull EnchantmentType type, @Nonnull EnchantmentBehavior behavior) {
        this.checkClosed();
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(behavior, "behavior");
        Preconditions.checkState(!behaviorMap.containsKey(type), "Enchantment %s already registered", type);

        behaviorMap.put(type, behavior);
        idMap.put(type, ((CloudEnchantmentType) type).getId());
        identifierMap.put(type, type.getType());
    }

    public EnchantmentInstance getEnchantment(@Nonnull EnchantmentType type) {
        return getEnchantment(type, 1);
    }

    public EnchantmentInstance getEnchantment(@Nonnull EnchantmentType type, int level) {
        Preconditions.checkNotNull(type, "type");

        return new CloudEnchantmentInstance(type, level);
    }

    public EnchantmentBehavior getBehavior(@Nonnull EnchantmentType type) {
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
        this.registerVanilla(EnchantmentTypes.FALL_PROTECTION, new EnchantmentProtectionFall());
        this.registerVanilla(EnchantmentTypes.EXPLOSION_PROTECTION, new EnchantmentProtectionExplosion());
        this.registerVanilla(EnchantmentTypes.PROJECTILE_PROTECTION, new EnchantmentProtectionProjectile());
        this.registerVanilla(EnchantmentTypes.THORNS, new EnchantmentThorns());
        this.registerVanilla(EnchantmentTypes.WATER_BREATHING, new EnchantmentWaterBreath());
        this.registerVanilla(EnchantmentTypes.WATER_WORKER, new EnchantmentWaterWorker());
        this.registerVanilla(EnchantmentTypes.WATER_WALKER, new EnchantmentWaterWalker());
        this.registerVanilla(EnchantmentTypes.DAMAGE_SHARPNESS, new EnchantmentDamageAll());
        this.registerVanilla(EnchantmentTypes.DAMAGE_SMITE, new EnchantmentDamageSmite());
        this.registerVanilla(EnchantmentTypes.DAMAGE_ARTHOPODS, new EnchantmentDamageArthropods());
        this.registerVanilla(EnchantmentTypes.KNOCKBACK, new EnchantmentKnockback());
        this.registerVanilla(EnchantmentTypes.FIRE_ASPECT, new EnchantmentFireAspect());
        this.registerVanilla(EnchantmentTypes.LOOTING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.EFFICIENCY, new EnchantmentEfficiency());
        this.registerVanilla(EnchantmentTypes.SILK_TOUCH, new EnchantmentSilkTouch());
        this.registerVanilla(EnchantmentTypes.DURABILITY, new EnchantmentDurability());
        this.registerVanilla(EnchantmentTypes.FORTUNE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.BOW_POWER, new EnchantmentBowPower());
        this.registerVanilla(EnchantmentTypes.BOW_PUNCH, new EnchantmentBowKnockback());
        this.registerVanilla(EnchantmentTypes.BOW_FLAME, new EnchantmentBowFlame());
        this.registerVanilla(EnchantmentTypes.BOW_INFINITY, new EnchantmentBowInfinity());
        this.registerVanilla(EnchantmentTypes.LURE, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.FROST_WALKER, new EnchantmentLure());
        this.registerVanilla(EnchantmentTypes.MENDING, new EnchantmentFrostWalker());
        this.registerVanilla(EnchantmentTypes.BINDING_CURSE, new EnchantmentMending());
        this.registerVanilla(EnchantmentTypes.VANISHING_CURSE, new EnchantmentBindingCurse());
        this.registerVanilla(EnchantmentTypes.TRIDENT_IMPALING, new EnchantmentVanishingCurse());
        this.registerVanilla(EnchantmentTypes.TRIDENT_RIPTIDE, new EnchantmentTridentImpaling());
        this.registerVanilla(EnchantmentTypes.TRIDENT_LOYALTY, new EnchantmentTridentRiptide());
        this.registerVanilla(EnchantmentTypes.TRIDENT_CHANNELING, new EnchantmentTridentLoyalty());
        this.registerVanilla(EnchantmentTypes.CROSSBOW_MULTISHOT, NoopEnchantmentBehavior.INSTANCE); //TODO: implement
        this.registerVanilla(EnchantmentTypes.CROSSBOW_PIERCING, NoopEnchantmentBehavior.INSTANCE);
        this.registerVanilla(EnchantmentTypes.CROSSBOW_QUICK_CHARGE, NoopEnchantmentBehavior.INSTANCE);
    }
}

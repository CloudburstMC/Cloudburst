package org.cloudburstmc.server.player;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.entity.EntityLiving;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@UtilityClass
class DeathMessageResolver {

    private static final String GENERIC = "death.attack.generic";
    private static final Map<DamageType, String> SIMPLE_MESSAGES = Map.ofEntries(
            Map.entry(DamageTypes.VOID, "death.attack.outOfWorld"),
            Map.entry(DamageTypes.SUFFOCATION, "death.attack.inWall"),
            Map.entry(DamageTypes.FIRE, "death.attack.onFire"),
            Map.entry(DamageTypes.FIRE_TICK, "death.attack.inFire"),
            Map.entry(DamageTypes.DROWNING, "death.attack.drown"),
            Map.entry(DamageTypes.FREEZING, "death.attack.freeze"),
            Map.entry(DamageTypes.MAGIC, "death.attack.magic"),
            Map.entry(DamageTypes.HUNGER, "death.attack.starve")
    );

    static Resolution resolve(CloudPlayer victim, @Nullable EntityDamageEvent event) {
        if (event == null) {
            return message(victim, GENERIC, null);
        }

        DamageSource source = event.getDamageSource();
        DamageType type = source.getDamageType();
        Entity attacker = source.getCausingEntity();
        if (type == DamageTypes.ENTITY_ATTACK) {
            return entityAttack(victim, attacker);
        }

        if (type == DamageTypes.PROJECTILE) {
            return projectile(victim, attacker);
        }

        if (type.is(DamageTypeTags.IS_EXPLOSION)) {
            return explosion(victim, attacker);
        }

        if (type == DamageTypes.THORNS) {
            return thorns(victim, attacker);
        }

        if (type == DamageTypes.FALL) {
            return message(victim, event.getDamage() > 2
                    ? "death.fell.accident.generic" : "death.attack.fall", null);
        }

        if (type == DamageTypes.LAVA) {
            return lava(victim);
        }

        if (type == DamageTypes.CONTACT) {
            return contact(victim, source.getBlock());
        }

        return message(victim, SIMPLE_MESSAGES.getOrDefault(type, GENERIC), null);
    }

    private static Resolution entityAttack(CloudPlayer victim, @Nullable Entity attacker) {
        if (attacker instanceof CloudPlayer player) {
            return message(victim, "death.attack.player", attacker, player.displayName());
        }

        if (attacker instanceof EntityLiving) {
            return message(victim, "death.attack.mob", attacker, displayName(attacker));
        }

        return message(victim, GENERIC, attacker);
    }

    private static Resolution projectile(CloudPlayer victim, @Nullable Entity attacker) {
        if (attacker instanceof CloudPlayer player) {
            return message(victim, "death.attack.arrow", attacker, player.displayName());
        }

        if (attacker instanceof EntityLiving) {
            return message(victim, "death.attack.arrow", attacker, displayName(attacker));
        }

        return message(victim, GENERIC, attacker);
    }

    private static Resolution explosion(CloudPlayer victim, @Nullable Entity attacker) {
        if (attacker instanceof CloudPlayer player) {
            return message(victim, "death.attack.explosion.player", attacker, player.displayName());
        }

        if (attacker instanceof EntityLiving) {
            return message(victim, "death.attack.explosion.player", attacker, displayName(attacker));
        }

        return message(victim, "death.attack.explosion", attacker);
    }

    private static Resolution thorns(CloudPlayer victim, @Nullable Entity attacker) {
        if (attacker == null) {
            return message(victim, GENERIC, null);
        }

        Component attackerName = attacker instanceof CloudPlayer player ? player.displayName() : displayName(attacker);
        return message(victim, "death.attack.thorns", attacker, attackerName);
    }

    private static Resolution lava(CloudPlayer victim) {
        boolean magma = victim.getLevel().getBlockState(victim.getPosition().add(0, -1, 0).toInt()).getType()
                == BlockTypes.MAGMA;
        return message(victim, magma ? "death.attack.magma" : "death.attack.lava", null);
    }

    private static Resolution contact(CloudPlayer victim, @Nullable Block block) {
        String key = block != null && block.getState().getType() == BlockTypes.CACTUS ? "death.attack.cactus" : GENERIC;
        return message(victim, key, null);
    }

    private static Resolution message(CloudPlayer victim, String key, @Nullable Entity killer, Component... additionalParameters) {
        Component[] parameters = new Component[additionalParameters.length + 1];
        parameters[0] = victim.displayName();
        System.arraycopy(additionalParameters, 0, parameters, 1, additionalParameters.length);
        return new Resolution(Component.translatable(key, List.of(parameters)), killer);
    }

    private static Component displayName(Entity entity) {
        return Objects.equals(entity.getNameTag(), "")
                ? Component.text(entity.getName())
                : Component.text(entity.getNameTag());
    }

    public record Resolution(Component message, @Nullable Entity killer) {
    }
}

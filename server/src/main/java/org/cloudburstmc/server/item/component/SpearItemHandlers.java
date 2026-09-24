package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.misc.EnderCrystal;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.*;
import org.cloudburstmc.api.level.BlockShapeMode;
import org.cloudburstmc.api.level.FluidCollisionMode;
import org.cloudburstmc.api.level.RayTraceContext;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.server.item.component.ProjectileWeaponSupport.enchantmentLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpearItemHandlers {

    private static final float STAB_REACH = 4.5f;
    private static final float CREATIVE_STAB_REACH = 7.5f;
    private static final float MINIMUM_REACH = 2.0f;
    private static final float STAB_HITBOX_MARGIN = 0.25f;
    private static final int CONTACT_COOLDOWN_TICKS = 10;
    private static final Map<CloudPlayer, Map<Long, Integer>> RECENT_CONTACTS = new WeakHashMap<>();
    private static final Map<CloudPlayer, Integer> LAST_STAB = new WeakHashMap<>();

    public static final ReleaseUseHandler RELEASE = (item, holder, ticksUsed) -> {
        if (holder instanceof CloudPlayer player) {
            RECENT_CONTACTS.remove(player);
        }

        return item;
    };

    public static UseHandler use(SoundEvent useSound) {
        requireNonNull(useSound, "useSound");
        return (item, holder) -> {
            if (holder instanceof CloudPlayer player && !player.isSpectator()) {
                RECENT_CONTACTS.remove(player);
                player.startUsingItem(item);
                playUseSoundToViewers(player, useSound);
            }

            return item;
        };
    }

    public static UseTickHandler kinetic(SpearProfile profile, SoundEvent hitSound) {
        requireNonNull(profile, "profile");
        requireNonNull(hitSound, "hitSound");
        return (item, holder, ticksUsed) -> {
            if (holder instanceof CloudPlayer player && !player.isSpectator()) {
                applyKineticAttack(player, profile, hitSound, ticksUsed);
            }

            return UseTickResult.continueUsing(item);
        };
    }

    private static void applyKineticAttack(CloudPlayer player, SpearProfile profile, SoundEvent hitSound, int ticksUsed) {
        int activeTicks = ticksUsed - profile.delayTicks();
        if (activeTicks < 0 || activeTicks > Math.max(profile.damageTicks(), Math.max(profile.knockbackTicks(), profile.dismountTicks()))) {
            return;
        }

        Vector3f look = player.getDirectionVector();
        float forwardSpeed = look.dot(player.getKnownMovement()) * 20;
        Map<Long, Integer> contacts = RECENT_CONTACTS.computeIfAbsent(player, ignored -> new HashMap<>());
        contacts.entrySet().removeIf(entry -> ticksUsed - entry.getValue() >= CONTACT_COOLDOWN_TICKS);
        boolean hitAnything = false;

        for (Entity target : targets(player)) {
            if (contacts.containsKey(target.getUniqueId())) {
                continue;
            }

            Vector3f targetMovement = target instanceof CloudPlayer other ? other.getKnownMovement() : target.getMotion();
            float relativeSpeed = Math.max(0, forwardSpeed - look.dot(targetMovement) * 20);
            boolean dismount = activeTicks <= profile.dismountTicks() && forwardSpeed >= profile.dismountSpeed();
            boolean knockback = activeTicks <= profile.knockbackTicks() && forwardSpeed >= profile.knockbackSpeed();
            boolean damage = activeTicks <= profile.damageTicks() && relativeSpeed >= profile.damageSpeed();
            if (!dismount && !knockback && !damage) {
                continue;
            }

            contacts.put(target.getUniqueId(), ticksUsed);
            if (dismount && target.getVehicle() != null) {
                target.dismount(target.getVehicle());
            }

            if (knockback && target instanceof EntityLiving living) {
                living.knockBack(player, 1.0f, look.getX(), look.getZ());
            }

            boolean affected = knockback || dismount && target.getVehicle() != null;
            if (damage) {
                affected |= player.attackWithBonusDamage(target, (float) Math.floor(relativeSpeed * profile.damageMultiplier()));
            }

            hitAnything |= affected;
        }

        if (hitAnything) {
            player.getLevel().addLevelSoundEvent(player.getPosition(), hitSound);
            swing(player);
        }
    }

    public static StabHandler stab(SpearProfile profile, SoundEvent hitSound, SoundEvent missSound) {
        requireNonNull(profile, "profile");
        requireNonNull(hitSound, "hitSound");
        requireNonNull(missSound, "missSound");
        return (item, holder) -> stab(item, holder, profile, hitSound, missSound);
    }

    private static void stab(ItemStack item, Entity holder, SpearProfile profile, SoundEvent hitSound, SoundEvent missSound) {
        if (!(holder instanceof CloudPlayer player) || player.isSpectator()) {
            return;
        }

        int now = player.getServer().getTick();
        int previous = LAST_STAB.getOrDefault(player, Integer.MIN_VALUE);
        if (previous != Integer.MIN_VALUE && now - previous < profile.stabCooldownTicks()) {
            return;
        }
        LAST_STAB.put(player, now);

        boolean damaged = false;
        Vector3f look = player.getDirectionVector();
        for (Entity target : targets(player)) {
            if (player.attack(target)) {
                damaged = true;
                if (target instanceof EntityLiving living) {
                    living.knockBack(player, 1.0f, look.getX(), look.getZ());
                }
            }
        }

        if (damaged) {
            player.getLevel().addLevelSoundEvent(player.getPosition(), hitSound);
        } else {
            player.getLevel().addLevelSoundEvent(player.getPosition(), missSound);
        }
        swing(player);

        int lungeLevel = enchantmentLevel(item, EnchantmentTypes.LUNGE);
        if (lungeLevel == 0 || player.getVehicle() != null || player.isGliding()
                || player.isInsideOfWater() || (!player.isCreative() && player.getFoodData().getLevel() <= 6)) {
            return;
        }

        Vector3f horizontal = Vector3f.from(look.getX(), 0, look.getZ());
        if (horizontal.lengthSquared() < 0.000001f) {
            return;
        }

        Vector3f motion = player.getKnownMovement().add(horizontal.normalize().mul(0.458f * lungeLevel));
        player.setMotion(motion);
        player.getFoodData().updateFoodExpLevel(4.0f * lungeLevel);
        player.getLevel().addLevelSoundEvent(player.getPosition(), switch (Math.min(lungeLevel, 3)) {
            case 1 -> SoundEvent.LUNGE_1;
            case 2 -> SoundEvent.LUNGE_2;
            default -> SoundEvent.LUNGE_3;
        });

        ItemStack held = player.getInventory().getSelectedItem();
        if (held.isEmpty() || held.getType() != item.getType() || player.isCreative()) {
            return;
        }

        DamageItemHandler damage = CloudItemRegistry.get().requireComponent(held.getType(), ItemBehaviors.ON_DAMAGE);
        player.getInventory().setSelectedItem(damage.execute(held, 1, player));
    }

    private static List<Entity> targets(CloudPlayer player) {
        Vector3f look = player.getDirectionVector();
        Vector3f eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vector3f start = eye.add(look.mul(MINIMUM_REACH));

        float reach = player.isCreative() ? CREATIVE_STAB_REACH : STAB_REACH;
        float forwardMotion = Math.max(0, look.dot(player.getKnownMovement()));

        Vector3f end = eye.add(look.mul(reach + forwardMotion));
        HitResult blockHit = player.getLevel().rayTraceBlocks(new RayTraceContext(eye, end,
                BlockShapeMode.COLLIDER, FluidCollisionMode.NONE, CollisionContext.of(player)));
        if (blockHit instanceof BlockHitResult hit) {
            end = hit.position();
        }

        if (eye.distanceSquared(end) < eye.distanceSquared(start)) {
            return List.of();
        }

        List<Entity> targets = new ArrayList<>();
        for (Entity target : player.getLevel().getNearbyEntities(new BoundingBox(start, end).inflate(1.25f, 1.25f, 1.25f))) {
            if (target == player || !target.isAlive()
                    || !(target instanceof Living || target instanceof EnderCrystal)
                    || (target instanceof CloudPlayer other && other.isSpectator())) {
                continue;
            }

            BoundingBox hitbox = target.getBoundingBox().inflate(STAB_HITBOX_MARGIN, STAB_HITBOX_MARGIN, STAB_HITBOX_MARGIN);
            if (hitbox.contains(start) || hitbox.intersectSegment(start, end) != null) {
                targets.add(target);
            }
        }

        targets.sort(Comparator.comparingDouble(target -> eye.distanceSquared(target.getPosition())));
        return targets;
    }

    private static void swing(CloudPlayer player) {
        AnimatePacket animation = new AnimatePacket();
        animation.setAction(AnimatePacket.Action.SWING_ARM);
        animation.setRuntimeEntityId(player.getRuntimeId());
        CloudServer.broadcastPacket(player.getViewers(), animation);
    }

    private static void playUseSoundToViewers(CloudPlayer player, SoundEvent sound) {
        LevelSoundEventPacket packet = new LevelSoundEventPacket();
        packet.setSound(sound);
        packet.setExtraData(-1);
        packet.setIdentifier(Identifier.EMPTY.toString());
        packet.setPosition(player.getPosition());
        CloudServer.broadcastPacket(player.getViewers(), packet);
    }
}

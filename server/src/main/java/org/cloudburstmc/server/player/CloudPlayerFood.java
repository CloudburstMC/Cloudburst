package org.cloudburstmc.server.player;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.event.player.PlayerExhaustionEvent;
import org.cloudburstmc.api.event.player.PlayerFoodLevelChangeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.ExhaustionReason;

import static java.util.Objects.requireNonNull;

public class CloudPlayerFood {

    public static final int MAX_LEVEL = 20;
    private static final float EXHAUSTION_THRESHOLD = 4.0f;

    private final CloudPlayer player;
    @Getter
    private int level = MAX_LEVEL;
    @Getter
    private float saturation = 5.0f;
    @Getter
    private float exhaustion;
    @Getter
    private int tickTimer;

    public CloudPlayerFood(CloudPlayer player) {
        this.player = player;
    }

    public void setLevel(int level) {
        this.setFood(level, this.saturation);
    }

    public void setSaturation(float saturation) {
        this.setFood(this.level, saturation);
    }

    public void setExhaustion(float exhaustion) {
        float previous = Math.min(this.exhaustion, 5.0f);
        this.exhaustion = Float.isFinite(exhaustion) ? Math.clamp(exhaustion, 0.0f, 40.0f) : 0.0f;
        float current = Math.min(this.exhaustion, 5.0f);
        if (this.player.spawned && current != previous) {
            this.player.setAttribute(Attribute.getAttribute(Attribute.EXHAUSTION).setValue(current));
        }
    }

    public void setTickTimer(int tickTimer) {
        this.tickTimer = Math.max(0, tickTimer);
    }

    public boolean isFull() {
        return this.level >= MAX_LEVEL;
    }

    public void load(int level, float saturation, float exhaustion, int tickTimer) {
        this.level = Math.clamp(level, 0, MAX_LEVEL);
        this.saturation = Float.isFinite(saturation) ? Math.clamp(saturation, 0.0f, MAX_LEVEL) : 0.0f;
        this.setExhaustion(exhaustion);
        this.setTickTimer(tickTimer);
    }

    public void reset() {
        this.load(MAX_LEVEL, 5.0f, 0.0f, 0);
        this.sendAttributes();
    }

    public void eat(int nutrition, float saturation) {
        this.eat(nutrition, saturation, null);
    }

    public void eat(int nutrition, float saturation, @Nullable ItemStack item) {
        if (nutrition < 0 || !Float.isFinite(saturation) || saturation < 0.0f) {
            throw new IllegalArgumentException("Food nutrition and saturation must be non-negative");
        }

        int nextLevel = (int) Math.min(MAX_LEVEL, (long) this.level + nutrition);
        this.setFood(nextLevel, Math.min(nextLevel, this.saturation + saturation), item);
    }

    private void setFood(int proposedLevel, float proposedSaturation) {
        this.setFood(proposedLevel, proposedSaturation, null);
    }

    private void setFood(int proposedLevel, float proposedSaturation, @Nullable ItemStack item) {
        PlayerFoodLevelChangeEvent event = new PlayerFoodLevelChangeEvent(
                this.player, Math.clamp(proposedLevel, 0, MAX_LEVEL),
                Float.isFinite(proposedSaturation) ? Math.clamp(proposedSaturation, 0.0f, MAX_LEVEL) : 0.0f, item);
        this.player.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            this.sendAttributes();
            return;
        }

        int nextLevel = Math.clamp(event.getFoodLevel(), 0, MAX_LEVEL);
        float nextSaturation = event.getSaturation();
        float oldSaturation = this.saturation;
        this.saturation = Float.isFinite(nextSaturation) ? Math.clamp(nextSaturation, 0.0f, MAX_LEVEL) : 0.0f;

        if (this.level > 6 && nextLevel <= 6 && this.player.isSprinting()) {
            this.player.setSprinting(false);
        }

        if (this.level != nextLevel || this.saturation != oldSaturation) {
            this.level = nextLevel;
            this.sendAttributes();
        }
    }

    public void sendAttributes() {
        this.player.sendFoodAttributes();
    }

    public void addExhaustion(float amount, ExhaustionReason reason) {
        requireNonNull(reason, "reason");
        if (!this.player.isFoodEnabled() || amount < 0.0f || !Float.isFinite(amount)) {
            return;
        }

        PlayerExhaustionEvent event = new PlayerExhaustionEvent(this.player, reason, amount);
        this.player.getServer().getEventManager().fire(event);
        if (!event.isCancelled() && event.getAmount() > 0.0f) {
            this.setExhaustion(this.exhaustion + event.getAmount());
        }
    }

    public void update(int ticks) {
        if (!this.player.isFoodEnabled() || !this.player.isAlive()) {
            return;
        }

        Difficulty difficulty = this.player.getServer().getDifficulty();
        for (int i = 0; i < ticks && this.player.isAlive(); i++) {
            if (this.exhaustion > EXHAUSTION_THRESHOLD) {
                this.setExhaustion(this.exhaustion - EXHAUSTION_THRESHOLD);
                if (this.saturation > 0.0f) {
                    this.setSaturation(Math.max(0.0f, this.saturation - 1.0f));
                } else if (difficulty != Difficulty.PEACEFUL) {
                    this.setLevel(this.level - 1);
                }
            }

            boolean naturalRegen = this.player.getLevel().getGameRules().get(GameRules.NATURAL_REGENERATION);
            boolean hurt = this.player.getHealth() < this.player.getMaxHealth();
            if (naturalRegen && hurt && this.level == MAX_LEVEL && this.saturation > 0.0f) {
                if (++this.tickTimer >= 10) {
                    float spent = Math.min(this.saturation, 6.0f);
                    this.player.heal(new EntityRegainHealthEvent(this.player, spent / 6.0f, EntityRegainHealthEvent.CAUSE_EATING));
                    this.addExhaustion(spent, ExhaustionReason.REGEN);
                    this.tickTimer = 0;
                }
            } else if (naturalRegen && hurt && this.level >= 18) {
                if (++this.tickTimer >= 80) {
                    this.player.heal(new EntityRegainHealthEvent(this.player, 1.0f, EntityRegainHealthEvent.CAUSE_EATING));
                    this.addExhaustion(6.0f, ExhaustionReason.REGEN);
                    this.tickTimer = 0;
                }
            } else if (this.level == 0) {
                if (++this.tickTimer >= 80) {
                    float health = this.player.getHealth();
                    if (health > 10.0f || difficulty == Difficulty.HARD || health > 1.0f && difficulty == Difficulty.NORMAL) {
                        this.player.damage(1, DamageSource.of(DamageTypes.STARVE));
                    }
                    this.tickTimer = 0;
                }
            } else {
                this.tickTimer = 0;
            }
        }
    }
}

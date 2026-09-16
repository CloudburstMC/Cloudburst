package org.cloudburstmc.api.entity.damage;

import lombok.EqualsAndHashCode;
import org.cloudburstmc.api.util.Identifier;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Describes the behavior shared by damage from the same source type.
 */
@EqualsAndHashCode(of = "id")
public final class DamageType {

    private final Identifier id;
    private final String messageId;
    private final DamageScaling scaling;
    private final float exhaustion;
    private final DamageEffect effect;
    private final DeathMessageType deathMessageType;
    private final Set<DamageTypeTag> tags;

    /**
     * Creates a damage type.
     *
     * @param id               the stable identifier
     * @param messageId        the suffix used by death-message translation keys
     * @param scaling          the difficulty scaling rule
     * @param exhaustion       the hunger exhaustion caused by the damage
     * @param effect           the client feedback effect
     * @param deathMessageType the death-message rule
     * @param tags             the behavioral tags
     */
    public DamageType(Identifier id, String messageId, DamageScaling scaling, float exhaustion, DamageEffect effect, DeathMessageType deathMessageType, Set<DamageTypeTag> tags) {
        this.id = requireNonNull(id, "id");
        this.messageId = requireNonNull(messageId, "messageId");
        this.scaling = requireNonNull(scaling, "scaling");
        if (!Float.isFinite(exhaustion) || exhaustion < 0) {
            throw new IllegalArgumentException("exhaustion must be finite and non-negative");
        }
        this.exhaustion = exhaustion;
        this.effect = requireNonNull(effect, "effect");
        this.deathMessageType = requireNonNull(deathMessageType, "deathMessageType");
        this.tags = Set.copyOf(requireNonNull(tags, "tags"));
    }

    /**
     * Returns the identifier of this damage type.
     *
     * @return the damage type identifier
     */
    public Identifier getId() {
        return this.id;
    }

    /**
     * Returns the death-message translation key suffix.
     *
     * @return the message id
     */
    public String getMessageId() {
        return this.messageId;
    }

    /**
     * Returns the default death-message translation key.
     *
     * @return the translation key
     */
    public String getTranslationKey() {
        return "death.attack." + this.messageId;
    }

    /**
     * Returns when damage of this type scales with difficulty.
     *
     * @return the difficulty scaling rule
     */
    public DamageScaling getDamageScaling() {
        return this.scaling;
    }

    /**
     * Returns the hunger exhaustion caused by this damage.
     *
     * @return the exhaustion amount
     */
    public float getExhaustion() {
        return this.exhaustion;
    }

    /**
     * Returns the feedback effect for this damage.
     *
     * @return the damage effect
     */
    public DamageEffect getDamageEffect() {
        return this.effect;
    }

    /**
     * Returns how death messages are resolved for this damage.
     *
     * @return the death-message type
     */
    public DeathMessageType getDeathMessageType() {
        return this.deathMessageType;
    }

    /**
     * Returns the behavioral tags assigned to this damage type.
     *
     * @return immutable damage type tags
     */
    public Set<DamageTypeTag> getTags() {
        return this.tags;
    }

    /**
     * Checks whether this damage type belongs to the supplied tag.
     *
     * @param tag the tag to test
     * @return whether this damage type belongs to the tag
     */
    public boolean is(DamageTypeTag tag) {
        return this.tags.contains(requireNonNull(tag, "tag"));
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}

package org.cloudburstmc.server.network;

import org.cloudburstmc.protocol.bedrock.data.AttributeData;

/**
 * Attribute packet definitions for values owned by gameplay code.
 */
public enum NetworkEntityAttribute {
    ABSORPTION("minecraft:absorption", Float.MAX_VALUE, 0f),
    SATURATION("minecraft:player.saturation", 20f, 5f),
    EXHAUSTION("minecraft:player.exhaustion", 5f, 0.41f),
    MOVEMENT_SPEED("minecraft:movement", Float.MAX_VALUE, 0.1f),
    HUNGER("minecraft:player.hunger", 20f, 20f),
    EXPERIENCE_LEVEL("minecraft:player.level", 24791f, 0f),
    EXPERIENCE("minecraft:player.experience", 1f, 0f);

    private final String identifier;
    private final float maximum;
    private final float defaultValue;

    NetworkEntityAttribute(String identifier, float maximum, float defaultValue) {
        this.identifier = identifier;
        this.maximum = maximum;
        this.defaultValue = defaultValue;
    }

    public AttributeData encode(float value) {
        return encode(this.identifier, this.maximum, value, this.defaultValue);
    }

    public static AttributeData encodeHealth(float health, int maxHealth) {
        return encode("minecraft:health", maxHealth, health, maxHealth);
    }

    private static AttributeData encode(String identifier, float maximum, float value, float defaultValue) {
        float boundedValue = Float.isNaN(value) ? 0f : Math.clamp(value, 0f, maximum);
        return new AttributeData(identifier, 0f, maximum, boundedValue, defaultValue);
    }
}

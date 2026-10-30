package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Persistent data shared by every entity.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Getter
@Setter
@EqualsAndHashCode
public class EntityData {

    @JsonProperty("Pos")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.FLOAT)
    private List<Float> pos = List.of(0f, 0f, 0f);

    @JsonProperty("Motion")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.FLOAT)
    private List<Float> motion = List.of(0f, 0f, 0f);

    @JsonProperty("Rotation")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.FLOAT)
    private List<Float> rotation = List.of(0f, 0f);

    @JsonProperty("FallDistance")
    private float fallDistance;

    @JsonProperty("Fire")
    private short fire;

    @JsonProperty("Air")
    private short air = 400;

    @JsonProperty("OnGround")
    private boolean onGround;

    @JsonProperty("Invulnerable")
    private boolean invulnerable;

    @JsonProperty("TicksFrozen")
    private Integer ticksFrozen;

    @JsonProperty("FromBucket")
    private Boolean fromBucket;

    @JsonProperty("Scale")
    private float scale = 1;

    @JsonProperty("ActiveEffects")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    private List<Effect> activeEffects;

    @JsonProperty("CustomName")
    private String customName;

    @JsonProperty("CustomNameVisible")
    private Boolean customNameVisible;

    @JsonProperty("CustomNameAlwaysVisible")
    private Boolean customNameAlwaysVisible;

    @JsonProperty("Tags")
    private List<String> tags = List.of();

    public EntityData() {
    }

    /**
     * A single active potion effect stored on an entity.
     */
    @JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class Effect {

        @JsonProperty("Id")
        private byte id;

        @JsonProperty("Amplifier")
        private byte amplifier;

        @JsonProperty("Duration")
        private int duration;

        @JsonProperty("Ambient")
        private boolean ambient;

        @JsonProperty("ShowParticles")
        private boolean showParticles;

        public Effect() {
        }
    }
}

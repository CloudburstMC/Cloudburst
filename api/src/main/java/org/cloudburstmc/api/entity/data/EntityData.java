package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Persistent data shared by every entity: the keys the base entity keeps in
 * runtime types. Movement and physics state ({@code Pos}, {@code Motion},
 * {@code Rotation}, {@code FallDistance}, {@code OnGround}), the air supply,
 * scale, the name tag and the active effects stay in their runtime holders and
 * travel through the base entity's load/save hooks instead.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Getter
@Setter
@EqualsAndHashCode
public class EntityData {

    @JsonProperty("Fire")
    private short fire;

    @JsonProperty("Invulnerable")
    private boolean invulnerable;

    @JsonProperty("TicksFrozen")
    private Integer ticksFrozen;

    @JsonProperty("FromBucket")
    private Boolean fromBucket;

    @JsonProperty("Tags")
    private Set<String> tags = new LinkedHashSet<>();

    public EntityData() {
    }
}

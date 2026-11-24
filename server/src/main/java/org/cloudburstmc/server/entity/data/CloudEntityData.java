package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Fields persisted for all entities.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class CloudEntityData {

    @JsonProperty("Invulnerable")
    private boolean invulnerable;

    @JsonProperty("Cloudburst.BaseMaxHealth")
    private Integer baseMaxHealthOverride;

    @JsonProperty("TicksFrozen")
    private Integer ticksFrozen;

    @JsonProperty("Cloudburst.FreezeLock")
    private Boolean freezeTickingLocked;

    @JsonProperty("FromBucket")
    private Boolean fromBucket;

    @JsonProperty("Tags")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> tags = new LinkedHashSet<>();
}

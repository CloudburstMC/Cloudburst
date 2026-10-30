package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a projectile: everything shared plus its damage.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ProjectileData extends EntityData {

    @JsonProperty("damage")
    private float damage;

    public ProjectileData() {
    }
}

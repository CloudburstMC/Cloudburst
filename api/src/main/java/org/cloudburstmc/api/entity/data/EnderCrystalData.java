package org.cloudburstmc.api.entity.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of an ender crystal: everything shared. Base visibility and
 * the beam target are metadata-backed, so they travel through the load/save
 * hooks instead.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EnderCrystalData extends EntityData {

    public EnderCrystalData() {
    }
}

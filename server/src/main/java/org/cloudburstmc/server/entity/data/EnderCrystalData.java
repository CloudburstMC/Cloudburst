package org.cloudburstmc.server.entity.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence type for end crystals. Beam target and base visibility are saved from entity metadata.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EnderCrystalData extends EntityData {
}

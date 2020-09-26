package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerConfig {

    private int skinChangeCooldown = 30;

    private boolean savePlayerData = true;

}

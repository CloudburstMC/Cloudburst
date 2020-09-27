package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerConfig {

    @Builder.Default
    private int skinChangeCooldown = 30;

    @Builder.Default
    private boolean savePlayerData = true;

}

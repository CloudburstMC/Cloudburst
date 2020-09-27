package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpawnLimitsConfig {

    @Builder.Default
    private int monsters = 70;

    @Builder.Default
    private int animals = 15;

    @Builder.Default
    private int waterAnimals = 5;

    @Builder.Default
    private int ambient = 15;

}

package org.cloudburstmc.server.config;

import lombok.*;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpawnLimitsConfig {

    private int monsters;

    private int animals;

    private int waterAnimals;

    private int ambient;

}

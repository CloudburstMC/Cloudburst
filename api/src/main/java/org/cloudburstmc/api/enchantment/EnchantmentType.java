package org.cloudburstmc.api.enchantment;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.Identifier;

@RequiredArgsConstructor
@Getter
@Builder
public class EnchantmentType {
    private final short id;
    private final Identifier type;
    private final int maxLevel;
    private final EnchantmentRarity rarity;
    private final boolean treasure;
    private final boolean cursed;
    private final EnchantmentTarget target;
}

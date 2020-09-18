package org.cloudburstmc.server.item;

import lombok.Value;

public class ArmorPartTypes {

    public static final ArmorPartType HELMET = new IntArmorPartType(11);
    public static final ArmorPartType CHESTPLATE = new IntArmorPartType(16);
    public static final ArmorPartType LEGGINGS = new IntArmorPartType(15);
    public static final ArmorPartType BOOTS = new IntArmorPartType(13);

    @Value
    private static class IntArmorPartType implements ArmorPartType {

        int durabilityBase;
    }
}

package org.cloudburstmc.api.block.material;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialTypes {

    public static final MaterialType AIR = MaterialType.builder().translucency(1.0f).replaceable().alwaysDestroyable().build();

    public static final MaterialType DIRT = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType WOOD = MaterialType.builder().flammable().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType STONE = MaterialType.builder().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType METAL = MaterialType.builder().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType WATER = MaterialType.builder().translucency(1.0f).liquid().alwaysDestroyable().blockingPrecipitation().replaceable().build();

    public static final MaterialType LAVA = MaterialType.builder().translucency(1.0f).liquid().alwaysDestroyable().blockingPrecipitation().replaceable().superHot().build();

    public static final MaterialType LEAVES = MaterialType.builder().translucency(0.5f).alwaysDestroyable().blockingPrecipitation().blockingMotion().flammable().neverBuildable().build();

    public static final MaterialType PLANT = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().build();

    public static final MaterialType REPLACEABLE_PLANT = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().replaceable().build();

    public static final MaterialType SPONGE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType CLOTH = MaterialType.builder().translucency(0.8f).alwaysDestroyable().blockingPrecipitation().blockingMotion().flammable().build();

    public static final MaterialType BED = MaterialType.builder().translucency(0.1f).alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType FIRE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().replaceable().superHot().build();

    public static final MaterialType AGGREGATE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType DECORATION = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().build();

    public static final MaterialType GLASS = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().blockingMotion().neverBuildable().build();

    public static final MaterialType EXPLOSIVE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().flammable().build();

    public static final MaterialType ICE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().neverBuildable().build();

    public static final MaterialType PACKED_ICE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType TOP_SNOW = MaterialType.builder().translucency(0.9f).alwaysDestroyable().blockingPrecipitation().replaceable().neverBuildable().build();

    public static final MaterialType SNOW = MaterialType.builder().blockingPrecipitation().blockingMotion().build(); // 21

    public static final MaterialType CACTUS = MaterialType.builder().translucency(0.5f).alwaysDestroyable().blockingPrecipitation().blockingMotion().neverBuildable().build(); // 22

    public static final MaterialType CLAY = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType VEGETABLE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType PORTAL = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().build();

    public static final MaterialType CAKE = MaterialType.builder().translucency(0.8f).alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType WEB = MaterialType.builder().translucency(0.8f).build();

    public static final MaterialType WIRE = MaterialType.builder().translucency(0.8f).alwaysDestroyable().blockingPrecipitation().build();

    public static final MaterialType CARPET = MaterialType.builder().translucency(0.8f).alwaysDestroyable().flammable().build();

    public static final MaterialType BUILDABLE_GLASS = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType SLIME = MaterialType.builder().translucency(0.1f).alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType PISTON = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType ALLOW = MaterialType.builder().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType DENY = MaterialType.builder().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType NETHER_WART = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType STONE_DECORATION = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().build();

    public static final MaterialType BUBBLE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().replaceable().build();

    public static final MaterialType EGG = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType BARRIER = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType DECORATION_FLAMMABLE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().build();

    public static final MaterialType DECORATION_BLOCKING = MaterialType.builder().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType DRIPSTONE = MaterialType.builder().blockingPrecipitation().blockingMotion().build();

    public static final MaterialType SCULK = MaterialType.builder().blockingPrecipitation().blockingMotion().build();
}

package org.cloudburstmc.api.block.material;

public class MaterialTypes {

    public static MaterialType AIR = MaterialType.builder().translucency(1.0f).replaceable().alwaysDestroyable().build();

    public static MaterialType DIRT = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType WOOD = MaterialType.builder().flammable().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType STONE = MaterialType.builder().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType METAL = MaterialType.builder().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType WATER = MaterialType.builder().translucency(1.0f).liquid().alwaysDestroyable().blockingPrecipitation().replaceable().build();

    public static MaterialType LAVA = MaterialType.builder().translucency(1.0f).liquid().alwaysDestroyable().blockingPrecipitation().replaceable().superHot().build();

    public static MaterialType LEAVES = MaterialType.builder().translucency(0.5f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().flammable().neverBuildable().build();

    public static MaterialType PLANT = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().build();

    public static MaterialType REPLACEABLE_PLANT = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().replaceable().build();

    public static MaterialType SPONGE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType CLOTH = MaterialType.builder().translucency(0.8f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().flammable().build();

    public static MaterialType BED = MaterialType.builder().translucency(0.1f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType FIRE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().replaceable().superHot().build();

    public static MaterialType AGGREGATE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType DECORATION = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().build();

    public static MaterialType GLASS = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().neverBuildable().build();

    public static MaterialType EXPLOSIVE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().flammable().build();

    public static MaterialType ICE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().neverBuildable().build();

    public static MaterialType PACKED_ICE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType TOP_SNOW = MaterialType.builder().translucency(0.9f).alwaysDestroyable().blockingPrecipitation().replaceable().neverBuildable().build();

    public static MaterialType SNOW = MaterialType.builder().blockingPrecipitation().solid().blockingMotion().build(); // 21

    public static MaterialType CACTUS = MaterialType.builder().translucency(0.5f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().neverBuildable().build(); // 22

    public static MaterialType CLAY = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType VEGETABLE = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType PORTAL = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().build();

    public static MaterialType CAKE = MaterialType.builder().translucency(0.8f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType WEB = MaterialType.builder().translucency(0.8f).build();

    public static MaterialType WIRE = MaterialType.builder().translucency(0.8f).alwaysDestroyable().blockingPrecipitation().build();

    public static MaterialType CARPET = MaterialType.builder().translucency(0.8f).alwaysDestroyable().flammable().build();

    public static MaterialType BUILDABLE_GLASS = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType SLIME = MaterialType.builder().translucency(0.1f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType PISTON = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType ALLOW = MaterialType.builder().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType DENY = MaterialType.builder().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType NETHER_WART = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType STONE_DECORATION = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().build();

    public static MaterialType BUBBLE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().replaceable().build();

    public static MaterialType EGG = MaterialType.builder().alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType BARRIER = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().solid().blockingMotion().build();

    public static MaterialType DECORATION_FLAMMABLE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().build();

    public static MaterialType DECORATION_SOLID = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().build();

    public static MaterialType DRIPSTONE = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().build();

    public static MaterialType SCULK = MaterialType.builder().translucency(1.0f).alwaysDestroyable().blockingPrecipitation().flammable().build();
}

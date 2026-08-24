package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.*;
import org.cloudburstmc.api.entity.passive.*;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

public class SpawnEggSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(EntityType.class, EntityTypes.BAT);
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Class<?> type = item.get(ItemKeys.SPAWN_EGG_TYPE).getEntityClass();

        if (Bat.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.BAT_SPAWN_EGG.getId().toString());
        } else if (Cow.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.BAT_SPAWN_EGG.getId().toString());
        } else if (Chicken.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.CHICKEN_SPAWN_EGG.getId().toString());
        } else if (Bee.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.BEE_SPAWN_EGG.getId().toString());
        } else if (Pig.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PIG_SPAWN_EGG.getId().toString());
        } else if (Sheep.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SHEEP_SPAWN_EGG.getId().toString());
        } else if (Wolf.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.WOLF_SPAWN_EGG.getId().toString());
        } else if (PolarBear.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.POLAR_BEAR_SPAWN_EGG.getId().toString());
        } else if (Ocelot.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.OCELOT_SPAWN_EGG.getId().toString());
        } else if (Cat.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.CAT_SPAWN_EGG.getId().toString());
        } else if (Mooshroom.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.MOOSHROOM_SPAWN_EGG.getId().toString());
        } else if (Parrot.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PARROT_SPAWN_EGG.getId().toString());
        } else if (Rabbit.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.RABBIT_SPAWN_EGG.getId().toString());
        } else if (Llama.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.LLAMA_SPAWN_EGG.getId().toString());
        } else if (Horse.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.HORSE_SPAWN_EGG.getId().toString());
        } else if (Donkey.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.DONKEY_SPAWN_EGG.getId().toString());
        } else if (Mule.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.MULE_SPAWN_EGG.getId().toString());
        } else if (SkeletonHorse.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SKELETON_HORSE_SPAWN_EGG.getId().toString());
        } else if (ZombieHorse.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ZOMBIE_HORSE_SPAWN_EGG.getId().toString());
        } else if (TropicalFish.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.TROPICAL_FISH_SPAWN_EGG.getId().toString());
        } else if (Cod.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.COD_SPAWN_EGG.getId().toString());
        } else if (Pufferfish.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PUFFERFISH_SPAWN_EGG.getId().toString());
        } else if (Salmon.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SALMON_SPAWN_EGG.getId().toString());
        } else if (Dolphin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.DOLPHIN_SPAWN_EGG.getId().toString());
        } else if (Turtle.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.TURTLE_SPAWN_EGG.getId().toString());
        } else if (Panda.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PANDA_SPAWN_EGG.getId().toString());
        } else if (Fox.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.FOX_SPAWN_EGG.getId().toString());
        } else if (Creeper.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.CREEPER_SPAWN_EGG.getId().toString());
        } else if (Enderman.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ENDERMAN_SPAWN_EGG.getId().toString());
        } else if (Silverfish.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SILVERFISH_SPAWN_EGG.getId().toString());
        } else if (Skeleton.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SKELETON_SPAWN_EGG.getId().toString());
        } else if (WitherSkeleton.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.WITHER_SKELETON_SPAWN_EGG.getId().toString());
        } else if (Stray.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.STRAY_SPAWN_EGG.getId().toString());
        } else if (Slime.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SLIME_SPAWN_EGG.getId().toString());
        } else if (Spider.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SPIDER_SPAWN_EGG.getId().toString());
        } else if (Zombie.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ZOMBIE_SPAWN_EGG.getId().toString());
        } else if (ZombiePigman.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ZOMBIE_PIGMAN_SPAWN_EGG.getId().toString());
        } else if (Husk.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.HUSK_SPAWN_EGG.getId().toString());
        } else if (Drowned.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.DROWNED_SPAWN_EGG.getId().toString());
        } else if (Squid.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SQUID_SPAWN_EGG.getId().toString());
        } else if (CaveSpider.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.CAVE_SPIDER_SPAWN_EGG.getId().toString());
        } else if (Witch.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.WITCH_SPAWN_EGG.getId().toString());
        } else if (Guardian.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.GUARDIAN_SPAWN_EGG.getId().toString());
        } else if (ElderGuardian.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ELDER_GUARDIAN_SPAWN_EGG.getId().toString());
        } else if (Endermite.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ENDERMITE_SPAWN_EGG.getId().toString());
        } else if (MagmaCube.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.MAGMA_CUBE_SPAWN_EGG.getId().toString());
        } else if (Strider.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.STRIDER_SPAWN_EGG.getId().toString());
        } else if (Hoglin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.HOGLIN_SPAWN_EGG.getId().toString());
        } else if (Piglin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PIGLIN_SPAWN_EGG.getId().toString());
        } else if (Zoglin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ZOGLIN_SPAWN_EGG.getId().toString());
        } else if (PiglinBrute.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PIGLIN_BRUTE_SPAWN_EGG.getId().toString());
        } else if (Ghast.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.GHAST_SPAWN_EGG.getId().toString());
        } else if (Blaze.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.BLAZE_SPAWN_EGG.getId().toString());
        } else if (Shulker.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.SHULKER_SPAWN_EGG.getId().toString());
        } else if (Vindicator.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.VINDICATOR_SPAWN_EGG.getId().toString());
        } else if (EvocationIllager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.EVOKER_SPAWN_EGG.getId().toString());
        } else if (Vex.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.VEX_SPAWN_EGG.getId().toString());
        } else if (Villager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.VILLAGER_SPAWN_EGG.getId().toString());
        } else if (WanderingTrader.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.WANDERING_TRADER_SPAWN_EGG.getId().toString());
        } else if (ZombieVillager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.ZOMBIE_VILLAGER_SPAWN_EGG.getId().toString());
        } else if (Phantom.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PHANTOM_SPAWN_EGG.getId().toString());
        } else if (Pillager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.PILLAGER_SPAWN_EGG.getId().toString());
        } else if (Ravager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemTypes.RAVAGER_SPAWN_EGG.getId().toString());
        } else {
            itemTag.putString(NAME_TAG, ItemTypes.BAT_SPAWN_EGG.getId().toString());
        }

    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}

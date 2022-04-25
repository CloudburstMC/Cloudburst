package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.*;
import org.cloudburstmc.api.entity.passive.*;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;

import java.util.Map;

public class SpawnEggSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(EntityType.class, EntityTypes.BAT);
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Class<?> type = item.getMetadata(EntityType.class).getEntityClass();

        if (Bat.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.BAT_SPAWN_EGG.toString());
        } else if (Cow.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.COW_SPAWN_EGG.toString());
        } else if (Chicken.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.CHICKEN_SPAWN_EGG.toString());
        } else if (Bee.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.BEE_SPAWN_EGG.toString());
        } else if (Pig.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PIG_SPAWN_EGG.toString());
        } else if (Sheep.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SHEEP_SPAWN_EGG.toString());
        } else if (Wolf.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.WOLF_SPAWN_EGG.toString());
        } else if (PolarBear.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.POLAR_BEAR_SPAWN_EGG.toString());
        } else if (Ocelot.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.OCELOT_SPAWN_EGG.toString());
        } else if (Cat.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.CAT_SPAWN_EGG.toString());
        } else if (Mooshroom.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.MOOSHROOM_SPAWN_EGG.toString());
        } else if (Parrot.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PARROT_SPAWN_EGG.toString());
        } else if (Rabbit.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.RABBIT_SPAWN_EGG.toString());
        } else if (Llama.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.LLAMA_SPAWN_EGG.toString());
        } else if (Horse.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.HORSE_SPAWN_EGG.toString());
        } else if (Donkey.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.DONKEY_SPAWN_EGG.toString());
        } else if (Mule.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.MULE_SPAWN_EGG.toString());
        } else if (SkeletonHorse.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SKELETON_HORSE_SPAWN_EGG.toString());
        } else if (ZombieHorse.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ZOMBIE_HORSE_SPAWN_EGG.toString());
        } else if (TropicalFish.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.TROPICAL_FISH_SPAWN_EGG.toString());
        } else if (Cod.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.COD_SPAWN_EGG.toString());
        } else if (Pufferfish.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PUFFERFISH_SPAWN_EGG.toString());
        } else if (Salmon.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SALMON_SPAWN_EGG.toString());
        } else if (Dolphin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.DOLPHIN_SPAWN_EGG.toString());
        } else if (Turtle.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.TURTLE_SPAWN_EGG.toString());
        } else if (Panda.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PANDA_SPAWN_EGG.toString());
        } else if (Fox.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.FOX_SPAWN_EGG.toString());
        } else if (Creeper.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.CREEPER_SPAWN_EGG.toString());
        } else if (Enderman.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ENDERMAN_SPAWN_EGG.toString());
        } else if (Silverfish.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SILVERFISH_SPAWN_EGG.toString());
        } else if (Skeleton.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SKELETON_SPAWN_EGG.toString());
        } else if (WitherSkeleton.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.WITHER_SKELETON_SPAWN_EGG.toString());
        } else if (Stray.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.STRAY_SPAWN_EGG.toString());
        } else if (Slime.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SLIME_SPAWN_EGG.toString());
        } else if (Spider.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SPIDER_SPAWN_EGG.toString());
        } else if (Zombie.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ZOMBIE_SPAWN_EGG.toString());
        } else if (ZombiePigman.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ZOMBIE_PIGMAN_SPAWN_EGG.toString());
        } else if (Husk.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.HUSK_SPAWN_EGG.toString());
        } else if (Drowned.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.DROWNED_SPAWN_EGG.toString());
        } else if (Squid.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SQUID_SPAWN_EGG.toString());
        } else if (CaveSpider.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.CAVE_SPIDER_SPAWN_EGG.toString());
        } else if (Witch.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.WITCH_SPAWN_EGG.toString());
        } else if (Guardian.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.GUARDIAN_SPAWN_EGG.toString());
        } else if (ElderGuardian.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ELDER_GUARDIAN_SPAWN_EGG.toString());
        } else if (Endermite.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ENDERMITE_SPAWN_EGG.toString());
        } else if (MagmaCube.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.MAGMA_CUBE_SPAWN_EGG.toString());
        } else if (Strider.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.STRIDER_SPAWN_EGG.toString());
        } else if (Hoglin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.HOGLIN_SPAWN_EGG.toString());
        } else if (Piglin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PIGLIN_SPAWN_EGG.toString());
        } else if (Zoglin.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ZOGLIN_SPAWN_EGG.toString());
        } else if (PiglinBrute.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PIGLIN_BRUTE_SPAWN_EGG.toString());
        } else if (Ghast.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.GHAST_SPAWN_EGG.toString());
        } else if (Blaze.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.BLAZE_SPAWN_EGG.toString());
        } else if (Shulker.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.SHULKER_SPAWN_EGG.toString());
        } else if (Vindicator.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.VINDICATOR_SPAWN_EGG.toString());
        } else if (EvocationIllager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.EVOKER_SPAWN_EGG.toString());
        } else if (Vex.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.VEX_SPAWN_EGG.toString());
        } else if (Villager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.VILLAGER_SPAWN_EGG.toString());
        } else if (WanderingTrader.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.WANDERING_TRADER_SPAWN_EGG.toString());
        } else if (ZombieVillager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.ZOMBIE_VILLAGER_SPAWN_EGG.toString());
        } else if (Phantom.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PHANTOM_SPAWN_EGG.toString());
        } else if (Pillager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.PILLAGER_SPAWN_EGG.toString());
        } else if (Ravager.class.isAssignableFrom(type)) {
            itemTag.putString(NAME_TAG, ItemIds.RAVAGER_SPAWN_EGG.toString());
        } else {
            itemTag.putString(NAME_TAG, ItemIds.SPAWN_EGG.toString());
        }

    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, amount, builder, tag);

        if(id.equals(ItemIds.BAT_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.BAT);
        } else if(id.equals(ItemIds.COW_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.COW);
        } else if(id.equals(ItemIds.CHICKEN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.CHICKEN);
        } else if(id.equals(ItemIds.BEE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.BEE);
        } else if(id.equals(ItemIds.PIG_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PIG);
        } else if(id.equals(ItemIds.SHEEP_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SHEEP);
        } else if(id.equals(ItemIds.WOLF_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.WOLF);
        } else if(id.equals(ItemIds.POLAR_BEAR_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.POLAR_BEAR);
        } else if(id.equals(ItemIds.OCELOT_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.OCELOT);
        } else if(id.equals(ItemIds.CAT_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.CAT);
        } else if(id.equals(ItemIds.MOOSHROOM_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.MOOSHROOM);
        } else if(id.equals(ItemIds.PARROT_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PARROT);
        } else if(id.equals(ItemIds.RABBIT_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.RABBIT);
        } else if(id.equals(ItemIds.LLAMA_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.LLAMA);
        } else if(id.equals(ItemIds.HORSE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.HORSE);
        } else if(id.equals(ItemIds.DONKEY_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.DONKEY);
        } else if(id.equals(ItemIds.MULE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.MULE);
        } else if(id.equals(ItemIds.SKELETON_HORSE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SKELETON_HORSE);
        } else if(id.equals(ItemIds.ZOMBIE_HORSE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ZOMBIE_HORSE);
        } else if(id.equals(ItemIds.TROPICAL_FISH_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.TROPICALFISH);
        } else if(id.equals(ItemIds.COD_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.COD);
        } else if(id.equals(ItemIds.PUFFERFISH_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PUFFERFISH);
        } else if(id.equals(ItemIds.SALMON_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SALMON);
        } else if(id.equals(ItemIds.DOLPHIN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.DOLPHIN);
        } else if(id.equals(ItemIds.TURTLE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.TURTLE);
        } else if(id.equals(ItemIds.PANDA_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PANDA);
        } else if(id.equals(ItemIds.FOX_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.FOX);
        } else if(id.equals(ItemIds.CREEPER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.CREEPER);
        } else if(id.equals(ItemIds.ENDERMAN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ENDERMAN);
        } else if(id.equals(ItemIds.SILVERFISH_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SILVERFISH);
        } else if(id.equals(ItemIds.SKELETON_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SKELETON);
        } else if(id.equals(ItemIds.SLIME_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SLIME);
        } else if(id.equals(ItemIds.SPIDER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SPIDER);
        } else if(id.equals(ItemIds.ZOMBIE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ZOMBIE);
        } else if(id.equals(ItemIds.ZOMBIE_PIGMAN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ZOMBIE_PIGMAN);
        } else if(id.equals(ItemIds.HUSK_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.HUSK);
        } else if(id.equals(ItemIds.DROWNED_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.DROWNED);
        } else if(id.equals(ItemIds.SQUID_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SQUID);
        } else if(id.equals(ItemIds.CAVE_SPIDER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.CAVE_SPIDER);
        } else if(id.equals(ItemIds.WITCH_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.WITCH);
        } else if(id.equals(ItemIds.GUARDIAN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.GUARDIAN);
        } else if(id.equals(ItemIds.ELDER_GUARDIAN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ELDER_GUARDIAN);
        } else if(id.equals(ItemIds.ENDERMITE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ENDERMITE);
        } else if(id.equals(ItemIds.MAGMA_CUBE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.MAGMA_CUBE);
        } else if(id.equals(ItemIds.STRIDER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.STRIDER);
        } else if(id.equals(ItemIds.HOGLIN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.HOGLIN);
        } else if(id.equals(ItemIds.PIGLIN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PIGLIN);
        } else if(id.equals(ItemIds.ZOGLIN_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ZOGLIN);
        } else if(id.equals(ItemIds.PIGLIN_BRUTE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PIGLIN_BRUTE);
        } else if(id.equals(ItemIds.GHAST_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.GHAST);
        } else if(id.equals(ItemIds.BLAZE_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.BLAZE);
        } else if(id.equals(ItemIds.SHULKER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.SHULKER);
        } else if(id.equals(ItemIds.VINDICATOR_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.VINDICATOR);
        } else if(id.equals(ItemIds.EVOKER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.EVOCATION_ILLAGER);
        } else if(id.equals(ItemIds.VEX_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.VEX);
        } else if(id.equals(ItemIds.VILLAGER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.VILLAGER);
        } else if(id.equals(ItemIds.WANDERING_TRADER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.WANDERING_TRADER);
        } else if(id.equals(ItemIds.ZOMBIE_VILLAGER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.ZOMBIE_VILLAGER);
        } else if(id.equals(ItemIds.PHANTOM_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PHANTOM);
        } else if(id.equals(ItemIds.PILLAGER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.PILLAGER);
        } else if(id.equals(ItemIds.RAVAGER_SPAWN_EGG)) {
            builder.itemData(EntityType.class, EntityTypes.RAVAGER);
        }
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}

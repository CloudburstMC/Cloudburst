package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.entity.vehicle.BoatType;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.nbt.serializer.BoatTypeDeserializer;
import org.cloudburstmc.server.nbt.serializer.BoatTypeSerializer;
import org.cloudburstmc.server.nbt.serializer.EffectTypeDeserializer;
import org.cloudburstmc.server.nbt.serializer.EffectTypeSerializer;
import org.cloudburstmc.server.nbt.serializer.ItemContainerDeserializer;
import org.cloudburstmc.server.nbt.serializer.ItemContainerSerializer;
import org.cloudburstmc.server.nbt.serializer.ItemStackDeserializer;
import org.cloudburstmc.server.nbt.serializer.ItemStackSerializer;
import org.cloudburstmc.server.nbt.serializer.PotionEffectDeserializer;
import org.cloudburstmc.server.nbt.serializer.PotionEffectSerializer;
import org.cloudburstmc.server.nbt.serializer.PotionTypeDeserializer;
import org.cloudburstmc.server.nbt.serializer.PotionTypeSerializer;
import org.cloudburstmc.server.nbt.serializer.SkinDeserializer;
import org.cloudburstmc.server.nbt.serializer.SkinSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Registers the server's NBT serializers for the runtime types data objects hold:
 * item stacks, item containers, skins, effect, potion and boat types, and potion
 * effects. The serializers live in {@code org.cloudburstmc.server.nbt.serializer}.
 */
public class ServerTypesNbtModule extends SimpleModule {

    public ServerTypesNbtModule() {
        super("cloudburst-server-types");
        addSerializer(ItemStack.class, new ItemStackSerializer());
        addDeserializer(ItemStack.class, new ItemStackDeserializer());
        addSerializer(Skin.class, new SkinSerializer());
        addDeserializer(Skin.class, new SkinDeserializer());
        addSerializer(ItemContainer.class, new ItemContainerSerializer());
        addDeserializer(ItemContainer.class, new ItemContainerDeserializer());
        addSerializer(EffectType.class, new EffectTypeSerializer());
        addDeserializer(EffectType.class, new EffectTypeDeserializer());
        addSerializer(BoatType.class, new BoatTypeSerializer());
        addDeserializer(BoatType.class, new BoatTypeDeserializer());
        addSerializer(PotionType.class, new PotionTypeSerializer());
        addDeserializer(PotionType.class, new PotionTypeDeserializer());
        addSerializer(PotionEffect.class, new PotionEffectSerializer());
        addDeserializer(PotionEffect.class, new PotionEffectDeserializer());
    }
}

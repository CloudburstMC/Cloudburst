package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.Cauldron;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.event.block.CauldronChangeReason;
import org.cloudburstmc.api.event.block.CauldronLevelChangeEvent;
import org.cloudburstmc.api.event.player.PlayerBucketFillEvent;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.level.particle.ParticleEmitter;
import org.cloudburstmc.api.level.particle.ParticleEmitterTypes;
import org.cloudburstmc.api.level.particle.ParticleNumber;
import org.cloudburstmc.api.level.particle.ParticleStruct;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.PotionForm;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.Bucket;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.potion.CloudPotionColor;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CauldronBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void dripFillingRevalidatesTheSourceAndStopsWhenFull() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        for (int fill : List.of(2, 4, 6)) {
            CauldronBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
            assertEquals(fill, world.state(POSITION).ensureTrait(BlockTraits.FILL_LEVEL));
        }

        CauldronBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
        assertEquals(3, world.getEvents(CauldronLevelChangeEvent.class).size());
        assertEquals(3, world.getSounds().size());
        for (SoundPlayback playback : world.getSoundPlaybacks()) {
            assertEquals(2f, playback.volume());
            assertTrue(playback.pitch() >= 0.9f && playback.pitch() <= 1f);
        }
        assertFalse(CauldronBlockHandlers.canReceiveDrip(world.block(POSITION), LiquidTypes.WATER));

        BlockTestWorld removed = supplied(BlockStates.WATER);
        removed.setState(POSITION.add(0, 6, 0), BlockStates.AIR);
        CauldronBlockHandlers.ON_TICK.execute(removed.block(POSITION), new Random(0));
        assertEquals(0, removed.state(POSITION).ensureTrait(BlockTraits.FILL_LEVEL));
        assertTrue(removed.getEvents(CauldronLevelChangeEvent.class).isEmpty());
    }

    @Test
    void lavaFillsAnEmptyCauldronButCannotReplaceWaterOrSnow() {
        BlockTestWorld world = supplied(BlockStates.LAVA);
        CauldronBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));

        assertEquals(6, world.state(POSITION).ensureTrait(BlockTraits.FILL_LEVEL));
        assertEquals(Bucket.LAVA, world.state(POSITION).ensureTrait(BlockTraits.CAULDRON_TYPE));
        assertEquals(List.of(SoundTypes.CAULDRON_DRIP_LAVA_POINTED_DRIPSTONE), world.getSounds());

        for (Bucket content : List.of(Bucket.WATER, Bucket.POWDER_SNOW)) {
            world.setState(POSITION, cauldron(content, 2));
            CauldronBlockHandlers.ON_TICK.execute(world.block(POSITION), new Random(0));
            assertSame(cauldron(content, 2), world.state(POSITION));
        }
    }

    @Test
    void naturalFillingCanBeCancelledOrRedirected() {
        BlockTestWorld cancelled = supplied(BlockStates.WATER);
        cancelled.onEvent(CauldronLevelChangeEvent.class, event -> event.setCancelled(true));
        CauldronBlockHandlers.ON_TICK.execute(cancelled.block(POSITION), new Random(0));

        assertEquals(0, cancelled.state(POSITION).ensureTrait(BlockTraits.FILL_LEVEL));
        assertTrue(cancelled.getSounds().isEmpty());

        BlockTestWorld redirected = supplied(BlockStates.WATER);
        redirected.onEvent(CauldronLevelChangeEvent.class, event -> event.setNewState(cauldron(Bucket.WATER, 4)));
        CauldronBlockHandlers.ON_TICK.execute(redirected.block(POSITION), new Random(0));

        assertEquals(4, redirected.state(POSITION).ensureTrait(BlockTraits.FILL_LEVEL));
        CauldronLevelChangeEvent event = redirected.getEvents(CauldronLevelChangeEvent.class).getFirst();
        assertEquals(CauldronChangeReason.NATURAL_FILL, event.getReason());
        assertNull(event.getEntity());
    }

    @Test
    void rainAndSnowFillOnlyMatchingContents() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, cauldron(Bucket.WATER, 0));

        CauldronBlockHandlers.collectPrecipitation(world.block(POSITION), true, new AlwaysFill());
        assertSame(cauldron(Bucket.POWDER_SNOW, 2), world.state(POSITION));

        CauldronBlockHandlers.collectPrecipitation(world.block(POSITION), false, new AlwaysFill());
        assertSame(cauldron(Bucket.POWDER_SNOW, 2), world.state(POSITION));
    }

    @Test
    void cancelledBucketFillingDoesNotExchangeItemsOrPlaySound() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        world.onEvent(CauldronLevelChangeEvent.class, event -> event.setCancelled(true));
        ItemStack[] held = {ItemStack.from(ItemTypes.BUCKET)};
        List<ItemStack> given = new ArrayList<>();
        Player player = player(world, held, given, false);

        assertTrue(CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]));

        assertSame(ItemTypes.BUCKET, held[0].getType());
        assertSame(cauldron(Bucket.WATER, 6), world.state(POSITION));
        assertTrue(given.isEmpty());
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void survivalBucketStacksKeepTheirRemainderAndCreativeKeepsTheHeldBucket() {
        for (boolean creative : List.of(false, true)) {
            BlockTestWorld world = supplied(BlockStates.WATER);
            world.setState(POSITION, cauldron(Bucket.WATER, 6));
            ItemStack[] held = {ItemStack.from(ItemTypes.BUCKET, 2)};
            List<ItemStack> given = new ArrayList<>();
            Player player = player(world, held, given, creative);

            CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

            assertEquals(creative ? 2 : 1, held[0].getCount());
            assertSame(ItemTypes.BUCKET, held[0].getType());
            assertEquals(1, given.size());
            assertSame(ItemTypes.WATER_BUCKET, given.getFirst().getType());
            assertSame(cauldron(Bucket.WATER, 0), world.state(POSITION));
        }
    }

    @Test
    void dyeingWaterPersistsColorAndConsumesOneDyeWithoutLoweringTheLevel() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        CauldronContents[] contents = attachContents(world, CauldronContents.PLAIN);
        ItemStack[] held = {ItemStack.from(ItemTypes.RED_DYE, 3)};
        Player player = player(world, held, new ArrayList<>(), false);

        assertTrue(CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]));

        assertEquals(0xB02E26, contents[0].color());
        assertEquals(2, held[0].getCount());
        assertSame(cauldron(Bucket.WATER, 6), world.state(POSITION));
        assertEquals(List.of(SoundTypes.CAULDRON_ADDDYE), world.getSounds());
        assertEquals(CauldronChangeReason.DYE_WATER, world.getEvents(CauldronLevelChangeEvent.class).getFirst().getReason());
    }

    @Test
    void equipmentCanBeDyedAndWashedWithoutLosingOtherComponents() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        CauldronContents[] contents = attachContents(world, new CauldronContents(null, PotionForm.DRINKABLE, 0x123456));
        ItemStack[] held = {ItemStack.builder(ItemTypes.LEATHER_HELMET)
                .setData(ItemDataComponents.CUSTOM_NAME, "Kept").build()};
        Player player = player(world, held, new ArrayList<>(), false);

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

        assertEquals(0x123456, held[0].get(ItemDataComponents.DYED_COLOR));
        assertEquals("Kept", held[0].get(ItemDataComponents.CUSTOM_NAME));
        assertSame(cauldron(Bucket.WATER, 4), world.state(POSITION));

        contents[0] = CauldronContents.PLAIN;
        CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

        assertNull(held[0].get(ItemDataComponents.DYED_COLOR));
        assertEquals("Kept", held[0].get(ItemDataComponents.CUSTOM_NAME));
        assertSame(cauldron(Bucket.WATER, 3), world.state(POSITION));
        assertEquals(List.of(SoundTypes.CAULDRON_DYEARMOR, SoundTypes.CAULDRON_CLEANARMOR), world.getSounds());
    }

    @Test
    void dyeingEquipmentWithOneRemainingLevelEmptiesTheCauldron() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 1));
        CauldronContents[] contents = attachContents(world, new CauldronContents(null, PotionForm.DRINKABLE, 0x123456));
        ItemStack[] held = {ItemStack.from(ItemTypes.LEATHER_HELMET)};
        Player player = player(world, held, new ArrayList<>(), false);

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

        assertEquals(0x123456, held[0].get(ItemDataComponents.DYED_COLOR));
        assertSame(cauldron(Bucket.WATER, 0), world.state(POSITION));
        assertEquals(CauldronContents.PLAIN, contents[0]);
    }

    @Test
    void addingAWaterBottleClearsDyeAndAddsTwoLevels() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 2));
        CauldronContents[] contents = attachContents(world, new CauldronContents(null, PotionForm.DRINKABLE, 0x123456));
        ItemStack[] held = {ItemStack.builder(ItemTypes.POTION)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.WATER)
                .build()};
        Player player = player(world, held, new ArrayList<>(), false);

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

        assertSame(ItemTypes.GLASS_BOTTLE, held[0].getType());
        assertSame(cauldron(Bucket.WATER, 4), world.state(POSITION));
        assertEquals(CauldronContents.PLAIN, contents[0]);
        assertEquals(List.of(SoundTypes.CAULDRON_FILLWATER), world.getSounds());
    }

    @Test
    void potionBottlesRetainTheirFormAndClearingTheLastLevelClearsContents() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        CauldronContents[] contents = attachContents(world, CauldronContents.PLAIN);
        ItemStack[] held = {ItemStack.builder(ItemTypes.LINGERING_POTION)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON).build()};
        Player player = player(world, held, new ArrayList<>(), false);

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

        assertEquals(new CauldronContents(PotionTypes.POISON, PotionForm.LINGERING, null), contents[0]);
        assertSame(ItemTypes.GLASS_BOTTLE, held[0].getType());
        assertSame(cauldron(Bucket.WATER, 2), world.state(POSITION));

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

        assertSame(ItemTypes.LINGERING_POTION, held[0].getType());
        assertSame(PotionTypes.POISON, held[0].get(ItemDataComponents.POTION_TYPE));
        assertEquals(CauldronContents.PLAIN, contents[0]);
        assertSame(cauldron(Bucket.WATER, 0), world.state(POSITION));
        assertEquals(List.of(SoundTypes.CAULDRON_FILLPOTION, SoundTypes.CAULDRON_TAKEPOTION), world.getSounds());
    }

    @Test
    void everyPotionFormEmitsColoredParticlesAtEachAcceptedFillHeight() {
        for (ItemType type : List.of(ItemTypes.POTION, ItemTypes.SPLASH_POTION, ItemTypes.LINGERING_POTION)) {
            BlockTestWorld world = supplied(BlockStates.WATER);
            attachContents(world, CauldronContents.PLAIN);
            ItemStack potion = ItemStack.builder(type)
                    .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON)
                    .build();
            ItemStack[] held = {potion};
            Player player = player(world, held, new ArrayList<>(), false);

            for (int fill : List.of(2, 4, 6)) {
                held[0] = potion;
                CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

                ParticleEmitter emitter = world.getParticleEmitters().getLast();
                ParticleNumber count = assertInstanceOf(ParticleNumber.class, emitter.variables().get("variable.particlecount"));
                ParticleStruct tint = assertInstanceOf(ParticleStruct.class, emitter.variables().get("variable.color"));
                int color = CloudPotionColor.calculateEffects(PotionTypes.POISON.getEffects()).orElseThrow();

                assertSame(ParticleEmitterTypes.CAULDRON_SPELL_EMITTER, emitter.type());
                assertTrue(count.value() > 0);
                assertEquals(new ParticleNumber((color >> 16 & 0xFF) / 255f), tint.members().get("r"));
                assertEquals(new ParticleNumber((color >> 8 & 0xFF) / 255f), tint.members().get("g"));
                assertEquals(new ParticleNumber((color & 0xFF) / 255f), tint.members().get("b"));
                assertEquals(new ParticleNumber(1), tint.members().get("a"));
                assertEquals(Vector3f.from(0.5f, 64 + (6 + 1.5f * fill) / 16f, 0.5f),
                        world.getParticlePositions().getLast());
            }

            held[0] = potion;
            CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, held[0]);

            assertEquals(3, world.getParticleEmitters().size());
        }
    }

    @Test
    void cancelledPotionFillingDoesNotEmitParticlesOrConsumeTheBottle() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        attachContents(world, CauldronContents.PLAIN);
        world.onEvent(CauldronLevelChangeEvent.class, event -> event.setCancelled(true));
        ItemStack potion = ItemStack.builder(ItemTypes.POTION)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON)
                .build();
        ItemStack[] held = {potion};

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, new ArrayList<>(), false), Direction.UP, held[0]);

        assertSame(potion, held[0]);
        assertTrue(world.getParticleEmitters().isEmpty());
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void incompatiblePotionsEmptyTheCauldronAndPlayTheMixingSound() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        CauldronContents[] contents = attachContents(world, new CauldronContents(PotionTypes.POISON, PotionForm.DRINKABLE, null));
        ItemStack[] held = {ItemStack.builder(ItemTypes.POTION)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.WATER).build()};

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, new ArrayList<>(), false), Direction.UP, held[0]);

        assertSame(cauldron(Bucket.WATER, 0), world.state(POSITION));
        assertEquals(CauldronContents.PLAIN, contents[0]);
        assertEquals(List.of(SoundTypes.CAULDRON_EXPLODE), world.getSounds());
    }

    @Test
    void tippingArrowsConsumesTheWholeTransferredAmountRatherThanOneArrow() {
        for (int fill : List.of(2, 4, 6)) {
            BlockTestWorld world = supplied(BlockStates.WATER);
            world.setState(POSITION, cauldron(Bucket.WATER, fill));
            CauldronContents[] contents = attachContents(world, new CauldronContents(PotionTypes.POISON, PotionForm.DRINKABLE, null));
            ItemStack[] held = {ItemStack.from(ItemTypes.ARROW, 64)};
            List<ItemStack> given = new ArrayList<>();

            CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, given, false), Direction.UP, held[0]);

            int expected = fill == 6 ? 64 : fill / 2 * 16;
            ItemStack output = expected == 64 ? held[0] : given.getFirst();
            assertEquals(expected, output.getCount());
            assertSame(PotionTypes.POISON, output.get(ItemDataComponents.POTION_TYPE));
            if (expected != 64) {
                assertEquals(64 - expected, held[0].getCount());
                assertNull(held[0].get(ItemDataComponents.POTION_TYPE));
            }
            assertSame(cauldron(Bucket.WATER, 0), world.state(POSITION));
            assertEquals(CauldronContents.PLAIN, contents[0]);
            assertEquals(List.of(SoundTypes.CAULDRON_TAKEPOTION), world.getSounds());
        }
    }

    @Test
    void cancellingDyeingPreventsContentsInventoryAndSoundChanges() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        CauldronContents[] contents = attachContents(world, CauldronContents.PLAIN);
        world.onEvent(CauldronLevelChangeEvent.class, event -> event.setCancelled(true));
        ItemStack original = ItemStack.from(ItemTypes.RED_DYE, 3);
        ItemStack[] held = {original};

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, new ArrayList<>(), false), Direction.UP, held[0]);

        assertSame(original, held[0]);
        assertEquals(CauldronContents.PLAIN, contents[0]);
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void cancelledBucketEventPreventsTheLevelEventAndItemExchange() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        world.onEvent(PlayerBucketFillEvent.class, event -> event.setCancelled(true));
        ItemStack[] held = {ItemStack.from(ItemTypes.BUCKET)};

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, new ArrayList<>(), false), Direction.WEST, held[0]);

        assertSame(ItemTypes.BUCKET, held[0].getType());
        assertSame(cauldron(Bucket.WATER, 6), world.state(POSITION));
        assertTrue(world.getEvents(CauldronLevelChangeEvent.class).isEmpty());
        assertTrue(world.getSounds().isEmpty());
        assertEquals(Direction.WEST, world.getEvents(PlayerBucketFillEvent.class).getFirst().getBlockFace());
    }

    @Test
    void eventInventoryChangesAreNotOverwrittenByTheOriginalExchange() {
        BlockTestWorld world = supplied(BlockStates.WATER);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        ItemStack[] held = {ItemStack.from(ItemTypes.BUCKET)};
        world.onEvent(CauldronLevelChangeEvent.class, event -> held[0] = ItemStack.from(ItemTypes.STICK));

        CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, new ArrayList<>(), false), Direction.UP, held[0]);

        assertSame(ItemTypes.STICK, held[0].getType());
        assertSame(cauldron(Bucket.WATER, 6), world.state(POSITION));
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void washingABannerRemovesOnlyTheLastPatternAndCreativeKeepsTheOriginal() {
        for (boolean creative : List.of(false, true)) {
            BlockTestWorld world = supplied(BlockStates.WATER);
            world.setState(POSITION, cauldron(Bucket.WATER, 6));
            BannerPattern first = new BannerPattern(BannerPattern.Type.PATTERN_BOTTOM_STRIPE, DyeColor.RED);
            BannerPattern last = new BannerPattern(BannerPattern.Type.PATTERN_TOP_STRIPE, DyeColor.BLUE);
            ItemStack original = ItemStack.builder(ItemTypes.BANNER)
                    .setData(ItemDataComponents.BANNER_DATA, BannerData.of(0, DyeColor.WHITE, List.of(first, last))).build();
            ItemStack[] held = {original};
            List<ItemStack> given = new ArrayList<>();

            CauldronBlockHandlers.USE.execute(world.block(POSITION), player(world, held, given, creative), Direction.UP, held[0]);

            ItemStack output = creative ? given.getFirst() : held[0];
            BannerData data = output.get(ItemDataComponents.BANNER_DATA);
            assertNotNull(data);
            assertEquals(List.of(first), data.getPatterns());
            assertSame(DyeColor.WHITE, data.getBase());
            if (creative) {
                assertSame(original, held[0]);
            }
            assertEquals(List.of(SoundTypes.CAULDRON_CLEANBANNER), world.getSounds());
        }
    }

    @Test
    void bottlesDoNotReplaceLavaOrPowderSnow() {
        for (Bucket liquid : List.of(Bucket.LAVA, Bucket.POWDER_SNOW)) {
            for (ItemType type : List.of(ItemTypes.POTION, ItemTypes.SPLASH_POTION, ItemTypes.LINGERING_POTION)) {
                BlockTestWorld world = new BlockTestWorld(REGISTRY);
                world.setState(POSITION, cauldron(liquid, 6));
                ItemStack original = ItemStack.builder(type)
                        .setData(ItemDataComponents.POTION_TYPE, PotionTypes.WATER)
                        .build();
                ItemStack[] held = {original};
                Player player = player(world, held, new ArrayList<>(), false);

                assertFalse(CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, original));

                assertSame(original, held[0]);
                assertSame(cauldron(liquid, 6), world.state(POSITION));
                assertTrue(world.getEvents(CauldronLevelChangeEvent.class).isEmpty());
                assertTrue(world.getSounds().isEmpty());
            }
        }
    }

    @Test
    void aFullCauldronDoesNotPreventUsingTheHeldPotionNormally() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, cauldron(Bucket.WATER, 6));
        ItemStack original = ItemStack.builder(ItemTypes.POTION)
                .setData(ItemDataComponents.POTION_TYPE, PotionTypes.POISON)
                .build();
        ItemStack[] held = {original};
        attachContents(world, new CauldronContents(PotionTypes.POISON, PotionForm.DRINKABLE, null));
        Player player = player(world, held, new ArrayList<>(), false);

        assertFalse(CauldronBlockHandlers.USE.execute(world.block(POSITION), player, Direction.UP, original));

        assertSame(original, held[0]);
        assertTrue(world.getEvents(CauldronLevelChangeEvent.class).isEmpty());
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void pickedItemsAndLootDoNotRetainCauldronContents() {
        for (Bucket liquid : List.of(Bucket.WATER, Bucket.LAVA, Bucket.POWDER_SNOW)) {
            BlockTestWorld world = new BlockTestWorld(REGISTRY);
            world.setState(POSITION, cauldron(liquid, 6));

            ItemStack picked = REGISTRY.requireComponent(BlockTypes.CAULDRON, BlockComponents.GET_PICK_BLOCK)
                    .execute(world.block(POSITION));
            List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.CAULDRON, BlockComponents.GET_LOOT)
                    .execute(world.block(POSITION), BlockLootContext.empty(new Random(0)));

            assertSame(ItemTypes.CAULDRON, picked.getType());
            assertSame(cauldron(Bucket.WATER, 0), picked.get(ItemDataComponents.BLOCK_STATE));
            assertEquals(List.of(picked), drops);
            assertFalse(picked.has(ItemDataComponents.POTION_TYPE));
            assertFalse(picked.has(ItemDataComponents.DYED_COLOR));
        }
    }

    private static Player player(BlockTestWorld world, ItemStack[] held, List<ItemStack> given, boolean creative) {
        PlayerInventoryView inventory = InterfaceProxy.create(PlayerInventoryView.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getSelectedItem" -> held[0];
            case "setSelectedItem" -> {
                held[0] = (ItemStack) arguments[0];
                yield null;
            }
            case "contains" -> false;
            case "addItem" -> {
                given.addAll(List.of((ItemStack[]) arguments[0]));
                yield new ItemStack[0];
            }
            default -> throw new AssertionError("Unexpected inventory access: " + method);
        });
        return InterfaceProxy.create(Player.class, Map.of("getInventory", inventory, "isCreative", creative,
                "getLevel", world.block(POSITION).getLevel()));
    }

    private static CauldronContents[] attachContents(BlockTestWorld world, CauldronContents initial) {
        CauldronContents[] contents = {initial};
        Cauldron entity = InterfaceProxy.create(Cauldron.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getContents" -> contents[0];
            case "setContents" -> {
                contents[0] = (CauldronContents) arguments[0];
                yield null;
            }
            default -> throw new AssertionError("Unexpected cauldron access: " + method);
        });
        world.setBlockEntity(POSITION, entity);
        return contents;
    }

    private static BlockTestWorld supplied(BlockState source) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, cauldron(Bucket.WATER, 0));
        world.setState(POSITION.add(0, 4, 0), BlockTypes.POINTED_DRIPSTONE.getDefaultState().withTrait(BlockTraits.IS_HANGING, true));
        world.setState(POSITION.add(0, 5, 0), BlockStates.STONE);
        world.setState(POSITION.add(0, 6, 0), source);
        return world;
    }

    private static BlockState cauldron(Bucket content, int fill) {
        return BlockTypes.CAULDRON.getDefaultState().withTrait(BlockTraits.CAULDRON_TYPE, content).withTrait(BlockTraits.FILL_LEVEL, fill);
    }

    private static class AlwaysFill extends Random {
        @Override
        public float nextFloat() {
            return 0;
        }
    }
}

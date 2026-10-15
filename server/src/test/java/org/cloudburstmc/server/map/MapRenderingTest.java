package org.cloudburstmc.server.map;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.map.MapCursor;
import org.cloudburstmc.api.map.MapCursorType;
import org.cloudburstmc.api.map.MapScale;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.MapDecoration;
import org.cloudburstmc.protocol.bedrock.data.MapTrackedObject;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.map.CloudMapView.SentMap;
import org.cloudburstmc.server.map.MapTerrainRenderer.MarkerPosition;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

class MapRenderingTest {

    @Test
    void imageLayersRemainIndependentOfTheBaseAndDoNotAccumulateOpacity() {
        CloudMapCanvas canvas = new CloudMapCanvas(InterfaceProxy.create(MapView.class));
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80ff0000);

        int[] base = new int[128 * 128];
        base[0] = 0xff0000ff;
        canvas.setBase(base);

        canvas.drawImage(0, 0, image);
        canvas.drawImage(0, 0, image);

        assertEquals(0x80ff0000, requireNonNull(canvas.getPixelColor(0, 0)).getRGB());

        int[] first = base.clone();
        canvas.compositeInto(first);

        assertEquals(0xff80007f, first[0]);

        base[0] = 0xff00ff00;
        canvas.setBase(base);
        int[] second = base.clone();
        canvas.compositeInto(second);

        assertEquals(0xff807f00, second[0]);

        image.setRGB(0, 0, 0);
        canvas.drawImage(0, 0, image);

        assertNull(canvas.getPixelColor(0, 0));
    }

    @Test
    void answersLockedCopyPreviewsUsingTheRequestedIdentityWithoutChangingTheSource() {
        MapView source = InterfaceProxy.create(MapView.class, Map.of("getId", 228L, "getScale", MapScale.CLOSEST));
        long[] hierarchy = {228, 229, 230, 231, 232};
        int[] pixels = new int[128 * 128];
        pixels[0] = 0xff123456;

        ClientboundMapItemDataPacket packet = MapPacketFactory.createLockedCopy(source, 0, hierarchy, pixels, 1177559052L);

        assertEquals(1177559052L, packet.getUniqueMapId());
        assertTrue(packet.isLocked());
        assertEquals((byte) 0, requireNonNull(packet.getScale()).byteValue());
        assertArrayEquals(new long[]{1177559052L, 229, 230, 231, 232}, requireNonNull(packet.getTrackedEntityIds()).toLongArray());
        assertTrue(requireNonNull(packet.getDecorations()).isEmpty());
        assertEquals(MapPacketFactory.toAbgr(pixels[0]), requireNonNull(packet.getColors())[0]);
        assertArrayEquals(new long[]{228, 229, 230, 231, 232}, hierarchy);
        assertFalse(source.isLocked());
        assertEquals(228L, source.getId());
    }

    @Test
    void announcesEveryRemainingScaleForCartographyPrediction() {
        for (MapScale scale : MapScale.values()) {
            long[] hierarchy = new long[MapScale.values().length - scale.ordinal()];
            for (int i = 0; i < hierarchy.length; i++) {
                hierarchy[i] = 216 + scale.ordinal() + i;
            }

            MapView map = InterfaceProxy.create(MapView.class, Map.of("getId", hierarchy[0], "getScale", scale));
            ClientboundMapItemDataPacket packet = requireNonNull(
                    MapPacketFactory.create(map, 0, hierarchy, new int[128 * 128], List.of(), null)
            );

            assertArrayEquals(hierarchy, requireNonNull(packet.getTrackedEntityIds()).toLongArray());
            assertEquals(hierarchy[0], packet.getUniqueMapId());
            assertEquals((byte) scale.ordinal(), requireNonNull(packet.getScale()).byteValue());
        }
    }

    @Test
    void preservesPlayerIdentityAndTintInMapDecorations() {
        MapView map = InterfaceProxy.create(MapView.class, Map.of("getId", 42L, "getScale", MapScale.CLOSEST));
        Entity entity = InterfaceProxy.create(Entity.class, Map.of("getUniqueId", 123L));
        MapCursor cursor = new MapCursor(3, -4, 2, MapCursorType.PLAYER, "Player", Color.RED, entity);
        ClientboundMapItemDataPacket packet = requireNonNull(
                MapPacketFactory.create(map, 0, new long[]{42, 43, 44, 45, 46}, new int[128 * 128], List.of(cursor), null)
        );

        List<MapTrackedObject> trackedObjects = requireNonNull(packet.getTrackedObjects());
        List<MapDecoration> decorations = requireNonNull(packet.getDecorations());

        assertEquals(1, trackedObjects.size());
        assertEquals(1, decorations.size());
        assertEquals(123L, trackedObjects.getFirst().getEntityId());
        assertEquals(Color.RED.getRGB(), decorations.getFirst().getColor());
    }

    @Test
    void projectsPlayersBetweenDimensionsWithoutChangingMapTerrain() {
        Vector3f spawn = Vector3f.from(100, 64, -200);

        MarkerPosition netherPlayer = requireNonNull(MapTerrainRenderer.projectPlayer(
                CloudLevel.DIMENSION_OVERWORLD, CloudLevel.DIMENSION_NETHER, 12, -9, spawn));

        assertEquals(96, netherPlayer.x());
        assertEquals(-72, netherPlayer.z());
        assertEquals(Color.RED, netherPlayer.color());

        MarkerPosition overworldPlayer = requireNonNull(MapTerrainRenderer.projectPlayer(
                CloudLevel.DIMENSION_NETHER, CloudLevel.DIMENSION_OVERWORLD, 96, -72, spawn));

        assertEquals(12, overworldPlayer.x());
        assertEquals(-9, overworldPlayer.z());

        MarkerPosition endPlayer = requireNonNull(MapTerrainRenderer.projectPlayer(
                CloudLevel.DIMENSION_OVERWORLD, CloudLevel.DIMENSION_THE_END, 12, -9, spawn));

        assertEquals(100, endPlayer.x());
        assertEquals(-200, endPlayer.z());
        assertEquals(Color.MAGENTA, endPlayer.color());

        assertNull(MapTerrainRenderer.projectPlayer(
                CloudLevel.DIMENSION_NETHER, CloudLevel.DIMENSION_THE_END, 12, -9, spawn));
        assertNull(MapTerrainRenderer.projectPlayer(
                CloudLevel.DIMENSION_THE_END, CloudLevel.DIMENSION_OVERWORLD, 12, -9, spawn));
    }

    @Test
    void announcesMapCreationBeforeIncrementalTextureUpdates() {
        MapView map = InterfaceProxy.create(MapView.class, Map.of(
                "getId", 42L,
                "getScale", MapScale.NORMAL,
                "getCenterX", 192,
                "getCenterZ", -320));
        int[] pixels = new int[128 * 128];
        long[] mapIds = {42, 43, 44};

        ClientboundMapItemDataPacket initial = requireNonNull(MapPacketFactory.create(map, 0, mapIds, pixels, List.of(), null));

        assertArrayEquals(mapIds, requireNonNull(initial.getTrackedEntityIds()).toLongArray());
        assertEquals(42L, initial.getUniqueMapId());
        assertEquals((byte) 2, requireNonNull(initial.getScale()).byteValue());
        assertEquals(192, initial.getOrigin().getX());
        assertEquals(-320, initial.getOrigin().getZ());
        assertEquals(128 * 128, requireNonNull(initial.getColors()).length);

        SentMap previous = new SentMap(pixels.clone(), List.of());
        assertNull(MapPacketFactory.create(map, 0, mapIds, pixels, List.of(), previous));

        pixels[130] = 0xff123456;
        ClientboundMapItemDataPacket update = requireNonNull(MapPacketFactory.create(map, 0, mapIds, pixels, List.of(), previous));

        assertNull(update.getTrackedEntityIds());
        assertEquals(1, requireNonNull(update.getWidth()).intValue());
        assertEquals(1, requireNonNull(update.getHeight()).intValue());
        assertArrayEquals(new int[]{MapPacketFactory.toAbgr(0xff123456)}, update.getColors());
    }

    @Test
    void findsCarriedMapsOutsideTheSelectedSlotAndInTheOffhand() {
        ItemStack map = ItemStack.builder(ItemTypes.FILLED_MAP).setData(ItemDataComponents.MAP_ID, 42L).build();
        Player inventoryHolder = holder(new ItemStack[]{ItemStack.EMPTY, map}, ItemStack.EMPTY);
        Player offhandHolder = holder(new ItemStack[]{ItemStack.EMPTY}, map);
        Player nonHolder = holder(new ItemStack[]{ItemStack.EMPTY}, ItemStack.EMPTY);

        assertTrue(CloudMapManager.carries(inventoryHolder, 42L));
        assertTrue(CloudMapManager.carries(offhandHolder, 42L));
        assertFalse(CloudMapManager.carries(inventoryHolder, 43L));
        assertFalse(CloudMapManager.carries(nonHolder, 42L));
    }

    @Test
    void alignsNegativeCoordinatesToTheSameGridAsPositiveCoordinates() {
        assertEquals(0, CloudMapManager.alignCenter(-64, MapScale.CLOSEST));
        assertEquals(-128, CloudMapManager.alignCenter(-65, MapScale.CLOSEST));
        assertEquals(0, CloudMapManager.alignCenter(63, MapScale.CLOSEST));
        assertEquals(128, CloudMapManager.alignCenter(64, MapScale.CLOSEST));
        assertEquals(64, CloudMapManager.alignCenter(-64, MapScale.CLOSE));
        assertEquals(-192, CloudMapManager.alignCenter(-65, MapScale.CLOSE));
    }

    @Test
    void compositesTranslucentLayersWithoutLosingTheirAlpha() {
        assertEquals(0xff0000ff, CloudMapCanvas.composite(0xff0000ff, 0));
        assertEquals(0xff80007f, CloudMapCanvas.composite(0xff0000ff, 0x80ff0000));
        assertEquals(0x80ff0000, CloudMapCanvas.composite(0, 0x80ff0000));
        assertEquals(0xffff0000, CloudMapCanvas.composite(0xff0000ff, 0xffff0000));
    }

    @Test
    void convertsMapPixelsToTheNetworkColorOrder() {
        assertEquals(0x78451223, MapPacketFactory.toAbgr(0x78231245));
        assertEquals(0xff0000ff, MapPacketFactory.toAbgr(0xffff0000));
    }

    @Test
    void shadesTerrainWithoutChangingOpacity() {
        assertEquals(0xffb40000, MapTerrainRenderer.shade(0xffff0000, 180));
        assertEquals(0x80ffffff, MapTerrainRenderer.shade(0x80ffffff, 255));
    }

    private static Player holder(ItemStack[] contents, ItemStack offhand) {
        return InterfaceProxy.create(Player.class, Map.of(
                "getInventory", InterfaceProxy.create(PlayerInventoryView.class, Map.of("getContents", contents)),
                "getOffhand", InterfaceProxy.create(OffhandView.class, Map.of("getOffhandItem", offhand))));
    }
}
